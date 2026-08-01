package com.acr.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.common.ai.LlmCallResult;
import com.acr.common.enums.LlmCallErrorType;
import com.acr.common.security.LlmApiKeyCryptoService;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.mapper.SysAiModelConfigMapper;

@ExtendWith(MockitoExtension.class)
class LlmCallServiceImplTest
{
    @Mock
    private SysAiModelConfigMapper aiModelConfigMapper;

    @Mock
    private LlmApiKeyCryptoService apiKeyCryptoService;

    @Mock
    private LlmEndpointValidator endpointValidator;

    @InjectMocks
    private LlmCallServiceImpl llmCallService;

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception
    {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception
    {
        server.shutdown();
    }

    @Test
    void testConnectionReturnsSuccessOnValidResponse() throws Exception
    {
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}")
            .addHeader("Content-Type", "application/json"));

        LlmCallResult result = llmCallService.testConnection(
            null,
            server.url("/v1/chat/completions").toString(),
            "sk-test-key",
            "demo-model",
            5000);

        assertTrue(result.isSuccess());
        assertTrue(result.getLatencyMs() >= 0);
        assertEquals("OK", result.getContent());
    }

    @Test
    void testConnectionClassifiesAuthFailure() throws Exception
    {
        server.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("{\"error\":{\"message\":\"invalid api key\"}}")
            .addHeader("Content-Type", "application/json"));

        LlmCallResult result = llmCallService.testConnection(
            null,
            server.url("/v1/chat/completions").toString(),
            "bad-key",
            "demo-model",
            5000);

        assertTrue(!result.isSuccess());
        assertEquals(LlmCallErrorType.AUTH, result.getErrorType());
    }

    @Test
    void testConnectionRejectsNonJsonSuccessPage()
    {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("<html>login</html>"));

        LlmCallResult result = llmCallService.testConnection(
            null, server.url("/login").toString(), "sk-test-key", "demo-model", 5000);

        assertTrue(!result.isSuccess());
        assertEquals(LlmCallErrorType.UNKNOWN, result.getErrorType());
    }

    @Test
    void testModelCallUsesStoredKeyWhenMasked() throws Exception
    {
        SysAiModelConfig stored = new SysAiModelConfig();
        stored.setApiUrl(server.url("/stored/chat/completions").toString());
        stored.setApiKey("v1:stored");
        stored.setModel("demo-model");
        stored.setTimeout(5000);
        when(aiModelConfigMapper.selectSysAiModelConfigById(7L)).thenReturn(stored);
        when(apiKeyCryptoService.isEncrypted("v1:stored")).thenReturn(true);
        when(apiKeyCryptoService.decrypt("v1:stored")).thenReturn("sk-real");
        when(aiModelConfigMapper.updateLastCheck(any())).thenReturn(1);

        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}")
            .addHeader("Content-Type", "application/json"));

        LlmCallResult result = llmCallService.testModelCall(
            7L,
            server.url("/attacker/collect").toString(),
            "sk-****real",
            null,
            null,
            32,
            0.2);

        assertTrue(result.isSuccess());
        assertEquals("/stored/chat/completions", server.takeRequest().getPath());
    }
}
