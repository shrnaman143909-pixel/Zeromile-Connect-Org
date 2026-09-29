package com.example.zeromile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zeromile.data.repository.DataSourceMode
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.ZeromilePageHeader
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.viewmodel.CivicUiState

@Composable
fun ProfileScreen(
    uiState: CivicUiState,
    onOpenAdminPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val profile = uiState.profile

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ZeromilePageHeader(
            title = stringResource(R.string.profile_title),
            subtitle = "Citizen identity & municipal service preferences",
            testTag = "profile_page_header"
        )

        // Citizen Header Card
        ZeromileCard(testTag = "citizen_profile_card") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.fullName ?: "Nagpur Citizen",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile?.phone ?: "+91 98230 12345",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZeromileStatusBadge(
                            text = "Citizen Role",
                            status = BadgeStatus.ACTIVE,
                            testTag = "citizen_role_badge"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ZeromileStatusBadge(
                            text = "Ward 32, Dharampeth",
                            status = BadgeStatus.INFO,
                            testTag = "citizen_ward_badge"
                        )
                    }
                }
            }
        }

        // Profile Details List
        Text(
            text = "Citizen Details",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        ZeromileCard(testTag = "profile_details_card") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileDetailRow(
                    icon = Icons.Default.LocationCity,
                    label = "Jurisdiction",
                    value = "Nagpur Municipal Corporation"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ProfileDetailRow(
                    icon = Icons.Default.Shield,
                    label = "Registered Ward",
                    value = "Ward 32 (Dharampeth Zone)"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ProfileDetailRow(
                    icon = Icons.Default.Language,
                    label = "Preferred Language",
                    value = "English / Marathi"
                )
            }
        }

        // Supabase Integration Diagnostics Card
        Text(
            text = "Backend & Cloud Architecture",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        val connection = uiState.connectionStatus
        val isCloud = connection?.mode == DataSourceMode.SUPABASE_CLOUD

        ZeromileCard(
            backgroundColor = if (isCloud) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            testTag = "supabase_diagnostic_card"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = if (isCloud) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Supabase PostgreSQL",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    ZeromileStatusBadge(
                        text = if (isCloud) "Connected (Live)" else "Nagpur Prototype Seed",
                        status = if (isCloud) BadgeStatus.ACTIVE else BadgeStatus.INFO
                    )
                }

                Text(
                    text = if (isCloud) {
                        "Connected to remote Supabase instance at: ${connection.endpoint}. Services and categories are dynamically queried from PostgreSQL with Row Level Security."
                    } else {
                        "Running in local prototype data mode with Nagpur civic seed data (Ward 32 Dharampeth, NMC departments, Noise Pollution & Potholes). Configure SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in Secrets to sync live."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                if (!isCloud) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "To connect live Supabase:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "1. Execute supabase/migrations/20260101000000_zeromile_phase1_schema.sql\n2. Execute supabase/seed.sql\n3. Set SUPABASE_URL & SUPABASE_PUBLISHABLE_KEY in .env",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                }
            }
        }

        // Municipal Staff & Administration Entry
        Text(
            text = "Municipal Administration",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        ZeromileCard(
            backgroundColor = Color(0xFF0F172A),
            testTag = "admin_portal_entry_card"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NMC Municipal Admin Portal",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Grievance management, squad assignment, and audit logs",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }

                Text(
                    text = "Separate administrative control center for authorized Nagpur Municipal Corporation personnel. Requires Supabase Auth with 'admin' role.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                )

                com.example.zeromile.ui.components.ZeromileButton(
                    text = "Launch Administration Portal →",
                    onClick = onOpenAdminPortal,
                    variant = com.example.zeromile.ui.components.ButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth().testTag("launch_admin_portal_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}
