package com.example.zeromile.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.LiveNotificationToastBanner
import com.example.zeromile.ui.components.NotificationsBottomSheet
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileInput
import com.example.zeromile.ui.components.ZeromileModal
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.viewmodel.CivicUiState
import com.example.zeromile.ui.viewmodel.CivicViewModel
import com.example.zeromile.ui.viewmodel.NavTab
import com.example.zeromile.ui.viewmodel.UiState

@Composable
fun MainScreen(
    viewModel: CivicViewModel,
    onOpenAdminPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val primaryNavTabs = listOf(NavTab.HOME, NavTab.SERVICES, NavTab.AI, NavTab.ACTIVITY, NavTab.PROFILE)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Adaptive / Desktop / Tablet Layout using NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("desktop_nav_rail"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ZM",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                ) {
                    primaryNavTabs.forEach { tab ->
                        val (label, icon) = getTabMeta(tab)
                        NavigationRailItem(
                            selected = uiState.currentTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        TabContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenAdminPortal = onOpenAdminPortal
                        )
                    }
                }
            }
        } else {
            // Handheld Mobile Layout with Bottom NavigationBar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        primaryNavTabs.forEach { tab ->
                            val (label, icon) = getTabMeta(tab)
                            NavigationBarItem(
                                selected = uiState.currentTab == tab,
                                onClick = { viewModel.setTab(tab) },
                                alwaysShowLabel = true,
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (uiState.currentTab == tab) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    TabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenAdminPortal = onOpenAdminPortal
                    )

                    // Realtime Live Toast Banner (Phase 8)
                    LiveNotificationToastBanner(
                        toast = uiState.activeLiveToast,
                        onDismiss = { viewModel.dismissLiveToast() },
                        onViewComplaint = { complaintId ->
                            viewModel.dismissLiveToast()
                            viewModel.openComplaintTracking(complaintId)
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                }
            }
        }

        // Realtime Notifications Sheet (Phase 8)
        if (uiState.showNotificationsSheet) {
            NotificationsBottomSheet(
                state = uiState.notificationsState,
                onDismiss = { viewModel.setShowNotificationsSheet(false) },
                onNotificationClick = { notif -> viewModel.onNotificationClicked(notif) },
                onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
                onMarkSingleAsRead = { id -> viewModel.markNotificationRead(id) }
            )
        }

        // Service Detail Modal
        val selectedService = uiState.selectedServiceForModal
        if (selectedService != null) {
            ZeromileModal(
                title = selectedService.name,
                onDismissRequest = { viewModel.openServiceDetail(null) },
                testTag = "service_detail_modal"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZeromileStatusBadge(
                            text = selectedService.department?.name ?: "Municipal Corporation",
                            status = BadgeStatus.INFO,
                            testTag = "modal_dept_badge"
                        )
                        ZeromileStatusBadge(
                            text = "Ward 32 • Active",
                            status = BadgeStatus.ACTIVE,
                            testTag = "modal_status_badge"
                        )
                    }

                    if (!selectedService.description.isNullOrBlank()) {
                        Text(
                            text = selectedService.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Service Specifications",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Type: ${selectedService.serviceType.replaceFirstChar { it.uppercase() }}\nJurisdiction: Nagpur Municipal Corporation\nResolution standard: 48 to 72 hours",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ZeromileButton(
                        text = "Report this Issue in Nagpur",
                        icon = Icons.Default.Send,
                        onClick = {
                            viewModel.openServiceDetail(null)
                            viewModel.openVoiceAssistant("Reporting issue: ${selectedService.name}")
                        },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "modal_proceed_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    uiState: CivicUiState,
    viewModel: CivicViewModel,
    onOpenAdminPortal: () -> Unit = {}
) {
    AnimatedContent(
        targetState = uiState.currentTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tab_transition"
    ) { currentTab ->
        when (currentTab) {
            NavTab.HOME -> {
                HomeScreen(
                    uiState = uiState,
                    onNavigateTab = { viewModel.setTab(it) },
                    onOpenAiModal = { prompt -> viewModel.openVoiceAssistant(prompt) },
                    onOpenServiceDetailById = { serviceId ->
                        // Find service in list or seed
                        val found = (uiState.servicesState as? UiState.Success)?.data?.find { it.id == serviceId }
                            ?: com.example.zeromile.data.repository.SeedData.services.find { it.id == serviceId }
                        viewModel.openServiceDetail(found)
                    },
                    onOpenNotifications = { viewModel.setShowNotificationsSheet(true) }
                )
            }
            NavTab.SERVICES -> {
                ServicesScreen(
                    uiState = uiState,
                    onCategorySelected = { categoryId -> viewModel.selectCategory(categoryId) },
                    onServiceSelected = { service -> viewModel.openServiceDetail(service) },
                    onRetry = { viewModel.retryLoading() }
                )
            }
            NavTab.AI -> {
                AiScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
            NavTab.ACTIVITY -> {
                ActivityScreen(
                    uiState = uiState,
                    onNavigateServices = { viewModel.setTab(NavTab.SERVICES) },
                    onRefresh = { viewModel.loadComplaints() },
                    onSelectComplaint = { complaintId -> viewModel.openComplaintDetail(complaintId) },
                    onBackFromDetail = { viewModel.closeComplaintDetail() },
                    onRefreshDetail = { viewModel.refreshComplaintDetail() },
                    onUploadEvidence = { cid, fn, mime, bytes -> viewModel.uploadAdditionalEvidence(cid, fn, mime, bytes) },
                    onDeleteEvidence = { cid, eid, sp -> viewModel.deleteEvidence(cid, eid, sp) }
                )
            }
            NavTab.PROFILE -> {
                ProfileScreen(
                    uiState = uiState,
                    onOpenAdminPortal = onOpenAdminPortal
                )
            }
            NavTab.EMERGENCY -> {
                EmergencyScreen(
                    uiState = uiState,
                    onBack = { viewModel.setTab(NavTab.HOME) },
                    onRetry = { viewModel.loadEmergencyServices() }
                )
            }
            NavTab.UPDATES -> {
                CivicUpdatesScreen(
                    uiState = uiState,
                    onBack = { viewModel.setTab(NavTab.HOME) },
                    onWardSelected = { wardId -> viewModel.setWardFilterForUpdates(wardId) },
                    onRetry = { viewModel.loadCivicUpdates(uiState.selectedWardFilterForUpdates) }
                )
            }
        }
    }
}

private fun getTabMeta(tab: NavTab): Pair<String, ImageVector> {
    return when (tab) {
        NavTab.HOME -> Pair("Home", Icons.Default.Home)
        NavTab.SERVICES -> Pair("Services", Icons.Default.Build)
        NavTab.AI -> Pair("AI", Icons.Default.AutoAwesome)
        NavTab.ACTIVITY -> Pair("Activity", Icons.Default.History)
        NavTab.PROFILE -> Pair("Profile", Icons.Default.AccountCircle)
        NavTab.EMERGENCY -> Pair("Emergency", Icons.Default.Warning)
        NavTab.UPDATES -> Pair("Updates", Icons.Default.Campaign)
    }
}
