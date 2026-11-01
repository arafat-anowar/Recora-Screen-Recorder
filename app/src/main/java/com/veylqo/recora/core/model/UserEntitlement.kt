package com.veylqo.recora.core.model

enum class UserEntitlement(
    val title: String,
    val badgeText: String,
    val isPremium: Boolean,
    val priority: Int
) {
    FREE("Free Plan", "FREE", false, 0),
    CODE_PASS("VIP Pass", "VIP PASS", true, 1),
    MONTHLY("Pro Monthly", "PRO", true, 2),
    LIFETIME("Pro Lifetime", "PRO LIFETIME", true, 3);

    companion object {
        fun reconcile(vararg entitlements: UserEntitlement): UserEntitlement {
            return entitlements.maxByOrNull { it.priority } ?: FREE
        }
    }
}
