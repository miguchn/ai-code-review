package com.acr.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.acr.common.exception.ServiceException;

class LlmEndpointValidatorTest
{
    @Test
    void rejectsPlainHttpByDefault()
    {
        LlmEndpointValidator validator = new LlmEndpointValidator(false, false);
        assertThrows(ServiceException.class,
            () -> validator.validate("http://api.example.com/v1/chat/completions"));
    }

    @Test
    void rejectsLoopbackEvenWhenHttpIsAllowed()
    {
        LlmEndpointValidator validator = new LlmEndpointValidator(true, false);
        assertThrows(ServiceException.class,
            () -> validator.validate("http://127.0.0.1/v1/chat/completions"));
    }

    @Test
    void permitsExplicitDevelopmentOverrides()
    {
        LlmEndpointValidator validator = new LlmEndpointValidator(true, true);
        assertDoesNotThrow(() -> validator.validate("http://127.0.0.1/v1/chat/completions"));
    }
}
