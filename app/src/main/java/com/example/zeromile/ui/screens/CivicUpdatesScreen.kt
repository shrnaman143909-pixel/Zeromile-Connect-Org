package com.example.zeromile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.CivicUpdate
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.ZeromileModal
import com.example.zeromile.ui.components.ZeromilePageHeader
import com.example.zeromile.ui.components.ZeromileSkeletonCard
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.viewmodel.CivicUiState
import com.example.zeromile.ui.viewmodel.UiState

@Composable
fun CivicUpdatesScreen(
    uiState: CivicUiState,
    onBack: () -> Unit,
    onWardSelected: (String?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPriorityFilter by remember { mutableStateOf<String?>(null) }
    var selectedUpdateForModal by remember { mutableStateOf<CivicUpdate?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("civic_updates_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("updates_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            ZeromilePageHeader(
                title = "Civic Updates & Alerts",
                subtitle = "Official municipal bulletins, road closures & alerts for Nagpur",
                testTag = "updates_page_header"
            )
        }

        // Horizontal Ward filter chip row
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 6.dp)
                .testTag("ward_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedWardFilterForUpdates == null,
                onClick = { onWardSelected(null) },
                label = { Text("All Nagpur") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("filter_ward_all")
            )

            val wards = com.example.zeromile.data.repository.SeedData.wards
            wards.forEach { ward ->
                val isSelected = uiState.selectedWardFilterForUpdates == ward.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onWardSelected(if (isSelected) null else ward.id) },
                    label = { Text("Ward ${ward.wardNumber}: ${ward.name}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_ward_${ward.wardNumber}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Civic Updates Content
        when (val state = uiState.civicUpdatesState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("updates_loading_container"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        ZeromileSkeletonCard(height = 110)
                    }
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("updates_error_container"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Failed to load civic updates",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ZeromileButton(
                        text = "Try Again",
                        icon = Icons.Default.Refresh,
                        onClick = onRetry,
                        variant = ButtonVariant.PRIMARY,
                        testTag = "updates_retry_button"
                    )
                }
            }
            is UiState.Success -> {
                val updates = state.data
                if (updates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No active civic updates for this ward.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("updates_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(updates, key = { it.id }) { update ->
                            CivicUpdateCard(
                                update = update,
                                onClick = { selectedUpdateForModal = update }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // Advisory Detail Modal
    val modalUpdate = selectedUpdateForModal
    if (modalUpdate != null) {
        ZeromileModal(
            title = modalUpdate.title,
            onDismissRequest = { selectedUpdateForModal = null },
            testTag = "update_detail_modal"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZeromileStatusBadge(
                        text = modalUpdate.category ?: "General",
                        status = BadgeStatus.INFO
                    )
                    CivicPriorityBadge(priority = modalUpdate.priority)
                }

                if (!modalUpdate.description.isNullOrBlank()) {
                    Text(
                        text = modalUpdate.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
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
                            text = "Scope & Jurisdiction",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (modalUpdate.ward != null) "Ward ${modalUpdate.ward.wardNumber} • ${modalUpdate.ward.name}" else "City-Wide • Nagpur Municipal Corporation",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        if (!modalUpdate.publishedAt.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Published: ${modalUpdate.publishedAt.take(10)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ZeromileButton(
                    text = "Close Bulletin",
                    onClick = { selectedUpdateForModal = null },
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "close_bulletin_button"
                )
            }
        }
    }
}

@Composable
fun CivicUpdateCard(
    update: CivicUpdate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ZeromileCard(
        onClick = onClick,
        modifier = modifier,
        testTag = "civic_update_card_${update.id}"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val catIcon = when (update.category?.lowercase()) {
                        "water supply" -> Icons.Default.Info
                        "traffic alert" -> Icons.Default.Warning
                        "road works" -> Icons.Default.Info
                        else -> Icons.Default.Campaign
                    }
                    Icon(
                        imageVector = catIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = update.category ?: "Bulletin",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                CivicPriorityBadge(priority = update.priority)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = update.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            )

            if (!update.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = update.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    ),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (update.ward != null) "Ward ${update.ward.wardNumber}, ${update.ward.name}" else "City-Wide Nagpur",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }

                if (!update.publishedAt.isNullOrBlank()) {
                    Text(
                        text = update.publishedAt.take(10),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
    }
}

@Composable
fun CivicPriorityBadge(priority: String) {
    val (bgColor, textColor, label) = when (priority.lowercase()) {
        "urgent" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "URGENT")
        "high" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "HIGH")
        "normal" -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "NORMAL")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF616161), "INFO")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}
