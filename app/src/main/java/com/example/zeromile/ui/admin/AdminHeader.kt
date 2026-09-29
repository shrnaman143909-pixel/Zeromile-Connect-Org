package com.example.zeromile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.RealtimeStatusChip
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.viewmodel.AdminUiState
import com.example.zeromile.ui.viewmodel.AdminViewModel

/**
 * Administrative Header Bar
 * Displays municipal title, logged-in administrator email, role badge, profile toggle, citizen app toggle, and logout.
 */
@Composable
fun AdminHeader(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    onSwitchToCitizenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val admin = adminUiState.adminUser

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_header"),
        color = Color(0xFF0F172A), // Slate 900
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Municipal Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0284C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Zeromile Connect",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ZeromileStatusBadge(
                            text = "Admin",
                            status = BadgeStatus.ACTIVE,
                            testTag = "admin_header_role_badge"
                        )
                    }
                    Text(
                        text = "Municipal Administration • Nagpur Municipal Corporation",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            // Right: Administrator info, Citizen app link, Profile & Logout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Realtime Sync Status
                RealtimeStatusChip(connectionState = com.example.zeromile.data.remote.RealtimeConnectionState.CONNECTED)

                // Switch to Citizen View
                ZeromileButton(
                    text = "Citizen View",
                    icon = Icons.Default.OpenInNew,
                    onClick = onSwitchToCitizenApp,
                    variant = ButtonVariant.OUTLINE,
                    modifier = Modifier.testTag("admin_switch_to_citizen_button")
                )

                // Admin Profile Pill / Modal Trigger
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.testTag("admin_profile_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = admin?.email ?: "shrnavan1439009@gmail.com",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Profile Dialog Button
                IconButton(
                    onClick = { adminViewModel.setProfileDialogVisible(true) },
                    modifier = Modifier.testTag("admin_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Administrator Profile",
                        tint = Color(0xFF94A3B8)
                    )
                }

                // Logout Button
                IconButton(
                    onClick = { adminViewModel.signOut() },
                    modifier = Modifier.testTag("admin_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color(0xFFF87171)
                    )
                }
            }
        }
    }
}
