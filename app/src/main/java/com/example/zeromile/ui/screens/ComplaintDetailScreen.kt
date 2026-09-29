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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.data.model.ComplaintTrackingDetails
import com.example.zeromile.data.model.ComplaintUpdateRecord
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ComplaintLifecycleBadge
import com.example.zeromile.ui.components.ComplaintStatusTimeline
import com.example.zeromile.ui.components.RealtimeStatusChip
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.evidence.EvidenceGalleryCard
import com.example.zeromile.ui.components.location.ComplaintLocationCard
import com.example.ui.theme.CivicAmberAccent
import com.example.ui.theme.CivicSuccess
import com.example.zeromile.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailScreen(
    trackingState: UiState<ComplaintTrackingDetails>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    realtimeState: com.example.zeromile.data.remote.RealtimeConnectionState = com.example.zeromile.data.remote.RealtimeConnectionState.CONNECTED,
    onUploadEvidence: ((complaintId: String, fileName: String, mimeType: String, bytes: ByteArray) -> Unit)? = null,
    onDeleteEvidence: ((complaintId: String, evidenceId: String, storagePath: String) -> Unit)? = null,
    isUploadingEvidence: Boolean = false,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Complaint Tracking",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("complaint_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    RealtimeStatusChip(
                        connectionState = realtimeState,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("complaint_detail_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh tracking details"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("complaint_detail_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (trackingState) {
                is UiState.Loading -> {
                    ComplaintDetailLoadingView()
                }
                is UiState.Error -> {
                    ComplaintDetailErrorView(
                        errorMessage = trackingState.message,
                        onRetry = onRefresh,
                        onBack = onBack
                    )
                }
                is UiState.Success -> {
                    ComplaintDetailContentView(
                        details = trackingState.data,
                        onRefresh = onRefresh,
                        onUploadEvidence = onUploadEvidence,
                        onDeleteEvidence = onDeleteEvidence,
                        isUploadingEvidence = isUploadingEvidence
                    )
                }
            }
        }
    }
}

