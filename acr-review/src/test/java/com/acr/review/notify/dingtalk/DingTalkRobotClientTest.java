package com.acr.review.notify.dingtalk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.notify.NotifyRobotException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class DingTalkRobotClientTest
{
    private MockWebServer server;
    private DingTalkRobotClient client;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        client = new DingTalkRobotClient(5000, 15000);
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void sendPostsMarkdownWithoutSignWhenSecretEmpty() throws InterruptedException
    {
        server.enqueue(json(200, "{\"errcode\":0,\"errmsg\":\"ok\"}"));

        String title = "AI Code Review · 通过";
        String body = "### ✅ AI Code Review · 通过\n总分 90/100";
        client.send(server.url("/").toString(), null, title, body);

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        HttpUrl url = request.getRequestUrl();
        assertFalse(url.queryParameterNames().contains("timestamp"));
        assertFalse(url.queryParameterNames().contains("sign"));

        JSONObject payload = JSON.parseObject(request.getBody().readUtf8());
        assertEquals("markdown", payload.getString("msgtype"));
        assertEquals(title, payload.getJSONObject("markdown").getString("title"));
        assertEquals(body, payload.getJSONObject("markdown").getString("text"));
    }

    @Test
    void sendAppendsValidSignWhenSecretPresent() throws Exception
    {
        server.enqueue(json(200, "{\"errcode\":0,\"errmsg\":\"ok\"}"));
        String secret = "SECdingtalk";

        client.send(server.url("/").toString(), secret, "title", "body");

        RecordedRequest request = server.takeRequest();
        HttpUrl url = request.getRequestUrl();
        String timestamp = url.queryParameter("timestamp");
        String sign = url.queryParameter("sign");
        assertTrue(timestamp != null && !timestamp.isBlank());
        assertTrue(sign != null && !sign.isBlank());
        assertEquals(expectedDingTalkSign(timestamp, secret), sign);
    }

    @Test
    void sendThrowsWhenErrcodeNonZero()
    {
        server.enqueue(json(200, "{\"errcode\":310000,\"errmsg\":\"sign not match\"}"));

        assertThrows(NotifyRobotException.class,
            () -> client.send(server.url("/").toString(), null, "t", "b"));
    }

    @Test
    void appendSignAddsTimestampAndSignWhenSecretPresent()
    {
        String webhookUrl = "https://oapi.dingtalk.com/robot/send?access_token=abc";
        String signed = DingTalkRobotClient.appendSign(webhookUrl, "SEC123");

        assertTrue(signed.startsWith(webhookUrl));
        assertTrue(signed.contains("timestamp="));
        assertTrue(signed.contains("sign="));
    }

    @Test
    void appendSignReturnsOriginalUrlWhenSecretEmpty()
    {
        String webhookUrl = "https://oapi.dingtalk.com/robot/send?access_token=abc";
        assertEquals(webhookUrl, DingTalkRobotClient.appendSign(webhookUrl, null));
        assertEquals(webhookUrl, DingTalkRobotClient.appendSign(webhookUrl, ""));
    }

    private static String expectedDingTalkSign(String timestamp, String secret) throws Exception
    {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    private static MockResponse json(int code, String body)
    {
        return new MockResponse().setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
