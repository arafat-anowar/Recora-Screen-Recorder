package com.veylqo.recora.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.data.model.RecordUiState
import com.veylqo.recora.ui.components.SettingRow
import com.veylqo.recora.ui.components.ToggleButton
import com.veylqo.recora.ui.theme.CardBg
import com.veylqo.recora.ui.theme.CardBorder
import com.veylqo.recora.ui.theme.CardCornerRadius
import com.veylqo.recora.ui.theme.PillCornerRadius
import com.veylqo.recora.ui.theme.PrimaryPurple
import com.veylqo.recora.ui.theme.ProGold
import com.veylqo.recora.ui.theme.ProGoldBg
import com.veylqo.recora.ui.theme.ProGoldBorder
import com.veylqo.recora.ui.theme.TextDim
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary

@Composable
fun MoreScreen(
    state: RecordUiState,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleSmoothAnimations: (Boolean) -> Unit,
    onRestorePurchases: () -> Unit,
    onRedeemCode: () -> Unit,
    onSelectMonthly: () -> Unit,
    onSelectLifetime: () -> Unit,
    onOpenInfoDialog: (title: String, message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "More",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Membership & application preferences",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }

        // Section Title: Membership
        item {
            Text(
                text = "Membership",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Gold Active Membership Banner (matches screenshot)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardCornerRadius))
                    .background(ProGoldBg)
                    .border(1.5.dp, ProGoldBorder, RoundedCornerShape(CardCornerRadius))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        text = if (state.isProUser) "Recora Premium Active" else "Recora Free Plan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isProUser) ProGold else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (state.isProUser) "Monthly Subscription · Renews: Dec 01, 2026" else "Upgrade to unlock 4K, 2K and remove watermarks",
                        fontSize = 12.sp,
                        color = if (state.isProUser) ProGold.copy(alpha = 0.85f) else TextMuted
                    )
                }
            }
        }

        // Two Plans Side-by-Side: Monthly vs Lifetime
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Monthly Plan Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(CardCornerRadius))
                        .background(CardBg)
                        .border(
                            1.dp,
                            if (state.isProUser) ProGoldBorder.copy(alpha = 0.7f) else CardBorder,
                            RoundedCornerShape(CardCornerRadius)
                        )
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Monthly",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$0.99",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = " /mo",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Flexible subscription",
                                fontSize = 11.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            PlanFeatureItem("No watermark")
                            Spacer(modifier = Modifier.height(4.dp))
                            PlanFeatureItem("Auto instant save")
                            Spacer(modifier = Modifier.height(4.dp))
                            PlanFeatureItem("Cancel anytime")
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(PillCornerRadius))
                                .background(if (state.isProUser) Color(0xFF231F3D) else PrimaryPurple)
                                .clickable { onSelectMonthly() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.isProUser) "Active" else "Select",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.isProUser) TextMuted else Color.White
                            )
                        }
                    }
                }

                // Lifetime Plan Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(CardCornerRadius))
                        .background(CardBg)
                        .border(1.dp, PrimaryPurple.copy(alpha = 0.6f), RoundedCornerShape(CardCornerRadius))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lifetime",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ProGold.copy(alpha = 0.2f))
                                        .border(0.5.dp, ProGold, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "BEST VALUE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$9.99",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = " once",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pay once, own forever",
                                fontSize = 11.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            PlanFeatureItem("No watermark")
                            Spacer(modifier = Modifier.height(4.dp))
                            PlanFeatureItem("4K & 2K unlock")
                            Spacer(modifier = Modifier.height(4.dp))
                            PlanFeatureItem("Lifetime pass")
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(PillCornerRadius))
                                .background(PrimaryPurple)
                                .clickable { onSelectLifetime() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Restore & Redeem Code Links
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onRestorePurchases() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Restore",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clickable { onRedeemCode() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Redeem Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }

        // Section Title: App Preferences
        item {
            Text(
                text = "App Preferences",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // App Preferences Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardCornerRadius))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(CardCornerRadius))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        title = "Haptic Feedback",
                        subtitle = "Vibrate on start, pause, and stop recording",
                        bulletDot = true,
                        trailingContent = {
                            ToggleButton(
                                checked = state.hapticFeedback,
                                onCheckedChange = onToggleHaptic
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Smooth Animations",
                        subtitle = "Enable fluid cosmic motion and tab transitions",
                        bulletDot = true,
                        trailingContent = {
                            ToggleButton(
                                checked = state.smoothAnimations,
                                onCheckedChange = onToggleSmoothAnimations
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Notification Settings",
                        subtitle = "Manage recording status & control bar alerts",
                        bulletDot = true,
                        showChevron = true,
                        onClick = {
                            onOpenInfoDialog(
                                "Notification Settings",
                                "Manage notification priority and persistent foreground controls from system settings."
                            )
                        }
                    )
                }
            }
        }

        // Section Title: System & Hardware
        item {
            Text(
                text = "System & Hardware",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // System & Hardware Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardCornerRadius))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(CardCornerRadius))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        title = "Device Compatibility",
                        subtitle = "Inspect hardware encoder limits, resolutions & codecs",
                        bulletDot = true,
                        showChevron = true,
                        onClick = {
                            onOpenInfoDialog(
                                "Device Compatibility",
                                "Hardware Encoders: H.264/AVC, H.265/HEVC\nMax Supported Resolution: 4K UHD (3840×2160)\nMax Profile: High@5.2\nAudio Codec: AAC (48kHz, 192kbps)"
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "System Diagnostics & Health",
                        subtitle = "Audit storage headroom, battery policy & permissions",
                        bulletDot = true,
                        showChevron = true,
                        onClick = {
                            onOpenInfoDialog(
                                "System Diagnostics & Health",
                                "Storage Headroom: 45.2 GB Available\nBattery Optimization: Unrestricted Recommended\nMediaProjection: Ready"
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Crash & Stability Journal",
                        subtitle = "Review locally recorded stability incidents & diagnostics",
                        bulletDot = true,
                        showChevron = true,
                        onClick = {
                            onOpenInfoDialog(
                                "Crash & Stability Journal",
                                "All systems nominal. Zero unhandled exceptions recorded in local journal."
                            )
                        }
                    )
                }
            }
        }

        // Section Title: About Recora
        item {
            Text(
                text = "About Recora",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // About Recora Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardCornerRadius))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(CardCornerRadius))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        title = "Version",
                        bulletDot = false,
                        trailingText = "0.9.5 (Build 14)"
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Package Name",
                        bulletDot = false,
                        trailingText = "com.veylqo.recora"
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Storage Directory",
                        bulletDot = false,
                        trailingText = "Movies/Recora (Local)"
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Privacy Policy & Disclosures",
                        subtitle = "Offline local storage guarantee & requested permissions",
                        bulletDot = false,
                        showChevron = true,
                        onClick = {
                            onOpenInfoDialog(
                                "Privacy Policy",
                                "Recora is 100% offline-first. No accounts, no analytics, no third-party SDKs, no network uploads. All screen recordings and diagnostic logs remain exclusively on your device in Movies/Recora."
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Terms of Service",
                        subtitle = "Standard usage policy & intellectual property rights",
                        bulletDot = false,
                        showChevron = true,
                        onClick = {
                            onOpenInfoDialog(
                                "Terms of Service",
                                "Recora is provided as-is for personal screen capture and editing on Android devices. You retain full ownership of all captured media."
                            )
                        }
                    )
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    SettingRow(
                        title = "Open-Source Licenses",
                        subtitle = "Third-party libraries & software attributions",
                        bulletDot = false,
                        showChevron = true,
                        onClick = {
                            onOpenInfoDialog(
                                "Open-Source Licenses",
                                "• Jetpack Compose & AndroidX (Apache 2.0)\n• Kotlin Standard Library (Apache 2.0)\n• Material 3 Components (Apache 2.0)"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanFeatureItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "✓",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryPurple,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextPrimary
        )
    }
}
