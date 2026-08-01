package com.acr.review.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Base64;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import com.acr.common.exception.ServiceException;

class CredentialCryptoServiceTest
{
    @Test
    void encryptsWithRandomIvAndDecrypts()
    {
        byte[] key = new byte[32];
        IntStream.range(0, key.length).forEach(index -> key[index] = (byte) index);
        CredentialCryptoService crypto = new CredentialCryptoService(Base64.getEncoder().encodeToString(key));

        String first = crypto.encrypt("test-token-value");
        String second = crypto.encrypt("test-token-value");

        assertNotEquals(first, second);
        assertFalse(first.contains("test-token-value"));
        assertEquals("test-token-value", crypto.decrypt(first));
        assertEquals("test-token-value", crypto.decrypt(second));
    }

    @Test
    void refusesOperationsWithoutMasterKey()
    {
        CredentialCryptoService crypto = new CredentialCryptoService("");
        assertThrows(ServiceException.class, () -> crypto.encrypt("test-token-value"));
    }
}
