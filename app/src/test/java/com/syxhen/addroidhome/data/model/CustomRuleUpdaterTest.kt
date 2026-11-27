package com.syxhen.addroidhome.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomRuleUpdaterTest {

    @Test
    fun `blocking adds normalized block rule and removes matching allow rule`() {
        val existing = listOf("@@||Ads.Example.COM^", "||other.test^")

        val updated = CustomRuleUpdater.applyQuickRule(
            existingRules = existing,
            rawDomain = "Ads.Example.COM.",
            block = true,
        )

        assertEquals(listOf("||other.test^", "||ads.example.com^"), updated)
    }

    @Test
    fun `unblocking domain from external blocklist adds exception rule`() {
        val existing = listOf("||other.test^")

        val updated = CustomRuleUpdater.applyQuickRule(
            existingRules = existing,
            rawDomain = "tracker.example.net",
            block = false,
            blockedByCustomRuleOnly = false,
        )

        assertEquals(listOf("||other.test^", "@@||tracker.example.net^"), updated)
    }

    @Test
    fun `unblocking domain blocked only by custom rule removes block rule without redundant exception`() {
        val existing = listOf("||custom.blocked.org^", "||keep.me^")

        val updated = CustomRuleUpdater.applyQuickRule(
            existingRules = existing,
            rawDomain = "custom.blocked.org",
            block = false,
            blockedByCustomRuleOnly = true,
        )

        assertEquals(listOf("||keep.me^"), updated)
    }
}
