package com.acr.review.notify.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.notify.NotifyRobotException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class WeComRobotClientTest
{
    private MockWebServer server;
    private WeComRobotClient client;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        client = new WeComRobotClient(5000, 15000);
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void sendPostsMarkdownContent() throws InterruptedException
    {
        server.enqueue(json(200, "{\"errcode\":0,\"errmsg\":\"ok\"}"));
        String body = "### ✅ AI Code Review · 通过\n总分 90/100";

        client.send(server.url("/").toString(), null, "ignored", body);

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        JSONObject payload = JSON.parseObject(request.getBody().readUtf8());
        assertEquals("markdown", payload.getString("msgtype"));
        assertEquals(body, payload.getJSONObject("markdown").getString("content"));
    }

    @Test
    void sendTruncatesBodyOverWeComByteLimit() throws InterruptedException
    {
        server.enqueue(json(200, "{\"errcode\":0}"));
        String oversized = "x".repeat(ReviewDeliveryConstants.WECOM_MAX_MARKDOWN_BYTES + 200);

        client.send(server.url("/").toString(), null, "t", oversized);

        RecordedRequest request = server.takeRequest();
        String content = JSON.parseObject(request.getBody().readUtf8())
            .getJSONObject("markdown").getString("content");
        assertTrue(content.getBytes(StandardCharsets.UTF_8).length
            <= ReviewDeliveryConstants.WECOM_MAX_MARKDOWN_BYTES);
        assertTrue(content.endsWith("..."));
    }

    @Test
    void sendThrowsWhenErrcodeNonZero()
    {
        server.enqueue(json(200, "{\"errcode\":93000,\"errmsg\":\"invalid webhook url\"}"));

        assertThrows(NotifyRobotException.class,
            () -> client.send(server.url("/").toString(), null, "t", "b"));
    }

    @Test
    void truncateKeepsShortTextUntouched()
    {
        assertEquals("abc中文", WeComRobotClient.truncateUtf8("abc中文", 100));
    }

    @Test
    void truncateCutsAtByteBudgetWithoutSplittingMultibyteChars()
    {
        String text = "中".repeat(10);

        String cut = WeComRobotClient.truncateUtf8(text, 7);

        assertEquals("中中", cut);
        assertTrue(cut.getBytes(StandardCharsets.UTF_8).length <= 7);
    }

    @Test
    void truncateHandlesZeroBudget()
    {
        assertEquals("", WeComRobotClient.truncateUtf8("abc", 0));
    }

    private static MockResponse json(int code, String body)
    {
        return new MockResponse().setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
