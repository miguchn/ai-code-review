package com.acr.review.notify.feishu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.notify.NotifyRobotException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class FeishuBotClientTest
{
    private MockWebServer server;
    private FeishuBotClient client;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        client = new FeishuBotClient(5000, 15000);
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void sendPostsZhCnContentWithoutSignWhenSecretEmpty() throws InterruptedException
    {
        server.enqueue(json(200, "{\"code\":0,\"msg\":\"success\"}"));
        String title = "AI Code Review · 通过";
        String body = "### ✅ AI Code Review · 通过 · 90/100";

        client.send(server.url("/").toString(), null, title, body);

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        JSONObject payload = JSON.parseObject(request.getBody().readUtf8());
        assertEquals("post", payload.getString("msg_type"));
        assertFalse(payload.containsKey("timestamp"));
        assertFalse(payload.containsKey("sign"));
        JSONObject zhCn = payload.getJSONObject("content").getJSONObject("post").getJSONObject("zh_cn");
        assertEquals(title, zhCn.getString("title"));
        assertTrue(zhCn.getJSONArray("content").size() > 0);
    }

    @Test
    void sendIncludesTimestampAndMatchingSignWhenSecretPresent() throws InterruptedException
    {
        server.enqueue(json(200, "{\"code\":0,\"msg\":\"success\"}"));
        String secret = "SECfeishu";

        client.send(server.url("/").toString(), secret, "title", "body");

        RecordedRequest request = server.takeRequest();
        JSONObject payload = JSON.parseObject(request.getBody().readUtf8());
        String timestamp = payload.getString("timestamp");
        assertTrue(timestamp.matches("\\d+"));
        assertEquals(FeishuBotClient.sign(Long.parseLong(timestamp), secret), payload.getString("sign"));
    }

    @Test
    void sendThrowsWhenCodeNonZero()
    {
        server.enqueue(json(200, "{\"code\":19021,\"msg\":\"sign match fail\"}"));

        assertThrows(NotifyRobotException.class,
            () -> client.send(server.url("/").toString(), "SEC", "t", "b"));
    }

    @Test
    void sendAcceptsLegacyStatusCodeZero()
    {
        server.enqueue(json(200, "{\"StatusCode\":0,\"StatusMessage\":\"success\"}"));

        client.send(server.url("/").toString(), null, "t", "b");
    }

    @Test
    void toPostConvertsHeadingBoldListAndMarkdownLinks()
    {
        JSONArray rows = FeishuBotClient.toPostLines(
            "### ⚠️ AI Code Review · 建议修改 · 78/100\n"
                + "**提交信息**\n"
                + "- 提交人: miguchn\n"
                + "[查看合并请求](https://github.com/acme/demo/pull/4) · "
                + "[查看审查详情](https://acr.example.com/review/record-detail/index/42)");

        assertEquals(4, rows.size());

        JSONObject heading = rows.getJSONArray(0).getJSONObject(0);
        assertEquals("text", heading.getString("tag"));
        assertEquals("⚠️ AI Code Review · 建议修改 · 78/100", heading.getString("text"));
        assertTrue(heading.getJSONArray("style").contains("bold"));

        JSONObject label = rows.getJSONArray(1).getJSONObject(0);
        assertEquals("提交信息", label.getString("text"));
        assertTrue(label.getJSONArray("style").contains("bold"));

        JSONObject list = rows.getJSONArray(2).getJSONObject(0);
        assertEquals("· 提交人: miguchn", list.getString("text"));

        JSONArray links = rows.getJSONArray(3);
        assertEquals("a", links.getJSONObject(0).getString("tag"));
        assertEquals("查看合并请求", links.getJSONObject(0).getString("text"));
        assertEquals("https://github.com/acme/demo/pull/4", links.getJSONObject(0).getString("href"));
        assertEquals(" · ", links.getJSONObject(1).getString("text"));
        assertEquals("查看审查详情", links.getJSONObject(2).getString("text"));
    }

    @Test
    void toPostKeepsPlainAndEmptyLines()
    {
        JSONArray rows = FeishuBotClient.toPostLines("标题行\n\n普通行");

        assertEquals(3, rows.size());
        assertEquals("标题行", rows.getJSONArray(0).getJSONObject(0).getString("text"));
        assertEquals(" ", rows.getJSONArray(1).getJSONObject(0).getString("text"));
        assertEquals("普通行", rows.getJSONArray(2).getJSONObject(0).getString("text"));
    }

    @Test
    void signIsDeterministicAndTimestampSensitive()
    {
        String first = FeishuBotClient.sign(1700000000L, "SEC");
        String second = FeishuBotClient.sign(1700000000L, "SEC");

        assertEquals(first, second);
        assertFalse(first.isEmpty());
        assertFalse(first.equals(FeishuBotClient.sign(1700000001L, "SEC")));
    }

    private static MockResponse json(int code, String body)
    {
        return new MockResponse().setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
