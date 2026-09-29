package com.example.zeromile.ui.components.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.ComplaintReviewData
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard

@Composable
fun ReadyForSubmissionCard(
    reviewData: ComplaintReviewData,
    isSubmitting: Boolean = false,
    submissionError: String? = null,
    onSubmitComplaint: () -> Unit,
    onReviewAgain: () -> Unit,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ready_for_submission_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ZeromileCard(testTag = "submission_ready_hero_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Success Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Ready for Submission",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Ready to Submit Grievance",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "All required fields for ${reviewData.serviceName} have been dynamically populated and verified for filing.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Draft Packet Token Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PRE-DISPATCH ID:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = reviewData.referenceId,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        // Submission error banner if any
        if (!submissionError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("submission_error_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Submission Issue",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Text(
                            text = submissionError,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                        )
                    }
                    IconButton(onClick = onDismissError) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Municipal Dispatch Routing Confirmation
        ZeromileCard(testTag = "municipal_routing_packet_card") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Validated Municipal Dispatch Packet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                listOf(
                    "Service Identified" to reviewData.serviceName,
                    "Assigned Department" to reviewData.departmentName,
                    "Priority Designation" to "${reviewData.priority.label} (${reviewData.priority.name})",
                    "Jurisdiction Ward" to reviewData.ward,
                    "Citizen Applicant" to "${reviewData.citizenName} (${reviewData.citizenPhone})",
                    "Dynamic Attributes Count" to "${reviewData.fieldResponses.size} fields filled",
                    "Evidence Attachment" to if (reviewData.hasEvidenceAttached) "Attached (${reviewData.evidenceType ?: "Media"})" else "None required"
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Live Supabase Integration Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Official Municipal Ledger",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Submitting registers your record directly into the Supabase complaint database with an authentic NMC ID.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        // Action Buttons: Submit Complaint & Review Details
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ZeromileButton(
                text = if (isSubmitting) "Lodging Complaint in Supabase..." else "Submit Complaint",
                icon = if (isSubmitting) null else Icons.Default.Send,
                onClick = onSubmitComplaint,
                enabled = !isSubmitting,
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_complaint_final_button")
            )

            ZeromileButton(
                text = "Back to Review",
                icon = Icons.Default.Visibility,
                onClick = onReviewAgain,
                enabled = !isSubmitting,
                variant = ButtonVariant.OUTLINE,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_review_again_button")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
