package com.example.zeromile.ui.admin

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.data.model.ComplaintStatusHistoryRecord
import com.example.zeromile.data.model.ComplaintTrackingDetails
import com.example.zeromile.data.model.ComplaintUpdateRecord
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.components.evidence.EvidenceGalleryCard
import com.example.zeromile.ui.components.location.ComplaintLocationCard
import com.example.zeromile.ui.viewmodel.AdminUiState
import com.example.zeromile.ui.viewmodel.AdminViewModel
import com.example.zeromile.ui.viewmodel.UiState

/**
 * Phase 7 Administrative Complaint Detail View
 * Comprehensive grievance details, AI assistance disclaimer, municipal team assignment, status transitions, official updates, and deletion.
 */
@Composable
fun AdminComplaintDetailView(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
            .testTag("admin_complaint_detail_view"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Back Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { adminViewModel.closeComplaintDetail() },
                    modifier = Modifier.testTag("admin_back_to_complaints_table_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Complaints",
                        tint = Color(0xFF38BDF8)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Complaints Table",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            IconButton(
                onClick = { adminViewModel.refreshCurrentDetail() },
                modifier = Modifier.testTag("admin_refresh_complaint_detail_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        // Action Feedback / Error Banner
        if (adminUiState.actionFeedbackMessage != null) {
            Surface(
                color = Color(0xFF065F46).copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("admin_action_feedback_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = adminUiState.actionFeedbackMessage,
                        color = Color(0xFFD1FAE5),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    IconButton(onClick = { adminViewModel.clearFeedbackMessage() }) {
                        Text("✕", color = Color(0xFFD1FAE5))
                    }
                }
            }
        }

        if (adminUiState.actionErrorMessage != null) {
            Surface(
                color = Color(0xFF7F1D1D).copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("admin_action_error_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = adminUiState.actionErrorMessage,
                        color = Color(0xFFFEE2E2),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    IconButton(onClick = { adminViewModel.clearFeedbackMessage() }) {
                        Text("✕", color = Color(0xFFFEE2E2))
                    }
                }
            }
        }

        // Detail Content
        when (val state = adminUiState.complaintDetailState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            }
            is UiState.Error -> {
                Surface(
                    color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Failed to load grievance details: ${state.message}",
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
            is UiState.Success -> {
                val details = state.data
                val complaint = details.complaint

                // Top Header Card
                ComplaintDetailHeader(complaint = complaint)

                // Administrative Action Controls
                AdminActionToolbar(
                    adminViewModel = adminViewModel,
                    complaint = complaint
                )

                // Two columns layout: Left (Details & AI), Right (History & Updates)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Grievance info & AI Analysis
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        GrievanceInfoCard(complaint = complaint)
                        AiAnalysisCard(complaint = complaint)

                        // Phase 9 Location & Maps Card
                        ComplaintLocationCard(
                            locationText = complaint.locationText,
                            latitude = complaint.latitude,
                            longitude = complaint.longitude,
                            accuracyMeters = complaint.locationAccuracyMeters,
                            source = complaint.locationSource,
                            wardName = complaint.ward?.name,
                            wardNumber = complaint.ward?.wardNumber?.toString(),
                            showExternalMapButton = true
                        )

                        // Phase 9 Photo Evidence Gallery
                        EvidenceGalleryCard(
                            evidence = details.evidence,
                            complaintId = complaint.id,
                            currentUserId = adminUiState.adminUser?.id,
                            isAdmin = true,
                            onUploadAdditionalPhoto = { name, mime, bytes ->
                                adminViewModel.uploadEvidence(complaint.id, name, mime, bytes)
                            },
                            onDeletePhoto = { evidenceId, storagePath ->
                                adminViewModel.deleteEvidence(complaint.id, evidenceId, storagePath)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Right Column: Operational Status & Official Updates & History
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        OperationalStatusCard(complaint = complaint, details = details)
                        OfficialUpdatesCard(
                            updates = details.updates,
                            onAddUpdateClick = { adminViewModel.setAddUpdateDialogVisible(true) }
                        )
                        StatusHistoryCard(history = details.history)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplaintDetailHeader(complaint: ComplaintRecord) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth().testTag("admin_detail_header_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = complaint.complaintNumber,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val badgeStatus = when (complaint.status.lowercase()) {
                        "submitted" -> BadgeStatus.WARNING
                        "assigned", "in progress" -> BadgeStatus.INFO
                        "resolved" -> BadgeStatus.SUCCESS
                        else -> BadgeStatus.DEFAULT
                    }
                    ZeromileStatusBadge(text = complaint.status, status = badgeStatus)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${complaint.service?.name ?: "Civic Issue"} • Registered on ${complaint.createdAt ?: ""}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
            }

            Surface(
                color = when (complaint.priority.lowercase()) {
                    "critical" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                    "high" -> Color(0xFFF97316).copy(alpha = 0.2f)
                    "medium" -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                    else -> Color(0xFF10B981).copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Priority: ${complaint.priority}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (complaint.priority.lowercase()) {
                            "critical" -> Color(0xFFEF4444)
                            "high" -> Color(0xFFF97316)
                            "medium" -> Color(0xFFFBBF24)
                            else -> Color(0xFF10B981)
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AdminActionToolbar(
    adminViewModel: AdminViewModel,
    complaint: ComplaintRecord
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth().testTag("admin_actions_toolbar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Administrative Actions:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                ),
                modifier = Modifier.padding(end = 4.dp)
            )

            // Assign Team Button
            ZeromileButton(
                text = if (complaint.assignedTeamName != null) "Reassign Team" else "Assign Team",
                icon = Icons.Default.Groups,
                onClick = { adminViewModel.setAssignTeamDialogVisible(true) },
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier.testTag("admin_open_assign_team_dialog_button")
            )

            // Update Status Button
            ZeromileButton(
                text = "Transition Status",
                icon = Icons.Default.Sync,
                onClick = { adminViewModel.setStatusUpdateDialogVisible(true) },
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.testTag("admin_open_status_dialog_button")
            )

            // Add Official Update Button
            ZeromileButton(
                text = "Post Update",
                icon = Icons.Default.PostAdd,
                onClick = { adminViewModel.setAddUpdateDialogVisible(true) },
                variant = ButtonVariant.OUTLINE,
                modifier = Modifier.testTag("admin_open_add_update_dialog_button")
            )

            Spacer(modifier = Modifier.weight(1f))

            // Delete Complaint (Admin-only Danger Zone)
            ZeromileButton(
                text = "Delete",
                icon = Icons.Default.DeleteForever,
                onClick = { adminViewModel.setDeleteConfirmDialogVisible(true) },
                variant = ButtonVariant.DESTRUCTIVE,
                modifier = Modifier.testTag("admin_open_delete_dialog_button")
            )
        }
    }
}

@Composable
private fun GrievanceInfoCard(complaint: ComplaintRecord) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth().testTag("admin_grievance_info_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Citizen Grievance Details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            // Description
            Column {
                Text("Description", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = complaint.description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFE2E8F0))
                )
            }

            // Original Voice Transcript if voice input
            if (complaint.originalTranscript.isNotBlank()) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Original Voice Transcript (${complaint.language})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${complaint.originalTranscript}\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                }
            }

            // Location & Ward
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Location", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(complaint.locationText, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ward", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(complaint.ward?.name ?: "Ward ${complaint.ward?.wardNumber ?: ""}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Citizen ID & Input mode
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Citizen / Submitter ID", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(complaint.userId ?: "Anonymous", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Input Channel", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${complaint.inputMode.replaceFirstChar { it.uppercase() }} (${complaint.language.uppercase()})",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AiAnalysisCard(complaint: ComplaintRecord) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().testTag("admin_ai_analysis_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Civic Analysis & Recommendation",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    )
                    Text(
                        text = "Automated classification assistance only — not official municipal determination",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            if (complaint.aiSummary != null) {
                Column {
                    Text("AI Executive Summary", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(complaint.aiSummary, color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (complaint.aiReason != null) {
                Column {
                    Text("Classification Rationale", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(complaint.aiReason, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Confidence Score", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${((complaint.aiConfidence ?: 0.9) * 100).toInt()}%",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Suggested Priority", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        complaint.priority,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun OperationalStatusCard(
    complaint: ComplaintRecord,
    details: ComplaintTrackingDetails
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth().testTag("admin_operational_status_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Operational Assignment",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            // Assigned Team
            val teamName = complaint.assignedTeamName ?: details.assignedTeam
            Column {
                Text("Assigned Operational Squad", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                if (teamName != null) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(teamName, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Text("No squad assigned yet. Click 'Assign Team' to deploy.", color = Color(0xFFF59E0B), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Resolution info if resolved
            if (complaint.resolutionText != null || details.resolutionNote != null) {
                Surface(
                    color = Color(0xFF065F46).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Official Resolution Note", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(complaint.resolutionText ?: details.resolutionNote ?: "", color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficialUpdatesCard(
    updates: List<ComplaintUpdateRecord>,
    onAddUpdateClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth().testTag("admin_official_updates_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Official Citizen Updates (${updates.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                IconButton(onClick = onAddUpdateClick, modifier = Modifier.testTag("add_update_header_btn")) {
                    Icon(Icons.Default.PostAdd, contentDescription = "Add Update", tint = Color(0xFF38BDF8))
                }
            }

            if (updates.isEmpty()) {
                Text(
                    text = "No public updates issued for this complaint yet.",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    updates.forEach { update ->
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth().testTag("admin_update_item_${update.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = update.officialName ?: "Nagpur Municipal Corporation",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = update.createdAt?.take(16)?.replace("T", " ") ?: "",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = update.message,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHistoryCard(history: List<ComplaintStatusHistoryRecord>) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth().testTag("admin_status_history_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status History & Audit Trail",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            if (history.isEmpty()) {
                Text("No history transitions recorded.", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    history.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.status,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = item.createdAt?.take(16)?.replace("T", " ") ?: "",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                if (!item.note.isNullOrBlank()) {
                                    Text(
                                        text = item.note,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                                    )
                                }
                                if (!item.changedBy.isNullOrBlank()) {
                                    Text(
                                        text = "By: ${item.changedBy}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
