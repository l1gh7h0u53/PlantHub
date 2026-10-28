package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Privacy & Philosophy",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "v1.6.1 Solo Vibecoder Disclosure & Terms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Disclosure Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DisclosureCard(
                            icon = Icons.Filled.Code,
                            badge = "SOLO VIBECODER DEV",
                            badgeColor = MaterialTheme.colorScheme.primaryContainer,
                            title = "Solo Vibecoder Development Disclosure",
                            description = "This is a vibecoded product crafted with passion and rapid iteration by a solo vibecoder \"dev\". Because of the experimental and vibecoded nature of development, there could be bugs or edge cases that haven't been caught yet as it is a solo vibecoder \"dev\". If you encounter any unexpected behavior or glitch, starter cultivar data can be cleanly reloaded anytime from the Settings ledger."
                        )
                    }

                    item {
                        DisclosureCard(
                            icon = Icons.Filled.CloudOff,
                            badge = "ZERO-CLOUD GUARANTEE",
                            badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                            title = "100% On-Device Privacy Boundary",
                            description = "PlantHub strictly does not collect, transmit, share, sell, or store any personal data, photographs, cultivation schedules, telemetry, or analytics. There are zero remote tracking SDKs, zero user accounts, zero password logins, and zero cloud databases. Everything you input lives solely inside your local sandboxed device."
                        )
                    }

                    item {
                        DisclosureCard(
                            icon = Icons.Filled.Storage,
                            badge = "RESILIENT PERSISTENCE",
                            badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                            title = "Automatic Plant Lifecycle Memory",
                            description = "Your active growing cultivars, custom photoperiod timers, feeding logs, and milestone photos are preserved in local SQLite Room tables and automatically mirrored into redundant fail-safe device backups. When updating PlantHub or launching new releases, your plants are remembered and preserved so you never have to start your garden from scratch."
                        )
                    }

                    item {
                        DisclosureCard(
                            icon = Icons.Filled.Lock,
                            badge = "PHOTO VAULT SANDBOX",
                            badgeColor = MaterialTheme.colorScheme.primaryContainer,
                            title = "Zero-Permission & Private Camera Sandbox",
                            description = "PlantHub utilizes Android's zero-permission Photo Picker and private in-app camera capture for progress and milestone photos. Your pictures remain strictly isolated inside your device's protected internal app sandbox storage, never appear in your phone gallery, and are never uploaded or analyzed by external machine-learning servers."
                        )
                    }

                    item {
                        DisclosureCard(
                            icon = Icons.Filled.Eco,
                            badge = "COMMUNITY PHILOSOPHY",
                            badgeColor = MaterialTheme.colorScheme.surfaceVariant,
                            title = "Mindful Cultivation & Craft",
                            description = "PlantHub was designed to honor home growers and botanical caretakers. We stand for clean code, ad-free utilities, data sovereignty, and intuitive Material 3 design."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_close_privacy_dialog")
                ) {
                    Text("I Understand & Agree", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DisclosureCard(
    icon: ImageVector,
    badge: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
