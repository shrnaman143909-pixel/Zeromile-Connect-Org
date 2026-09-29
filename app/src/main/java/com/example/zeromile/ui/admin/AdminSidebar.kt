package com.example.zeromile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.ui.viewmodel.AdminSection
import com.example.zeromile.ui.viewmodel.AdminUiState
import com.example.zeromile.ui.viewmodel.AdminViewModel

/**
 * Navigation sidebar for desktop/tablet and bottom navigation for mobile admin view
 */
@Composable
fun AdminDesktopSidebar(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight()
            .testTag("admin_desktop_sidebar"),
        color = Color(0xFF0F172A), // Slate 900
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "ADMINISTRATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            val navItems = listOf(
                Pair(AdminSection.DASHBOARD, Icons.Default.Dashboard),
                Pair(AdminSection.COMPLAINTS, Icons.Default.Assessment),
                Pair(AdminSection.UPDATES, Icons.Default.Campaign),
                Pair(AdminSection.TEAMS, Icons.Default.Groups),
                Pair(AdminSection.SERVICES, Icons.Default.Build),
                Pair(AdminSection.AUDIT_LOGS, Icons.Default.Security)
            )

            navItems.forEach { (section, icon) ->
                val isSelected = adminUiState.currentSection == section ||
                        (section == AdminSection.COMPLAINTS && adminUiState.currentSection == AdminSection.COMPLAINT_DETAIL)

                Surface(
                    onClick = { adminViewModel.setSection(section) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.2f) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_nav_${section.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = section.displayName,
                            tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = section.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Version info
            Text(
                text = "Zeromile Core v7.0\nRLS Protected",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Color(0xFF475569)
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun AdminMobileBottomBar(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("admin_mobile_bottom_bar"),
        containerColor = Color(0xFF0F172A),
        tonalElevation = 8.dp
    ) {
        val navItems = listOf(
            Pair(AdminSection.DASHBOARD, Icons.Default.Dashboard),
            Pair(AdminSection.COMPLAINTS, Icons.Default.Assessment),
            Pair(AdminSection.UPDATES, Icons.Default.Campaign),
            Pair(AdminSection.AUDIT_LOGS, Icons.Default.Security)
        )

        navItems.forEach { (section, icon) ->
            val isSelected = adminUiState.currentSection == section ||
                    (section == AdminSection.COMPLAINTS && adminUiState.currentSection == AdminSection.COMPLAINT_DETAIL)

            NavigationBarItem(
                selected = isSelected,
                onClick = { adminViewModel.setSection(section) },
                icon = { Icon(icon, contentDescription = section.displayName) },
                label = { Text(section.displayName, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF38BDF8),
                    selectedTextColor = Color(0xFF38BDF8),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color(0xFF0284C7).copy(alpha = 0.25f)
                ),
                modifier = Modifier.testTag("admin_mobile_nav_${section.name.lowercase()}")
            )
        }
    }
}
