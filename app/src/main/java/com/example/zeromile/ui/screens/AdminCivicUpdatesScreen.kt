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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.CivicUpdate
import com.example.zeromile.data.model.CreateCivicUpdatePayload
import com.example.zeromile.data.repository.AdminRepository
import com.example.zeromile.data.repository.SeedData
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.ZeromileModal
import com.example.zeromile.ui.components.ZeromilePageHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCivicUpdatesScreen(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var updates by remember { mutableStateOf<List<CivicUpdate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateModal by remember { mutableStateOf(false) }
    var deleteConfirmUpdate by remember { mutableStateOf<CivicUpdate?>(null) }

    fun refreshUpdates() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = adminRepository.getCivicUpdates()
            result.onSuccess { list ->
                updates = list
                isLoading = false
            }.onFailure { error ->
                errorMessage = error.localizedMessage ?: "Failed to fetch updates"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshUpdates()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("admin_civic_updates_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("admin_updates_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Admin Dashboard",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                ZeromilePageHeader(
                    title = "Civic Updates & Broadcasts",
                    subtitle = "Manage public alerts, traffic diversions & ward advisories",
                    testTag = "admin_updates_header"
                )
            }

            ZeromileButton(
                text = "New Alert",
                icon = Icons.Default.Add,
                onClick = { showCreateModal = true },
                variant = ButtonVariant.PRIMARY,
                testTag = "create_alert_button"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = errorMessage ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ZeromileButton(
                        text = "Retry",
                        icon = Icons.Default.Refresh,
                        onClick = { refreshUpdates() },
                        variant = ButtonVariant.PRIMARY
                    )
                }
            }
        } else if (updates.isEmpty()) {
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
                        text = "No civic updates published yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("admin_updates_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(updates, key = { it.id }) { update ->
                    AdminCivicUpdateItem(
                        update = update,
                        onToggleActive = { active ->
                            coroutineScope.launch {
                                adminRepository.updateCivicUpdate(update.id, mapOf("is_active" to active))
                                refreshUpdates()
                            }
                        },
                        onDelete = { deleteConfirmUpdate = update }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Create Update Modal
    if (showCreateModal) {
        CreateUpdateModal(
            onDismiss = { showCreateModal = false },
            onSubmit = { title, description, category, priority, wardId, active ->
                coroutineScope.launch {
                    adminRepository.createCivicUpdate(
                        CreateCivicUpdatePayload(
                            title = title,
                            description = description,
                            category = category,
                            priority = priority,
                            wardId = wardId,
                            isActive = active
                        )
                    )
                    showCreateModal = false
                    refreshUpdates()
                }
            }
        )
    }

    // Delete Confirmation
    val toDelete = deleteConfirmUpdate
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmUpdate = null },
            title = { Text("Delete Civic Update?") },
            text = { Text("Are you sure you want to delete '${toDelete.title}'? This action is recorded in the admin audit log.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            adminRepository.deleteCivicUpdate(toDelete.id, toDelete.title)
                            deleteConfirmUpdate = null
                            refreshUpdates()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmUpdate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminCivicUpdateItem(
    update: CivicUpdate,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    ZeromileCard(
        testTag = "admin_update_card_${update.id}"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CivicPriorityBadge(priority = update.priority)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = update.category ?: "General Alert",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (update.isActive) "Active" else "Hidden",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (update.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = update.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.testTag("switch_active_${update.id}")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_update_${update.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = update.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )

            if (!update.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = update.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        text = if (update.ward != null) "Ward ${update.ward.wardNumber}: ${update.ward.name}" else "City-Wide Nagpur",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }

                if (!update.publishedAt.isNullOrBlank()) {
                    Text(
                        text = "Published: ${update.publishedAt.take(10)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUpdateModal(
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String, category: String, priority: String, wardId: String?, active: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Water Supply") }
    var priority by remember { mutableStateOf("normal") }
    var selectedWardId by remember { mutableStateOf<String?>(null) }
    var isActive by remember { mutableStateOf(true) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var wardExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Water Supply", "Traffic Alert", "Road Works", "Sanitation", "Public Health", "Power Alert", "General Advisory")
    val priorities = listOf("urgent", "high", "normal", "low")
    val wards = SeedData.wards

    ZeromileModal(
        title = "Publish Civic Alert",
        onDismissRequest = onDismiss,
        testTag = "create_update_modal"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                placeholder = { Text("e.g. Water Pipeline Maintenance - Dharampeth") },
                modifier = Modifier.fillMaxWidth().testTag("input_update_title")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                placeholder = { Text("Details of disruption, hours, alternate arrangements...") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("input_update_description")
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().testTag("select_update_category")
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Priority Dropdown
            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = !priorityExpanded }
            ) {
                OutlinedTextField(
                    value = priority.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().testTag("select_update_priority")
                )
                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false }
                ) {
                    priorities.forEach { prio ->
                        DropdownMenuItem(
                            text = { Text(prio.uppercase()) },
                            onClick = {
                                priority = prio
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            // Ward Dropdown
            ExposedDropdownMenuBox(
                expanded = wardExpanded,
                onExpandedChange = { wardExpanded = !wardExpanded }
            ) {
                val wardLabel = if (selectedWardId == null) "City-Wide (All Nagpur)" else {
                    val w = wards.find { it.id == selectedWardId }
                    if (w != null) "Ward ${w.wardNumber}: ${w.name}" else "City-Wide"
                }
                OutlinedTextField(
                    value = wardLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ward Scope") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().testTag("select_update_ward")
                )
                ExposedDropdownMenu(
                    expanded = wardExpanded,
                    onDismissRequest = { wardExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("City-Wide (All Nagpur)") },
                        onClick = {
                            selectedWardId = null
                            wardExpanded = false
                        }
                    )
                    wards.forEach { w ->
                        DropdownMenuItem(
                            text = { Text("Ward ${w.wardNumber}: ${w.name}") },
                            onClick = {
                                selectedWardId = w.id
                                wardExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ZeromileButton(
                text = "Broadcast Alert",
                icon = Icons.Default.Campaign,
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onSubmit(title, description, category, priority, selectedWardId, isActive)
                    }
                },
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                testTag = "submit_broadcast_button"
            )
        }
    }
}
