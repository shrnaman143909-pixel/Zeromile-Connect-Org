package com.example.zeromile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.zeromile.ui.viewmodel.AdminSection
import com.example.zeromile.ui.viewmodel.AdminViewModel

/**
 * Phase 7 Master Administrative Screen
 * Enforces authentication, route protection, adaptive sidebar layout, and action modals.
 */
@Composable
fun AdminScreen(
    adminViewModel: AdminViewModel,
    onSwitchToCitizenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adminUiState by adminViewModel.uiState.collectAsState()

    // Load initial data when authenticated
    LaunchedEffect(adminUiState.isAuthenticated) {
        if (adminUiState.isAuthenticated) {
            adminViewModel.loadDashboardData()
            adminViewModel.loadComplaints()
            adminViewModel.loadTeams()
        }
    }

    if (!adminUiState.isAuthenticated) {
        // Unauthenticated -> Render Admin Login Route
        AdminLoginScreen(
            adminUiState = adminUiState,
            adminViewModel = adminViewModel,
            onBackToCitizenApp = onSwitchToCitizenApp,
            modifier = modifier
        )
    } else {
        // Authenticated Administrator -> Render Admin Portal
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0B1329)) // Slate 950 deep canvas
                .testTag("admin_portal_root")
        ) {
            val isWideScreen = maxWidth >= 768.dp

            Scaffold(
                topBar = {
                    AdminHeader(
                        adminUiState = adminUiState,
                        adminViewModel = adminViewModel,
                        onSwitchToCitizenApp = onSwitchToCitizenApp
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        AdminMobileBottomBar(
                            adminUiState = adminUiState,
                            adminViewModel = adminViewModel
                        )
                    }
                },
                containerColor = Color(0xFF0B1329)
            ) { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Desktop Sidebar if wide screen
                    if (isWideScreen) {
                        AdminDesktopSidebar(
                            adminUiState = adminUiState,
                            adminViewModel = adminViewModel
                        )
                    }

                    // Main Content Canvas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color(0xFF0B1329))
                    ) {
                        when (adminUiState.currentSection) {
                            AdminSection.DASHBOARD -> AdminDashboardView(
                                adminUiState = adminUiState,
                                adminViewModel = adminViewModel
                            )
                            AdminSection.COMPLAINTS -> AdminComplaintsView(
                                adminUiState = adminUiState,
                                adminViewModel = adminViewModel
                            )
                            AdminSection.COMPLAINT_DETAIL -> AdminComplaintDetailView(
                                adminUiState = adminUiState,
                                adminViewModel = adminViewModel
                            )
                            AdminSection.TEAMS -> AdminTeamsView(
                                adminUiState = adminUiState,
                                adminViewModel = adminViewModel
                            )
                            AdminSection.SERVICES -> AdminServicesView(
                                adminUiState = adminUiState,
                                adminViewModel = adminViewModel
                            )
                            AdminSection.UPDATES -> com.example.zeromile.ui.screens.AdminCivicUpdatesScreen(
                                adminRepository = adminViewModel.adminRepo,
                                onBack = { adminViewModel.setSection(AdminSection.DASHBOARD) }
                            )
                            AdminSection.AUDIT_LOGS -> AdminAuditLogsView(
                                adminUiState = adminUiState,
                                adminViewModel = adminViewModel
                            )
                        }
                    }
                }
            }

            // Administrative Action Modals (Assign, Update Status, Add Update, Delete, Profile)
            AdminModalsContainer(
                adminUiState = adminUiState,
                adminViewModel = adminViewModel
            )
        }
    }
}
