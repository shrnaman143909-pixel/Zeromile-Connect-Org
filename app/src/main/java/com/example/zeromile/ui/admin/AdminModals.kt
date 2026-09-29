package com.example.zeromile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.Team
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.viewmodel.AdminUiState
import com.example.zeromile.ui.viewmodel.AdminViewModel
import com.example.zeromile.ui.viewmodel.UiState

/**
 * Administrative Action Modals & Dialogs
 * - Assign Team Modal
 * - Transition Status Modal
 * - Add Official Update Modal
 * - Delete Complaint Irreversible Confirmation Modal
 * - Administrator Profile Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModalsContainer(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel
) {
    if (adminUiState.showAssignTeamDialog) {
        AssignTeamModal(
            adminUiState = adminUiState,
            adminViewModel = adminViewModel,
            onDismiss = { adminViewModel.setAssignTeamDialogVisible(false) }
        )
    }

    if (adminUiState.showStatusUpdateDialog) {
        StatusUpdateModal(
            adminUiState = adminUiState,
            adminViewModel = adminViewModel,
            onDismiss = { adminViewModel.setStatusUpdateDialogVisible(false) }
        )
    }

    if (adminUiState.showAddUpdateDialog) {
        AddOfficialUpdateModal(
            adminUiState = adminUiState,
            adminViewModel = adminViewModel,
            onDismiss = { adminViewModel.setAddUpdateDialogVisible(false) }
        )
    }

    if (adminUiState.showDeleteConfirmDialog) {
        DeleteComplaintModal(
            adminUiState = adminUiState,
            adminViewModel = adminViewModel,
            onDismiss = { adminViewModel.setDeleteConfirmDialogVisible(false) }
        )
    }

    if (adminUiState.showProfileDialog) {
        AdminProfileModal(
            adminUiState = adminUiState,
            adminViewModel = adminViewModel,
            onDismiss = { adminViewModel.setProfileDialogVisible(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignTeamModal(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val teams = when (val state = adminUiState.teamsState) {
        is UiState.Success -> state.data
        else -> emptyList()
    }

    var selectedTeamId by remember { mutableStateOf(teams.firstOrNull()?.id ?: "") }
    var noteInput by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_assign_team_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Assign Operational Squad",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Text(
                    text = "Select municipal operational team and provide field instructions:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )

                // Teams List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    teams.forEach { team ->
                        val isSelected = selectedTeamId == team.id
                        Surface(
                            onClick = { selectedTeamId = team.id },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("team_option_${team.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTeamId = team.id },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF38BDF8),
                                        unselectedColor = Color(0xFF94A3B8)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = team.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    )
                                    if (!team.description.isNullOrBlank()) {
                                        Text(
                                            text = team.description,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Field Instructions / Assignment Note
                Column {
                    Text(
                        text = "Operational Instructions (Optional)",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("e.g. Inspect drainage block near Dharampeth square.", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_assign_note_input"),
                        minLines = 2
                    )
                }

                // Error if any
                if (adminUiState.actionErrorMessage != null) {
                    Text(
                        text = adminUiState.actionErrorMessage,
                        color = Color(0xFFF87171),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZeromileButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.OUTLINE
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val selectedTeam = teams.find { it.id == selectedTeamId }
                    ZeromileButton(
                        text = if (adminUiState.actionInProgress) "Assigning..." else "Assign Team",
                        onClick = {
                            if (selectedTeam != null) {
                                adminViewModel.assignTeam(selectedTeam.id, selectedTeam.name, noteInput)
                            }
                        },
                        variant = ButtonVariant.PRIMARY,
                        enabled = !adminUiState.actionInProgress && selectedTeam != null,
                        modifier = Modifier.testTag("admin_confirm_assign_team_button")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusUpdateModal(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val statuses = listOf("Submitted", "Assigned", "In Progress", "Resolved", "Closed")
    var selectedStatus by remember { mutableStateOf("In Progress") }
    var noteInput by remember { mutableStateOf("") }
    var resolutionTextInput by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_status_update_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Transition Complaint Status",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Text(
                    text = "Update municipal workflow state and record official resolution audit trail:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )

                // Status Options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { st ->
                        val isSelected = selectedStatus == st
                        Surface(
                            onClick = { selectedStatus = st },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("status_option_$st")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedStatus = st },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF38BDF8),
                                        unselectedColor = Color(0xFF94A3B8)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = st,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // If Resolved: Resolution Summary
                if (selectedStatus == "Resolved") {
                    Column {
                        Text(
                            text = "Resolution Summary / Actions Taken (Mandatory)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = resolutionTextInput,
                            onValueChange = { resolutionTextInput = it },
                            placeholder = { Text("e.g. Pipeline patched and pressure restored to 2.4 bar.", color = Color(0xFF64748B)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_resolution_text_input"),
                            minLines = 2
                        )
                    }
                }

                // Transition Note
                Column {
                    Text(
                        text = "Internal Audit Note",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("e.g. Field inspection completed by junior engineer.", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_status_note_input"),
                        minLines = 2
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZeromileButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.OUTLINE
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val canSubmit = if (selectedStatus == "Resolved") resolutionTextInput.isNotBlank() else true
                    ZeromileButton(
                        text = if (adminUiState.actionInProgress) "Updating..." else "Save Transition",
                        onClick = {
                            adminViewModel.updateStatus(
                                newStatus = selectedStatus,
                                note = noteInput,
                                resolutionText = if (selectedStatus == "Resolved") resolutionTextInput else null
                            )
                        },
                        variant = ButtonVariant.PRIMARY,
                        enabled = !adminUiState.actionInProgress && canSubmit,
                        modifier = Modifier.testTag("admin_confirm_status_update_button")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOfficialUpdateModal(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    var officialNameInput by remember { mutableStateOf("Nagpur Municipal Corporation") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_add_update_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Publish Official Citizen Update",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Text(
                    text = "This official bulletin will appear immediately on the citizen's complaint tracking timeline:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )

                // Message Input
                Column {
                    Text(
                        text = "Update Message",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("e.g. Sanitary inspector conducted on-site decibel assessment.", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_official_message_input"),
                        minLines = 3
                    )
                }

                // Official Name
                Column {
                    Text(
                        text = "Official Authority Label",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = officialNameInput,
                        onValueChange = { officialNameInput = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_official_name_input"),
                        singleLine = true
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZeromileButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.OUTLINE
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ZeromileButton(
                        text = if (adminUiState.actionInProgress) "Publishing..." else "Publish to Citizen",
                        onClick = {
                            adminViewModel.addOfficialUpdate(messageInput, officialNameInput)
                        },
                        variant = ButtonVariant.PRIMARY,
                        enabled = !adminUiState.actionInProgress && messageInput.isNotBlank(),
                        modifier = Modifier.testTag("admin_confirm_add_update_button")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteComplaintModal(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var reasonInput by remember { mutableStateOf("") }
    var confirmCheckbox by remember { mutableStateOf(false) }

    val complaintId = adminUiState.selectedComplaintId ?: ""

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_delete_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Permanently Delete Complaint?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                    )
                }

                Text(
                    text = "This action CANNOT be undone. The complaint, its status history, and municipal updates will be permanently purged from the database. A permanent immutable record will be stored in the admin audit log.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0))
                )

                // Mandatory deletion reason
                Column {
                    Text(
                        text = "Reason for Deletion (Mandatory for Audit Logs)",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        placeholder = { Text("e.g. Duplicate complaint registered by error; consolidated with NMC-2026-001248.", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_delete_reason_input"),
                        minLines = 2
                    )
                }

                // Two-step Confirmation checkbox
                Surface(
                    onClick = { confirmCheckbox = !confirmCheckbox },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = confirmCheckbox,
                            onCheckedChange = { confirmCheckbox = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = Color(0xFFEF4444),
                                uncheckedColor = Color(0xFF94A3B8)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I confirm that I am an authorized administrator and intend to permanently delete this complaint.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZeromileButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.OUTLINE
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ZeromileButton(
                        text = if (adminUiState.actionInProgress) "Deleting..." else "Delete Permanently",
                        onClick = {
                            adminViewModel.deleteComplaint(reasonInput)
                        },
                        variant = ButtonVariant.DESTRUCTIVE,
                        enabled = !adminUiState.actionInProgress && confirmCheckbox && reasonInput.isNotBlank(),
                        modifier = Modifier.testTag("admin_confirm_permanent_delete_button")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminProfileModal(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val admin = adminUiState.adminUser

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_profile_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Administrator Profile",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Nagpur Municipal Corporation (NMC)",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileInfoRow("Account Email", admin?.email ?: "shrnavan1439009@gmail.com")
                        ProfileInfoRow("Role Definition", "Administrator (Full Municipal Privileges)")
                        ProfileInfoRow("Database Policy", "RLS is_admin() / user_roles Enforcement")
                        ProfileInfoRow("Authentication", "Supabase Auth GoTrue")
                        ProfileInfoRow("Municipal Scope", "Nagpur City Wards 1 to 156")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZeromileButton(
                        text = "Sign Out",
                        onClick = {
                            onDismiss()
                            adminViewModel.signOut()
                        },
                        variant = ButtonVariant.DESTRUCTIVE,
                        modifier = Modifier.testTag("admin_dialog_sign_out_button")
                    )

                    ZeromileButton(
                        text = "Close",
                        onClick = onDismiss,
                        variant = ButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}
