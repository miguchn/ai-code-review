package com.acr.review.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * 回归：BufferedSource.read 单次上游读会在 TCP 分包下截尾；
 * request(cap) + 精确截取必须取回完整（或恰好 cap）字节。
 */
class HttpResponseBodiesTest
{
    private MockWebServer server;
    private OkHttpClient client;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build();
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void readCappedAssemblesThrottledBodyBelowCap() throws IOException
    {
        long cap = 8_192L;
        String body = "a".repeat(3_000);
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(body)
            .throttleBody(1024, 50, TimeUnit.MILLISECONDS));

        try (Response response = client.newCall(request()).execute())
        {
            String got = HttpResponseBodies.readCapped(response, cap);
            assertEquals(body, got);
        }
    }

    @Test
    void readCappedTruncatesExactlyAtCap() throws IOException
    {
        long cap = 2_048L;
        String body = "b".repeat(5_000);
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(body)
            .throttleBody(1024, 50, TimeUnit.MILLISECONDS));

        try (Response response = client.newCall(request()).execute())
        {
            String got = HttpResponseBodies.readCapped(response, cap);
            assertEquals(cap, got.getBytes(StandardCharsets.UTF_8).length);
            assertEquals(body.substring(0, (int) cap), got);
        }
    }

    @Test
    void readCappedKeepsSinglePacketBody() throws IOException
    {
        String body = "diff --git a/A.java b/A.java\n+line\n";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        try (Response response = client.newCall(request()).execute())
        {
            assertEquals(body, HttpResponseBodies.readCapped(response, 8_192L));
        }
    }

    @Test
    void readWithinLimitAssemblesThrottledBodyBelowCap() throws IOException
    {
        long cap = 8_192L;
        String body = "c".repeat(3_000);
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(body)
            .throttleBody(1024, 50, TimeUnit.MILLISECONDS));

        try (Response response = client.newCall(request()).execute())
        {
            Optional<String> got = HttpResponseBodies.readWithinLimit(response.body(), cap);
            assertTrue(got.isPresent());
            assertEquals(body, got.get());
        }
    }

    @Test
    void readWithinLimitRejectsBodyOverCap() throws IOException
    {
        long cap = 2_048L;
        String body = "d".repeat(3_000);
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(body)
            .throttleBody(1024, 50, TimeUnit.MILLISECONDS));

        try (Response response = client.newCall(request()).execute())
        {
            assertTrue(HttpResponseBodies.readWithinLimit(response.body(), cap).isEmpty());
        }
    }

    @Test
    void readWithinLimitKeepsSinglePacketBody() throws IOException
    {
        String body = "server:\n  port: 8080\n";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        try (Response response = client.newCall(request()).execute())
        {
            Optional<String> got = HttpResponseBodies.readWithinLimit(response.body(), 8_192L);
            assertTrue(got.isPresent());
            assertEquals(body, got.get());
        }
    }

    private Request request()
    {
        return new Request.Builder().url(server.url("/")).build();
    }
}
