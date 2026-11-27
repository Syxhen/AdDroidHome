package com.syxhen.addroidhome.data.network

import java.net.URI
import java.util.Locale

/**
 * Restricts plain `http://` connections to local/private network targets (LAN, loopback,
 * VPN ranges such as Tailscale CGNAT, and local mDNS/homelab hostnames) while requiring
 * HTTPS for public Internet hosts.
 */
object LocalNetworkPolicy {

    private val localSuffixes = listOf(
        ".local",
        ".lan",
        ".home",
        ".home.arpa",
        ".internal",
        ".localdomain",
        ".localhost",
        ".ts.net",
    )

    fun isCleartextAllowedForUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme.equals("https", ignoreCase = true)) return true
        if (!uri.scheme.equals("http", ignoreCase = true)) return false
        val host = uri.host ?: return false
        return isLocalOrPrivateHost(host)
    }

    fun isLocalOrPrivateHost(rawHost: String): Boolean {
        val host = rawHost.trim().trimStart('[').trimEnd(']').lowercase(Locale.US)
        if (host.isEmpty()) return false
        if (host == "localhost" || host == "ip6-localhost") return true

        parseIpv4(host)?.let { octets ->
            return isPrivateIpv4(octets)
        }

        if (host.contains(':')) {
            return isPrivateIpv6(host)
        }

        // Single-label hostnames (e.g. "adguard", "pihole", "raspberrypi") are LAN-only.
        if (!host.contains('.')) return true

        return localSuffixes.any { suffix -> host.endsWith(suffix) }
    }

    private fun parseIpv4(host: String): IntArray? {
        val parts = host.split('.')
        if (parts.size != 4) return null
        val octets = IntArray(4)
        for (i in 0 until 4) {
            val value = parts[i].toIntOrNull() ?: return null
            if (value !in 0..255) return null
            octets[i] = value
        }
        return octets
    }

    private fun isPrivateIpv4(o: IntArray): Boolean {
        val a = o[0]
        val b = o[1]
        return when {
            a == 10 -> true // 10.0.0.0/8 (RFC 1918)
            a == 127 -> true // 127.0.0.0/8 (Loopback)
            a == 169 && b == 254 -> true // 169.254.0.0/16 (Link-local)
            a == 172 && b in 16..31 -> true // 172.16.0.0/12 (RFC 1918)
            a == 192 && b == 168 -> true // 192.168.0.0/16 (RFC 1918)
            a == 100 && b in 64..127 -> true // 100.64.0.0/10 (CGNAT / Tailscale)
            else -> false
        }
    }

    private fun isPrivateIpv6(host: String): Boolean {
        val withoutZone = host.substringBefore('%')
        if (withoutZone == "::1" || withoutZone == "0:0:0:0:0:0:0:1") return true
        val firstHextet = withoutZone.substringBefore(':').toIntOrNull(16) ?: return false
        return when {
            (firstHextet and 0xFE00) == 0xFC00 -> true // fc00::/7 (Unique Local Address)
            (firstHextet and 0xFFC0) == 0xFE80 -> true // fe80::/10 (Link-local)
            else -> false
        }
    }
}
