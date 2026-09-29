package com.example.zeromile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zeromile.data.repository.DataSourceMode
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.NotificationBellButton
import com.example.zeromile.ui.components.RealtimeStatusChip
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.viewmodel.CivicUiState
import com.example.zeromile.ui.viewmodel.NavTab

@Composable
fun HomeScreen(
    uiState: CivicUiState,
    onNavigateTab: (NavTab) -> Unit,
    onOpenAiModal: (String) -> Unit,
    onOpenServiceDetailById: (String) -> Unit,
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Civic Bar: Nagpur location, realtime status, and notification bell
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.testTag("location_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Nagpur • Ward 32, Dharampeth",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RealtimeStatusChip(connectionState = uiState.realtimeConnectionState)

                NotificationBellButton(
                    unreadCount = uiState.unreadNotificationsCount,
                    onClick = onOpenNotifications
                )
            }
        }

        // Hero Card: Zeromile Connect & "Your gateway to civic services."
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(24.dp)
                .testTag("hero_card")
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Powered",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Primary CTA Button: "Tell Zeromile what you need."
                ZeromileButton(
                    text = stringResource(R.string.primary_cta),
                    icon = Icons.Default.Search,
                    onClick = { onOpenAiModal("") },
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "primary_cta_button"
                )
            }
        }

        // Secondary Access Grid: Services, Emergency, Civic Updates, My Activity
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickAccessCard(
                            title = "Services",
                            subtitle = "Directory",
                            icon = Icons.Default.Build,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                            onClick = { onNavigateTab(NavTab.SERVICES) },
                            testTag = "quick_access_services",
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "Emergency",
                            subtitle = "24x7 Help",
                            icon = Icons.Default.Warning,
                            containerColor = Color(0xFFFFEBEE),
                            contentColor = Color(0xFFC62828),
                            onClick = { onNavigateTab(NavTab.EMERGENCY) },
                            testTag = "quick_access_emergency",
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "Updates",
                            subtitle = "City Alerts",
                            icon = Icons.Default.Campaign,
                            containerColor = Color(0xFFFFF7ED),
                            contentColor = Color(0xFFD97706),
                            onClick = { onNavigateTab(NavTab.UPDATES) },
                            testTag = "quick_access_updates",
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "Activity",
                            subtitle = "Status Ledger",
                            icon = Icons.Default.History,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.secondary,
                            onClick = { onNavigateTab(NavTab.ACTIVITY) },
                            testTag = "quick_access_activity",
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickAccessCard(
                                title = "Services",
                                subtitle = "Directory",
                                icon = Icons.Default.Build,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary,
                                onClick = { onNavigateTab(NavTab.SERVICES) },
                                testTag = "quick_access_services",
                                modifier = Modifier.weight(1f)
                            )
                            QuickAccessCard(
                                title = "Emergency",
                                subtitle = "24x7 Help",
                                icon = Icons.Default.Warning,
                                containerColor = Color(0xFFFFEBEE),
                                contentColor = Color(0xFFC62828),
                                onClick = { onNavigateTab(NavTab.EMERGENCY) },
                                testTag = "quick_access_emergency",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickAccessCard(
                                title = "Updates",
                                subtitle = "City Alerts",
                                icon = Icons.Default.Campaign,
                                containerColor = Color(0xFFFFF7ED),
                                contentColor = Color(0xFFD97706),
                                onClick = { onNavigateTab(NavTab.UPDATES) },
                                testTag = "quick_access_updates",
                                modifier = Modifier.weight(1f)
                            )
                            QuickAccessCard(
                                title = "Activity",
                                subtitle = "Status Ledger",
                                icon = Icons.Default.History,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.secondary,
                                onClick = { onNavigateTab(NavTab.ACTIVITY) },
                                testTag = "quick_access_activity",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Active Nagpur Civic Alerts Banner / Highlights
        val updatesList = (uiState.civicUpdatesState as? com.example.zeromile.ui.viewmodel.UiState.Success)?.data.orEmpty()
        val activeUpdates = updatesList.filter { it.isActive }.take(2)
        if (activeUpdates.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nagpur Civic Alerts",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clickable { onNavigateTab(NavTab.UPDATES) }
                            .testTag("home_view_all_updates")
                    )
                }

                activeUpdates.forEach { alert ->
                    ZeromileCard(
                        onClick = { onNavigateTab(NavTab.UPDATES) },
                        testTag = "home_alert_card_${alert.id}"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (alert.priority.lowercase()) {
                                            "urgent" -> Color(0xFFFFEBEE)
                                            "high" -> Color(0xFFFFF7ED)
                                            else -> Color(0xFFF0FDF4)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = when (alert.priority.lowercase()) {
                                        "urgent" -> Color(0xFFDC2626)
                                        "high" -> Color(0xFFD97706)
                                        else -> Color(0xFF16A34A)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                                Text(
                                    text = if (alert.ward != null) "Ward ${alert.ward.wardNumber}: ${alert.ward.name}" else "City-Wide Nagpur",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Featured Civic Services in Nagpur (Noise Pollution primary requirement)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequent Nagpur Requests",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.testTag("view_all_link")
                )
            }

            // Primary Service: Noise Pollution
            ZeromileCard(
                onClick = { onOpenServiceDetailById("s1111111-1111-1111-1111-111111111111") },
                testTag = "featured_noise_pollution_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Noise Pollution",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ZeromileStatusBadge(text = "Pollution", status = BadgeStatus.WARNING)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Report loudspeakers, commercial decibels & late-night noise in Dharampeth",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Handled by Municipal Corporation",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Pothole Complaint
            ZeromileCard(
                onClick = { onOpenServiceDetailById("s2222222-2222-2222-2222-222222222222") },
                testTag = "featured_pothole_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pothole Complaint",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ZeromileStatusBadge(text = "Roads", status = BadgeStatus.INFO)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Report uneven tar, asphalt depressions, and damaged surface",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Handled by Roads Department",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    ZeromileCard(
        modifier = modifier,
        onClick = onClick,
        testTag = testTag,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
