package com.syxhen.addroidhome.data.network

import com.syxhen.addroidhome.data.network.model.FilterStatusDto
import com.syxhen.addroidhome.data.network.model.QueryLogResponseDto
import com.syxhen.addroidhome.data.network.model.ServerStatusDto
import com.syxhen.addroidhome.data.network.model.SetFilteringRulesRequest
import com.syxhen.addroidhome.data.network.model.SetProtectionRequest
import com.syxhen.addroidhome.data.network.model.StatsDto
import com.syxhen.addroidhome.data.network.model.TopCountEntry
import com.syxhen.addroidhome.data.network.model.TopLatencyEntry
import com.syxhen.addroidhome.data.network.model.toTopCountEntries
import com.syxhen.addroidhome.data.network.model.toTopLatencyEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdGuardModelsSerializationTest {

    private val json = AdGuardNetworkFactory.json

    @Test
    fun `decodes server status response and ignores unknown keys`() {
        val payload = """
            {
              "dns_addresses": ["192.168.1.2", "::1"],
              "dns_port": 53,
              "http_port": 3000,
              "protection_enabled": true,
              "protection_disabled_duration": 0,
              "running": true,
              "version": "v0.107.72",
              "language": "en",
              "dhcp_available": true,
              "start_time": 1732570000000.0,
              "future_unknown_field": "ignored"
            }
        """.trimIndent()

        val status = json.decodeFromString<ServerStatusDto>(payload)

        assertEquals(listOf("192.168.1.2", "::1"), status.dnsAddresses)
        assertEquals(53, status.dnsPort)
        assertEquals(3000, status.httpPort)
        assertTrue(status.protectionEnabled)
        assertEquals(0L, status.protectionDisabledDurationMillis)
        assertTrue(status.running)
        assertEquals("v0.107.72", status.version)
        assertEquals("en", status.language)
        assertTrue(status.dhcpAvailable)
        assertEquals(1732570000000.0, status.startTimeMillis!!, 0.01)
    }

    @Test
    fun `decodes stats response and maps top entries`() {
        val payload = """
            {
              "time_units": "hours",
              "num_dns_queries": 1000,
              "num_blocked_filtering": 250,
              "num_replaced_safebrowsing": 3,
              "num_replaced_safesearch": 5,
              "num_replaced_parental": 2,
              "avg_processing_time": 0.0185,
              "dns_queries": [100, 200, 700],
              "blocked_filtering": [25, 50, 175],
              "replaced_safebrowsing": [0, 1, 2],
              "replaced_parental": [0, 0, 2],
              "top_queried_domains": [{"example.com": 120}, {"github.com": 80}],
              "top_blocked_domains": [{"ads.example.net": 95}],
              "top_clients": [{"192.168.1.10": 600}],
              "top_upstreams_responses": [{"https://dns.quad9.net/dns-query": 950}],
              "top_upstreams_avg_time": [{"https://dns.quad9.net/dns-query": 0.022}]
            }
        """.trimIndent()

        val stats = json.decodeFromString<StatsDto>(payload)

        assertEquals("hours", stats.timeUnits)
        assertEquals(1000L, stats.numDnsQueries)
        assertEquals(250L, stats.numBlockedFiltering)
        assertEquals(25.0, stats.blockedPercentage, 0.001)
        assertEquals(18.5, stats.avgProcessingTimeMillis, 0.001)
        assertEquals(
            listOf(
                TopCountEntry("example.com", 120L),
                TopCountEntry("github.com", 80L),
            ),
            stats.topQueriedDomains.toTopCountEntries(),
        )
        assertEquals(
            listOf(TopLatencyEntry("https://dns.quad9.net/dns-query", 0.022)),
            stats.topUpstreamsAvgTime.toTopLatencyEntries(),
        )
    }

    @Test
    fun `encodes protection request omitting null duration`() {
        val enablePayload = json.encodeToString(SetProtectionRequest(enabled = true))
        val pausePayload = json.encodeToString(
            SetProtectionRequest(enabled = false, durationMillis = 300_000L),
        )

        assertEquals("""{"enabled":true}""", enablePayload)
        assertEquals("""{"enabled":false,"duration":300000}""", pausePayload)
        assertFalse(enablePayload.contains("duration"))
    }

    @Test
    fun `decodes query log response with rules, answers, and client info`() {
        val payload = """
            {
              "oldest": "2026-11-26T00:02:41+01:00",
              "data": [
                {
                  "time": "2026-11-26T00:10:00+01:00",
                  "question": {
                    "name": "ads.example.com",
                    "unicode_name": "ads.example.com",
                    "type": "A",
                    "class": "IN"
                  },
                  "answer": [
                    { "type": "A", "value": "0.0.0.0", "ttl": 10 }
                  ],
                  "cached": false,
                  "upstream": "",
                  "client": "192.168.1.15",
                  "client_info": {
                    "name": "Pixel 9",
                    "disallowed": false,
                    "disallowed_rule": "",
                    "whois": {}
                  },
                  "elapsedMs": "0.42",
                  "reason": "FilteredBlackList",
                  "rules": [
                    { "filter_list_id": 0, "text": "||ads.example.com^" }
                  ],
                  "status": "NOERROR"
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<QueryLogResponseDto>(payload)

        assertEquals("2026-11-26T00:02:41+01:00", response.oldest)
        assertEquals(1, response.data.size)
        val item = response.data.first()
        assertEquals("ads.example.com", item.displayDomain)
        assertEquals("Pixel 9", item.clientDisplayName)
        assertTrue(item.isBlocked)
        assertTrue(item.isBlockedByCustomRuleOnly)
        assertEquals("0.42 ms", item.formattedElapsedMs)
        assertEquals("Custom filtering rules", item.effectiveRules.first().sourceLabel)
    }

    @Test
    fun `decodes filtering status and encodes set_rules request`() {
        val payload = """
            {
              "enabled": true,
              "interval": 24,
              "filters": [
                {
                  "id": 1,
                  "enabled": true,
                  "url": "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt",
                  "name": "AdGuard DNS filter",
                  "rules_count": 58000
                }
              ],
              "whitelist_filters": [],
              "user_rules": ["||ads.example.com^", "@@||good.example.com^"]
            }
        """.trimIndent()

        val status = json.decodeFromString<FilterStatusDto>(payload)
        assertTrue(status.enabled)
        assertEquals(1, status.filters.size)
        assertEquals(listOf("||ads.example.com^", "@@||good.example.com^"), status.userRules)

        val encoded = json.encodeToString(SetFilteringRulesRequest(rules = listOf("||ads.example.com^")))
        assertEquals("""{"rules":["||ads.example.com^"]}""", encoded)
    }

    @Test
    fun `encodes filter management requests and decodes refresh response`() {
        val setUrl = json.encodeToString(
            com.syxhen.addroidhome.data.network.model.SetFilterUrlRequest(
                url = "https://example.com/list.txt",
                whitelist = false,
                data = com.syxhen.addroidhome.data.network.model.FilterUrlDataDto(
                    name = "My List",
                    url = "https://example.com/list.txt",
                    enabled = false,
                ),
            ),
        )
        assertEquals(
            """{"url":"https://example.com/list.txt","whitelist":false,"data":{"name":"My List","url":"https://example.com/list.txt","enabled":false}}""",
            setUrl,
        )

        val addUrl = json.encodeToString(
            com.syxhen.addroidhome.data.network.model.AddFilterUrlRequest(
                name = "Allowlist",
                url = "https://example.com/allow.txt",
                whitelist = true,
            ),
        )
        assertEquals(
            """{"name":"Allowlist","url":"https://example.com/allow.txt","whitelist":true}""",
            addUrl,
        )

        val removeUrl = json.encodeToString(
            com.syxhen.addroidhome.data.network.model.RemoveFilterUrlRequest(
                url = "https://example.com/allow.txt",
                whitelist = true,
            ),
        )
        assertEquals(
            """{"url":"https://example.com/allow.txt","whitelist":true}""",
            removeUrl,
        )

        val refreshReq = json.encodeToString(
            com.syxhen.addroidhome.data.network.model.RefreshFiltersRequest(whitelist = false),
        )
        assertEquals("""{"whitelist":false}""", refreshReq)

        val refreshResp = json.decodeFromString<com.syxhen.addroidhome.data.network.model.RefreshFiltersResponse>(
            """{"updated":2}""",
        )
        assertEquals(2, refreshResp.updated)

        val configReq = json.encodeToString(
            com.syxhen.addroidhome.data.network.model.SetFilteringConfigRequest(
                enabled = true,
                intervalHours = 24,
            ),
        )
        assertEquals("""{"enabled":true,"interval":24}""", configReq)
    }

    @Test
    fun `decodes blocked services catalog and preserves schedule verbatim on round-trip`() {
        val allPayload = """
            {
              "blocked_services": [
                {
                  "id": "tiktok",
                  "name": "TikTok",
                  "group_id": "social_network",
                  "icon_svg": "PHN2Zz48L3N2Zz4=",
                  "rules": ["||tiktok.com^"]
                },
                {
                  "id": "chatgpt",
                  "name": "ChatGPT",
                  "group_id": "ai",
                  "icon_svg": "",
                  "rules": ["||chatgpt.com^"]
                }
              ],
              "groups": [
                { "id": "social_network" },
                { "id": "ai" }
              ]
            }
        """.trimIndent()

        val catalog = json.decodeFromString<com.syxhen.addroidhome.data.network.model.BlockedServicesAllDto>(allPayload)
        assertEquals(2, catalog.blockedServices.size)
        assertEquals("Social Networks", catalog.groups[0].displayLabel)
        assertEquals("AI", catalog.groups[1].displayLabel)

        val schedulePayload = """
            {
              "ids": ["tiktok"],
              "schedule": {
                "time_zone": "Europe/Rome",
                "mon": { "start": 32400000, "end": 64800000 },
                "fri": { "start": 36000000, "end": 72000000 }
              }
            }
        """.trimIndent()

        val current = json.decodeFromString<com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto>(schedulePayload)
        assertEquals(listOf("tiktok"), current.ids)
        assertTrue(current.hasActiveSchedule)
        assertEquals(2, current.activeScheduleDaysCount)
        assertEquals("Europe/Rome", current.scheduleTimeZone)

        val updated = current.copy(ids = listOf("chatgpt", "tiktok"))
        val reencoded = json.encodeToString(updated)
        val decodedAgain = json.decodeFromString<com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto>(reencoded)
        assertEquals(listOf("chatgpt", "tiktok"), decodedAgain.ids)
        assertEquals(current.schedule, decodedAgain.schedule)

        val nullIds = json.decodeFromString<com.syxhen.addroidhome.data.network.model.BlockedServicesScheduleDto>(
            """{"ids":null,"schedule":{"time_zone":"Local"}}""",
        )
        assertTrue(nullIds.ids.isEmpty())
        assertFalse(nullIds.hasActiveSchedule)
    }

    @Test
    fun `decodes safe search config and rewrite entries including legacy omit enabled`() {
        val safeSearchPayload = """
            {
              "enabled": true,
              "bing": true,
              "duckduckgo": false,
              "ecosia": true,
              "google": true,
              "pixabay": false,
              "yandex": false,
              "youtube": true
            }
        """.trimIndent()
        val safeSearch = json.decodeFromString<com.syxhen.addroidhome.data.network.model.SafeSearchConfigDto>(
            safeSearchPayload,
        )
        assertTrue(safeSearch.enabled)
        assertEquals(4, safeSearch.enabledEngineCount)
        assertTrue(safeSearch.google)
        assertTrue(safeSearch.youTube)
        assertFalse(safeSearch.duckDuckGo)

        val rewritesPayload = """
            [
              {"domain": "nas.home.lan", "answer": "192.168.1.50", "enabled": false},
              {"domain": "router.home.lan", "answer": "192.168.1.1"}
            ]
        """.trimIndent()
        val rewrites = json.decodeFromString<List<com.syxhen.addroidhome.data.network.model.RewriteEntryDto>>(
            rewritesPayload,
        )
        assertEquals(2, rewrites.size)
        assertFalse(rewrites[0].enabled)
        assertTrue(rewrites[1].enabled)

        val updateJson = json.encodeToString(
            com.syxhen.addroidhome.data.network.model.RewriteUpdateRequest(
                target = rewrites[0],
                update = rewrites[0].copy(enabled = true),
            ),
        )
        assertTrue(updateJson.contains("\"target\""))
        assertTrue(updateJson.contains("\"update\""))
    }
}
