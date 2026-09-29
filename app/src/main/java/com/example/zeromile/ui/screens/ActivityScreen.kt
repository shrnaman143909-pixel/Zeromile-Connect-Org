package com.example.zeromile.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.ZeromilePageHeader
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.viewmodel.CivicUiState
import com.example.zeromile.ui.viewmodel.UiState

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import com.example.zeromile.ui.components.RealtimeStatusChip
import com.example.zeromile.ui.components.ComplaintLifecycleBadge

@Composable
fun ActivityScreen(
    uiState: CivicUiState,
    onNavigateServices: () -> Unit,
    onRefresh: () -> Unit = {},
    onSelectComplaint: (String) -> Unit = {},
    onBackFromDetail: () -> Unit = {},
    onRefreshDetail: () -> Unit = {},
    onUploadEvidence: ((complaintId: String, fileName: String, mimeType: String, bytes: ByteArray) -> Unit)? = null,
    onDeleteEvidence: ((complaintId: String, evidenceId: String, storagePath: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (uiState.selectedComplaintId != null) {
        ComplaintDetailScreen(
            trackingState = uiState.complaintTrackingState,
            onBack = onBackFromDetail,
            onRefresh = onRefreshDetail,
            realtimeState = uiState.realtimeConnectionState,
            onUploadEvidence = onUploadEvidence,
            onDeleteEvidence = onDeleteEvidence,
            isUploadingEvidence = uiState.isUploadingEvidence,
            modifier = modifier
        )
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("activity_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ZeromilePageHeader(
            title = stringResource(R.string.activity_header_title),
            subtitle = stringResource(R.string.activity_header_subtitle),
            testTag = "activity_page_header"
        )

        // Architectural Overview for Activity & Resolution Tracking
        ZeromileCard(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            testTag = "activity_overview_card"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Official Grievance Ledger",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real complaints recorded in Supabase with assigned NMC tracking numbers and verification SLAs.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        // Section Title & Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Filed Grievances",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RealtimeStatusChip(connectionState = uiState.realtimeConnectionState)

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp).testTag("activity_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh complaints",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Dynamic State Handling: Loading / Error / Complaints List
        when (val complaintsState = uiState.userComplaintsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Loading grievances from municipal ledger...",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
            is UiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("complaints_error_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Unable to load municipal records",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = complaintsState.message,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ZeromileButton(
                            text = "Retry",
                            onClick = onRefresh,
                            variant = ButtonVariant.OUTLINE,
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val complaints = complaintsState.data
                if (complaints.isEmpty()) {
                    ZeromileCard(testTag = "complaints_empty_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No complaints filed yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Use AI Voice or browse civic services to file your first Nagpur municipal request.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        complaints.forEach { complaint ->
                            ComplaintRecordCard(
                                complaint = complaint,
                                onClick = { onSelectComplaint(complaint.complaintNumber) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ZeromileButton(
            text = "Browse Civic Services",
            onClick = onNavigateServices,
            variant = ButtonVariant.OUTLINE,
            modifier = Modifier.fillMaxWidth(),
            testTag = "activity_browse_services_button"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ComplaintRecordCard(
    complaint: ComplaintRecord,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    ZeromileCard(
        testTag = "activity_item_${complaint.complaintNumber}",
        modifier = modifier.clickable { onClick() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Complaint Number & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = complaint.complaintNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(complaint.complaintNumber))
                            copied = true
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = if (copied) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                ComplaintLifecycleBadge(
                    status = complaint.status,
                    testTag = "badge_${complaint.complaintNumber}"
                )
            }

            // Service & Department
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = complaint.service?.name ?: "Municipal Grievance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Description
            Text(
                text = complaint.description.lines().firstOrNull() ?: complaint.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Metadata Row: Location & Department
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = complaint.locationText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Text(
                    text = complaint.department?.name ?: "Nagpur MC",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // SLA / Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tap to view status history",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    )
                }

                Text(
                    text = "Priority: ${complaint.priority}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (complaint.priority.uppercase()) {
                            "EMERGENCY", "URGENT", "CRITICAL" -> Color(0xFFEF4444)
                            "HIGH" -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
            }
        }
    }
}
