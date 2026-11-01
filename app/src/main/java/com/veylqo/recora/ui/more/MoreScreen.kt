package com.veylqo.recora.ui.more

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veylqo.recora.core.model.UserEntitlement
import com.veylqo.recora.data.preferences.RecoraSettings
import com.veylqo.recora.ui.theme.CosmicBorder
import com.veylqo.recora.ui.theme.CosmicCardBorder
import com.veylqo.recora.ui.theme.CosmicSurface
import com.veylqo.recora.ui.theme.GoldAccent
import com.veylqo.recora.ui.theme.TextMuted
import com.veylqo.recora.ui.theme.TextPrimary
import com.veylqo.recora.ui.theme.TextSecondary
import com.veylqo.recora.ui.theme.VioletPrimary

@Composable
fun MoreScreen(
    settings: RecoraSettings,
    onHapticsToggle: (Boolean) -> Unit,
    onSmoothAnimationsToggle: (Boolean) -> Unit,
    onSelectPlan: (UserEntitlement) -> Unit,
    onRestorePurchases: () -> Unit,
    onRedeemCode: () -> Unit,
    onOpenCompatibility: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        // Header
        Text(
            text = "More",
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Membership & application preferences",
            color = TextSecondary,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        // SECTION 1: Membership
        Text(
            text = "Membership",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Active membership banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GoldAccent.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .background(Color(0x22F59E0B), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👑", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (settings.entitlement.isPremium) "Recora Premium Active" else "Free Plan",
                        color = if (settings.entitlement.isPremium) Color(0xFFFBBF24) else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (settings.entitlement.isPremium) {
                        "${settings.entitlement.title} • Active subscription"
                    } else {
                        "Upgrade to remove watermarks and record in 4K UHD"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Side-by-side Pricing Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Monthly Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = if (settings.entitlement == UserEntitlement.MONTHLY) GoldAccent else CosmicCardBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .background(CosmicSurface, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(text = "Monthly", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "$0.99", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = " /mo", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(text = "Flexible subscription", color = TextMuted, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(10.dp))
                    CheckFeatureItem("No watermark")
                    CheckFeatureItem("Auto instant save")
                    CheckFeatureItem("Cancel anytime")

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onSelectPlan(UserEntitlement.MONTHLY) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.entitlement == UserEntitlement.MONTHLY) Color(0xFF281E4D) else VioletPrimary
                        )
                    ) {
                        Text(
                            text = if (settings.entitlement == UserEntitlement.MONTHLY) "Active" else "Select",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Lifetime Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = if (settings.entitlement == UserEntitlement.LIFETIME) GoldAccent else CosmicCardBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .background(CosmicSurface, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lifetime",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF451A03), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "BEST VALUE",
                                color = Color(0xFFFBBF24),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "$9.99", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = " once", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(text = "Pay once, own forever", color = TextMuted, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(10.dp))
                    CheckFeatureItem("No watermark")
                    CheckFeatureItem("4K & 2K unlock")
                    CheckFeatureItem("Lifetime pass")

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onSelectPlan(UserEntitlement.LIFETIME) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.entitlement == UserEntitlement.LIFETIME) Color(0xFF281E4D) else VioletPrimary
                        )
                    ) {
                        Text(
                            text = if (settings.entitlement == UserEntitlement.LIFETIME) "Active" else "Select",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Restore & Redeem Code Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CosmicBorder, RoundedCornerShape(14.dp))
                    .background(CosmicSurface, RoundedCornerShape(14.dp))
                    .clickable(onClick = onRestorePurchases)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Restore",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Restore", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CosmicBorder, RoundedCornerShape(14.dp))
                    .background(CosmicSurface, RoundedCornerShape(14.dp))
                    .clickable(onClick = onRedeemCode)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CardGiftcard,
                        contentDescription = "Redeem",
                        tint = VioletPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Redeem Code", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: App Preferences
        Text(
            text = "App Preferences",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicCardBorder, RoundedCornerShape(18.dp))
                .background(CosmicSurface, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Haptic Feedback
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Vibration, contentDescription = null, tint = VioletPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Haptic Feedback", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Vibrate on start, pause, and stop recording", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.hapticFeedback,
                        onCheckedChange = onHapticsToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VioletPrimary
                        )
                    )
                }

                // Smooth Animations
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = VioletPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Smooth Animations", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Enable fluid cosmic motion and tab transitions", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.smoothAnimations,
                        onCheckedChange = onSmoothAnimationsToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VioletPrimary
                        )
                    )
                }

                // Notification Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                ) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = VioletPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Notification Settings", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Manage recording status & control bar alerts", color = TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: System & Hardware
        Text(
            text = "System & Hardware",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicCardBorder, RoundedCornerShape(18.dp))
                .background(CosmicSurface, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SystemRowItem(
                icon = Icons.Rounded.Speed,
                title = "Device Compatibility",
                subtitle = "Inspect hardware encoder limits, resolutions & codecs",
                onClick = onOpenCompatibility
            )
            SystemRowItem(
                icon = Icons.Rounded.HealthAndSafety,
                title = "System Diagnostics & Health",
                subtitle = "Audit storage headroom, battery policy & permissions",
                onClick = onOpenDiagnostics
            )
            SystemRowItem(
                icon = Icons.Rounded.ReportProblem,
                title = "Crash & Stability Journal",
                subtitle = "Review locally recorded stability incidents & diagnostics",
                onClick = onOpenJournal
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 4: About Recora
        Text(
            text = "About Recora",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicCardBorder, RoundedCornerShape(18.dp))
                .background(CosmicSurface, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoRow("Version", "0.1.0 (Build 1)")
            InfoRow("Package Name", "com.veylqo.recora")
            InfoRow("Storage Directory", "Movies/Recora (Local)")

            Spacer(modifier = Modifier.height(4.dp))

            AboutClickableRow("Privacy Policy & Disclosures", "Offline local storage guarantee & requested permissions", onOpenPrivacyPolicy)
            AboutClickableRow("Terms of Service", "Standard usage policy & intellectual property rights", onOpenTerms)
            AboutClickableRow("Open-Source Licenses", "Third-party libraries & software attributions", onOpenLicenses)
        }
    }
}

@Composable
private fun CheckFeatureItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = VioletPrimary, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun SystemRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = null, tint = VioletPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AboutClickableRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}