@Composable
private fun ComplaintDetailLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("complaint_detail_loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Fetching complaint ledger...",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Querying Nagpur Municipal Corporation records and audit history",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ComplaintDetailErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("complaint_detail_error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "We couldn't load this complaint",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ZeromileButton(
                        text = "Go Back",
                        onClick = onBack,
                        variant = ButtonVariant.OUTLINE,
                        modifier = Modifier.weight(1f).height(44.dp)
                    )
                    ZeromileButton(
                        text = "Retry",
                        onClick = onRetry,
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f).height(44.dp),
                        testTag = "complaint_detail_retry_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun ComplaintDetailContentView(
    details: ComplaintTrackingDetails,
    onRefresh: () -> Unit,
    onUploadEvidence: ((complaintId: String, fileName: String, mimeType: String, bytes: ByteArray) -> Unit)? = null,
    onDeleteEvidence: ((complaintId: String, evidenceId: String, storagePath: String) -> Unit)? = null,
    isUploadingEvidence: Boolean = false
) {
    val complaint = details.complaint
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Primary Header Card (ID, Status Badge, Title, Service, Date)
        ZeromileCard(
            testTag = "tracking_header_card"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Complaint ID and Status Badge
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
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(complaint.complaintNumber))
                                copied = true
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("copy_complaint_id_button")
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                                contentDescription = "Copy Complaint ID",
                                tint = if (copied) CivicSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    ComplaintLifecycleBadge(
                        status = complaint.status,
                        testTag = "detail_status_badge"
                    )
                }

                // Service Name & Category
                Text(
                    text = complaint.service?.name ?: "Municipal Grievance",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Date Submitted & Department
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Submitted: ${formatIsoDate(complaint.createdAt)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            }
        }

        // Card 2: Visual Status Timeline (Timeline node list)
        ZeromileCard(
            testTag = "tracking_timeline_card"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status Timeline",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Real-time ledger",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                ComplaintStatusTimeline(
                    currentStatusText = complaint.status,
                    history = details.history,
                    testTag = "detail_status_timeline"
                )
            }
        }

        // Card 3: Municipal Assignment & Resolution Info
        ZeromileCard(
            testTag = "tracking_assignment_card"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Municipal Routing & Assignment",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Assigned Team
                InfoRow(
                    icon = Icons.Default.Group,
                    label = "Assigned Team",
                    value = details.assignedTeam ?: "Pending municipal assignment"
                )

                // Department
                InfoRow(
                    icon = Icons.Default.Business,
                    label = "Responsible Department",
                    value = complaint.department?.name ?: "Nagpur Municipal Corporation"
                )

                // Location / Ward
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Location / Ward",
                    value = complaint.locationText
                )

                // Priority
                InfoRow(
                    icon = Icons.Default.PriorityHigh,
                    label = "Priority Level",
                    value = complaint.priority,
                    valueColor = when (complaint.priority.uppercase()) {
                        "EMERGENCY", "URGENT", "CRITICAL" -> Color(0xFFEF4444)
                        "HIGH" -> CivicAmberAccent
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Resolution section if resolved
                if (complaint.status.equals("Resolved", ignoreCase = true) || !details.resolutionNote.isNullOrBlank()) {
                    HorizontalDivider(color = CivicSuccess.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CivicSuccess.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = CivicSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Resolution Details",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CivicSuccess
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = details.resolutionNote ?: "Complaint has been resolved by the municipal field crew.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (!details.resolvedAt.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Resolved on: ${formatIsoDate(details.resolvedAt)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 3.5: Nagpur Ward, GPS Coordinates & Interactive Map Visualizer
        ComplaintLocationCard(
            locationText = complaint.locationText,
            latitude = complaint.latitude,
            longitude = complaint.longitude,
            accuracyMeters = complaint.locationAccuracyMeters,
            source = complaint.locationSource,
            wardName = complaint.ward?.name,
            wardNumber = complaint.ward?.wardNumber?.toString(),
            showExternalMapButton = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Card 3.6: Municipal & Citizen Photo Evidence Gallery (Phase 9)
        EvidenceGalleryCard(
            evidence = details.evidence,
            complaintId = complaint.id,
            currentUserId = complaint.userId,
            isAdmin = false,
            onUploadAdditionalPhoto = { fileName, mimeType, bytes ->
                onUploadEvidence?.invoke(complaint.id, fileName, mimeType, bytes)
            },
            onDeletePhoto = { evidenceId, storagePath ->
                onDeleteEvidence?.invoke(complaint.id, evidenceId, storagePath)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Card 4: Municipal Updates / Notes Section
        ZeromileCard(
            testTag = "tracking_updates_card"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Municipal Updates",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (details.updates.isNotEmpty()) {
                        Text(
                            text = "${details.updates.size} official note${if (details.updates.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                if (details.updates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No official updates yet from municipal officers.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        details.updates.forEach { update ->
                            MunicipalUpdateItem(update = update)
                        }
                    }
                }
            }
        }

        // Card 5: Full Complaint Description & Original Input
        ZeromileCard(
            testTag = "tracking_details_card"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Grievance Description",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Text(
                    text = complaint.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                )

                if (!complaint.originalTranscript.isNullOrBlank() && complaint.originalTranscript != complaint.description) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Citizen Intake (${complaint.inputMode.uppercase()} - ${complaint.language.uppercase()})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = complaint.originalTranscript,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                if (!complaint.aiSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AI Classification: ${complaint.aiSummary}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MunicipalUpdateItem(update: ComplaintUpdateRecord) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("municipal_update_item")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = update.officialName ?: "Nagpur Municipal Corporation",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = formatIsoDate(update.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                )
            }

            Text(
                text = update.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

private fun formatIsoDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "N/A"
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val cleanIso = isoString.replace("Z", "").substringBefore(".")
        val date = parser.parse(cleanIso) ?: return isoString
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy, h:mm a", java.util.Locale.US)
        formatter.format(date)
    } catch (e: Exception) {
        if (isoString.length >= 10) isoString.substring(0, 10) else isoString
    }
}
