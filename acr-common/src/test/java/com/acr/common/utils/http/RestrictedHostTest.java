package com.acr.common.utils.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class RestrictedHostTest
{
    @Test
    void allowsLoopbackAndPrivateLan() throws Exception
    {
        assertFalse(RestrictedHost.isBlocked(InetAddress.getByName("127.0.0.1")));
        assertFalse(RestrictedHost.isBlocked(InetAddress.getByAddress(new byte[] {10, 0, 0, 1})));
        assertDoesNotThrow(() -> RestrictedHost.requireAllowed("127.0.0.1"));
    }

    @Test
    void blocksLinkLocalAndAliyunMetadata() throws Exception
    {
        assertTrue(RestrictedHost.isBlocked(InetAddress.getByAddress(
            new byte[] {(byte) 169, (byte) 254, (byte) 169, (byte) 254})));
        assertTrue(RestrictedHost.isBlocked(InetAddress.getByAddress(
            new byte[] {100, 100, 100, (byte) 200})));
        assertThrows(IllegalArgumentException.class, () -> RestrictedHost.requireAllowed("169.254.169.254"));
        assertThrows(IllegalArgumentException.class, () -> RestrictedHost.requireAllowed("100.100.100.200"));
        assertThrows(IllegalArgumentException.class, () -> RestrictedHost.requireAllowed("metadata.google.internal"));
    }
}
