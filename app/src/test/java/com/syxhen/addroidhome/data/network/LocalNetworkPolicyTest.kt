package com.syxhen.addroidhome.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPolicyTest {

    @Test
    fun `allows plain http for private ipv4, tailscale, ipv6 local, and lan hostnames`() {
        val allowedUrls = listOf(
            "http://192.168.1.2:3000",
            "http://10.0.0.5:80",
            "http://172.16.0.10:3000",
            "http://172.31.255.1:8080",
            "http://100.64.0.1:3000",
            "http://100.127.255.254:3000",
            "http://127.0.0.1:3000",
            "http://169.254.1.5:3000",
            "http://[::1]:3000",
            "http://[fd00::1]:3000",
            "http://[fe80::1]:3000",
            "http://localhost:3000",
            "http://adguard.local:3000",
            "http://adguard.lan",
            "http://adguard.home",
            "http://dns.tailnet.ts.net:3000",
            "http://adguard:3000",
        )

        allowedUrls.forEach { url ->
            assertTrue("Expected LAN HTTP to be allowed for $url", LocalNetworkPolicy.isCleartextAllowedForUrl(url))
        }
    }

    @Test
    fun `blocks plain http for public domains and public ip addresses`() {
        val blockedUrls = listOf(
            "http://example.com",
            "http://adguard.example.org:3000",
            "http://8.8.8.8:3000",
            "http://172.15.0.1:3000",
            "http://172.32.0.1:3000",
            "http://100.63.255.255:3000",
            "http://100.128.0.1:3000",
            "http://[2001:db8::1]:3000",
        )

        blockedUrls.forEach { url ->
            assertFalse("Expected public HTTP to be blocked for $url", LocalNetworkPolicy.isCleartextAllowedForUrl(url))
        }
    }

    @Test
    fun `always allows https for public and private hosts`() {
        assertTrue(LocalNetworkPolicy.isCleartextAllowedForUrl("https://adguard.example.com"))
        assertTrue(LocalNetworkPolicy.isCleartextAllowedForUrl("https://192.168.1.2:443"))
    }
}
