package com.acr.web.controller.review;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.review.domain.WebhookHandleResult;
import com.acr.review.service.IReviewWebhookService;

/** GitHub Webhook 接入。匿名访问，安全由签名校验保证；只负责协议转换与快速响应。 */
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
    public ResponseEntity<Map<String, Object>> github(
        @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
        @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
        @RequestBody(required = false) byte[] payload)
    {
        WebhookHandleResult result = webhookService.handleGitHubWebhook(eventType, deliveryId, signatureHeader, payload);
        Map<String, Object> body = new HashMap<>(2);
        body.put("message", result.message());
        return ResponseEntity.status(result.httpStatus()).body(body);
    }
}
