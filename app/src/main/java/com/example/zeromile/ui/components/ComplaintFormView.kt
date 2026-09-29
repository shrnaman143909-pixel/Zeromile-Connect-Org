package com.example.zeromile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.zeromile.data.model.CivicAiRecommendation
import com.example.zeromile.data.model.ComplaintFormDraft
import com.example.zeromile.data.model.VoiceLanguage

@Composable
fun ComplaintFormView(
    formDraft: ComplaintFormDraft,
    recommendation: CivicAiRecommendation,
    onBackToAnalysis: () -> Unit,
    onDraftUpdated: (ComplaintFormDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    var landmark by remember { mutableStateOf(formDraft.locationLandmark) }
    var description by remember { mutableStateOf(formDraft.citizenTranscript) }
    var phone by remember { mutableStateOf(formDraft.citizenPhone) }
    var incidentTime by remember { mutableStateOf(formDraft.incidentTime) }
    var showSubmissionNotice by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("complaint_form_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZeromileButton(
                text = "Back to AI Analysis",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBackToAnalysis,
                variant = ButtonVariant.TEXT,
                testTag = "back_to_analysis_button"
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Phase 3 Intake Form",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Service & Department Summary Card
        ZeromileCard(testTag = "form_summary_card") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formDraft.categoryName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = formDraft.serviceName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF97316).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${recommendation.priority} Priority",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFF97316),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formDraft.departmentName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Form Fields Card
        ZeromileCard(testTag = "form_fields_card") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Complaint Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Pre-filled Citizen Complaint Description (Voice or Text transcript)
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        onDraftUpdated(formDraft.copy(citizenTranscript = it))
                    },
                    label = { Text("Problem Description (from voice/text)") },
                    supportingText = {
                        Text("Transcribed from citizen's words (${formDraft.language.displayName})")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_description_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5
                )

                // Nagpur Location / Landmark
                OutlinedTextField(
                    value = landmark,
                    onValueChange = {
                        landmark = it
                        onDraftUpdated(formDraft.copy(locationLandmark = it))
                    },
                    label = { Text("Location / Landmark in Nagpur") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    supportingText = { Text("Pre-filled: Nagpur • Ward 32, Dharampeth") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_landmark_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Citizen Contact Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        onDraftUpdated(formDraft.copy(citizenPhone = it))
                    },
                    label = { Text("Citizen Contact Number") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Incident Time
                OutlinedTextField(
                    value = incidentTime,
                    onValueChange = {
                        incidentTime = it
                        onDraftUpdated(formDraft.copy(incidentTime = it))
                    },
                    label = { Text("Time or Period of Occurrence") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_time_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Submission Notice / Phase 3 Completion Banner
        if (showSubmissionNotice) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phase_3_ready_notice")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Phase 3 Intake Complete",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "AI classification verified and mapped to ${formDraft.departmentName}. Ready for municipal submission in Phase 4.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        // Ready Button
        ZeromileButton(
            text = "Validate & Package Complaint",
            icon = Icons.Default.CheckCircle,
            onClick = {
                showSubmissionNotice = true
            },
            variant = ButtonVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth(),
            testTag = "validate_package_button"
        )
    }
}
