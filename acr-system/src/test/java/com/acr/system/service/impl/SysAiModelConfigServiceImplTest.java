package com.acr.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.common.exception.ServiceException;
import com.acr.common.security.LlmApiKeyCryptoService;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.mapper.SysAiModelConfigMapper;

@ExtendWith(MockitoExtension.class)
class SysAiModelConfigServiceImplTest
{
    @Mock
    private SysAiModelConfigMapper aiModelConfigMapper;

    @Mock
    private LlmApiKeyCryptoService apiKeyCryptoService;

    @Mock
    private LlmCallServiceImpl llmCallService;

    @InjectMocks
    private SysAiModelConfigServiceImpl service;

    @Test
    void encryptsApiKeyOnInsert()
    {
        when(apiKeyCryptoService.encrypt("plain-key")).thenReturn("v1:encrypted");
        SysAiModelConfig config = new SysAiModelConfig();
        config.setProvider("deepseek");
        config.setModelName("DeepSeek");
        config.setApiUrl("https://api.deepseek.com/v1/chat/completions");
        config.setApiKey("plain-key");
        when(aiModelConfigMapper.insertSysAiModelConfig(any())).thenReturn(1);

        service.insertSysAiModelConfig(config);

        assertEquals("v1:encrypted", config.getApiKey());
    }

    @Test
    void skipsApiKeyUpdateWhenBlankOnEdit()
    {
        SysAiModelConfig config = new SysAiModelConfig();
        config.setModelId(1L);
        config.setProvider("openai");
        config.setModelName("OpenAI");
        config.setApiUrl("https://api.openai.com/v1/chat/completions");
        config.setApiKey("");
        SysAiModelConfig existing = new SysAiModelConfig();
        existing.setProvider("openai");
        existing.setApiUrl(config.getApiUrl());
        existing.setEnabled("1");
        existing.setIsDefault("0");
        when(aiModelConfigMapper.selectSysAiModelConfigById(1L)).thenReturn(existing);
        when(aiModelConfigMapper.updateSysAiModelConfig(any())).thenReturn(1);

        service.updateSysAiModelConfig(config);

        verify(aiModelConfigMapper).updateSysAiModelConfig(config);
        assertNull(config.getApiKey());
    }

    @Test
    void clearsOtherDefaultsWhenSettingDefault()
    {
        SysAiModelConfig existing = new SysAiModelConfig();
        existing.setModelId(9L);
        existing.setEnabled("1");
        when(aiModelConfigMapper.selectSysAiModelConfigById(9L)).thenReturn(existing);
        when(aiModelConfigMapper.clearDefaultModel()).thenReturn(1);
        when(aiModelConfigMapper.markDefaultIfEnabled(9L)).thenReturn(1);

        service.setDefaultModel(9L);

        verify(aiModelConfigMapper).clearDefaultModel();
        verify(aiModelConfigMapper).markDefaultIfEnabled(9L);
    }

    @Test
    void returnsOnlyMaskedApiKey()
    {
        SysAiModelConfig stored = new SysAiModelConfig();
        stored.setApiKey("v1:encrypted");
        when(aiModelConfigMapper.selectSysAiModelConfigById(3L)).thenReturn(stored);
        when(apiKeyCryptoService.isEncrypted("v1:encrypted")).thenReturn(true);
        when(apiKeyCryptoService.decrypt("v1:encrypted")).thenReturn("sk-live-secret-abcd");

        SysAiModelConfig result = service.selectSysAiModelConfigById(3L);

        assertEquals("sk-****abcd", result.getApiKey());
    }

    @Test
    void rejectsDisablingDefaultModel()
    {
        SysAiModelConfig existing = new SysAiModelConfig();
        existing.setIsDefault("1");
        when(aiModelConfigMapper.selectSysAiModelConfigById(5L)).thenReturn(existing);

        assertThrows(ServiceException.class, () -> service.enableModel(5L, "0"));
    }

    @Test
    void encryptsLegacyPlaintextKeysAtStartup()
    {
        SysAiModelConfig legacy = new SysAiModelConfig();
        legacy.setModelId(8L);
        legacy.setApiKey("legacy-plaintext");
        when(aiModelConfigMapper.selectSysAiModelConfigList(any())).thenReturn(java.util.List.of(legacy));
        when(apiKeyCryptoService.encrypt("legacy-plaintext")).thenReturn("v1:migrated");

        service.encryptLegacyApiKeys();

        verify(aiModelConfigMapper).updateSysAiModelConfig(argThat(config ->
            Long.valueOf(8L).equals(config.getModelId()) && "v1:migrated".equals(config.getApiKey())));
    }

    @Test
    void requiresNewKeyWhenEndpointChanges()
    {
        SysAiModelConfig existing = new SysAiModelConfig();
        existing.setProvider("openai");
        existing.setApiUrl("https://old.example/v1/chat/completions");
        existing.setIsDefault("0");
        when(aiModelConfigMapper.selectSysAiModelConfigById(6L)).thenReturn(existing);

        SysAiModelConfig update = new SysAiModelConfig();
        update.setModelId(6L);
        update.setProvider("openai");
        update.setApiUrl("https://new.example/v1/chat/completions");
        update.setApiKey("");

        assertThrows(ServiceException.class, () -> service.updateSysAiModelConfig(update));
    }

    @Test
    void rejectsUnsettingDefaultThroughEdit()
    {
        SysAiModelConfig existing = new SysAiModelConfig();
        existing.setProvider("openai");
        existing.setApiUrl("https://api.example/v1/chat/completions");
        existing.setEnabled("1");
        existing.setIsDefault("1");
        when(aiModelConfigMapper.selectSysAiModelConfigById(7L)).thenReturn(existing);

        SysAiModelConfig update = new SysAiModelConfig();
        update.setModelId(7L);
        update.setProvider("openai");
        update.setApiUrl(existing.getApiUrl());
        update.setApiKey("");
        update.setEnabled("0");
        update.setIsDefault("0");

        assertThrows(ServiceException.class, () -> service.updateSysAiModelConfig(update));
    }

    @Test
    void decryptsKeyOnlyForTrustedRuntimeConfig()
    {
        SysAiModelConfig stored = new SysAiModelConfig();
        stored.setApiKey("v1:encrypted");
        when(aiModelConfigMapper.selectSysAiModelConfigById(10L)).thenReturn(stored);
        when(apiKeyCryptoService.isEncrypted("v1:encrypted")).thenReturn(true);
        when(apiKeyCryptoService.decrypt("v1:encrypted")).thenReturn("sk-runtime");

        SysAiModelConfig result = service.selectRuntimeConfigById(10L);

        assertEquals("sk-runtime", result.getApiKey());
    }
}
