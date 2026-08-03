package com.acr.web.controller.review;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.review.domain.WebhookHandleResult;
import com.acr.review.git.GitProviderCodes;
import com.acr.review.git.WebhookRequestHeaders;
import com.acr.review.service.IReviewWebhookService;
import jakarta.servlet.http.HttpServletRequest;

/** Git 平台 Webhook 接入。匿名访问，安全由签名校验保证；只负责协议转换与快速响应。 */
@RestController
@RequestMapping("/webhook")
public class GitHubWebhookController
{
    private final IReviewWebhookService webhookService;

    public GitHubWebhookController(IReviewWebhookService webhookService)
    {
        this.webhookService = webhookService;
    }

    @PostMapping("/github")
    public ResponseEntity<Map<String, Object>> github(@RequestBody(required = false) byte[] payload,
                                                      HttpServletRequest request)
    {
        return handle(GitProviderCodes.GITHUB, payload, request);
    }

    @PostMapping("/gitlab")
    public ResponseEntity<Map<String, Object>> gitlab(@RequestBody(required = false) byte[] payload,
                                                      HttpServletRequest request)
    {
        return handle(GitProviderCodes.GITLAB, payload, request);
    }

    @PostMapping("/gitee")
    public ResponseEntity<Map<String, Object>> gitee(@RequestBody(required = false) byte[] payload,
                                                     HttpServletRequest request)
    {
        return handle(GitProviderCodes.GITEE, payload, request);
    }

    @PostMapping("/gitea")
    public ResponseEntity<Map<String, Object>> gitea(@RequestBody(required = false) byte[] payload,
                                                     HttpServletRequest request)
    {
        return handle(GitProviderCodes.GITEA, payload, request);
    }

    private ResponseEntity<Map<String, Object>> handle(String providerCode, byte[] payload, HttpServletRequest request)
    {
        WebhookHandleResult result = webhookService.handleWebhook(
            providerCode, collectHeaders(request), payload);
        Map<String, Object> body = new HashMap<>(2);
        body.put("message", result.message());
        return ResponseEntity.status(result.httpStatus()).body(body);
    }

    private static WebhookRequestHeaders collectHeaders(HttpServletRequest request)
    {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements())
        {
            String name = names.nextElement();
            if (name == null)
            {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            // x-* 覆盖 GitHub/Gitee/Gitea；webhook-* 覆盖 GitLab 新版签名头
            if (lower.startsWith("x-") || lower.startsWith("webhook-") || lower.equals("content-type"))
            {
                headers.put(name, request.getHeader(name));
            }
        }
        return WebhookRequestHeaders.of(headers);
    }
}
