package com.syxhen.addroidhome.data.model

import java.util.Locale

/**
 * Updates AdGuard Home `user_rules` cleanly for one-tap domain Block / Unblock actions
 * using AdGuard's standard AdBlock-style syntax (`||domain^` and `@@||domain^`).
 */
object CustomRuleUpdater {

    fun normalizeDomain(rawDomain: String): String =
        rawDomain.trim().trimEnd('.').lowercase(Locale.US)

    fun blockRuleFor(domain: String): String = "||${normalizeDomain(domain)}^"

    fun allowRuleFor(domain: String): String = "@@||${normalizeDomain(domain)}^"

    fun applyQuickRule(
        existingRules: List<String>,
        rawDomain: String,
        block: Boolean,
        blockedByCustomRuleOnly: Boolean = false,
    ): List<String> {
        val domain = normalizeDomain(rawDomain)
        require(domain.isNotEmpty()) { "Domain must not be empty." }

        val blockRule = blockRuleFor(domain)
        val allowRule = allowRuleFor(domain)
        val importantBlockRule = "$blockRule\$important"
        val importantAllowRule = "$allowRule\$important"

        val updated = existingRules.toMutableList()
        if (block) {
            updated.removeAll { rule ->
                val trimmed = rule.trim()
                trimmed.equals(allowRule, ignoreCase = true) ||
                    trimmed.equals(importantAllowRule, ignoreCase = true)
            }
            val alreadyBlocked = updated.any { rule ->
                val trimmed = rule.trim()
                trimmed.equals(blockRule, ignoreCase = true) ||
                    trimmed.equals(importantBlockRule, ignoreCase = true)
            }
            if (!alreadyBlocked) {
                updated.add(blockRule)
            }
        } else {
            val removedCustomBlock = updated.removeAll { rule ->
                val trimmed = rule.trim()
                trimmed.equals(blockRule, ignoreCase = true) ||
                    trimmed.equals(importantBlockRule, ignoreCase = true)
            }
            val alreadyAllowed = updated.any { rule ->
                val trimmed = rule.trim()
                trimmed.equals(allowRule, ignoreCase = true) ||
                    trimmed.equals(importantAllowRule, ignoreCase = true)
            }
            if (!alreadyAllowed && !(blockedByCustomRuleOnly && removedCustomBlock)) {
                updated.add(allowRule)
            }
        }
        return updated
    }
}
