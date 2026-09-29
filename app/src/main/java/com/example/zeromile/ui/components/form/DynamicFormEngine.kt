package com.example.zeromile.ui.components.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.zeromile.data.model.CivicAiRecommendation
import com.example.zeromile.data.model.ComplaintLocationData
import com.example.zeromile.data.model.FormFieldConfig
import com.example.zeromile.data.model.FormFieldType
import com.example.zeromile.data.model.FormStep
import com.example.zeromile.data.model.Profile
import com.example.zeromile.data.model.ServiceFormConfig
import com.example.zeromile.data.model.StagedEvidenceItem
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.evidence.EvidenceAttachmentField
import com.example.zeromile.ui.components.location.ComplaintLocationCaptureCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicFormEngine(
    formConfig: ServiceFormConfig,
    recommendation: CivicAiRecommendation,
    fieldValues: Map<String, String>,
    formErrors: Map<String, String>,
    profile: Profile?,
    onFieldValueChange: (String, String) -> Unit,
    onBackToAnalysis: () -> Unit,
    onReviewComplaint: () -> Unit,
    stagedEvidence: List<StagedEvidenceItem> = emptyList(),
    onAddStagedEvidence: (StagedEvidenceItem) -> Unit = {},
    onRemoveStagedEvidence: (String) -> Unit = {},
    capturedLocation: ComplaintLocationData? = null,
    isCapturingLocation: Boolean = false,
    locationError: String? = null,
    onCaptureLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_form_engine"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation & Step Indicator
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
                testTag = "form_back_to_analysis_button"
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Step 1 of 2: Fill Details",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Service & Department Header Card
        ZeromileCard(testTag = "form_service_header_card") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${formConfig.categoryName} • Dynamic Form",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formConfig.serviceName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }

                    // Priority Pill
                    val priColor = when (recommendation.priority.uppercase()) {
                        "EMERGENCY" -> Color(0xFFEF4444)
                        "URGENT", "HIGH" -> Color(0xFFF97316)
                        else -> Color(0xFF3B82F6)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, priColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${recommendation.priority} Priority",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = priColor,
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
                            text = formConfig.departmentName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Text(
                    text = formConfig.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        // Original Citizen Statement Card (Pre-filled Reference)
        ZeromileCard(testTag = "citizen_voice_reference_card") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (recommendation.inputMode == "voice") Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Original Citizen Statement (${recommendation.languageCode})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Auto-Transcribed",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${recommendation.originalTranscript ?: ""}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                recommendation.summary?.let { sum ->
                    Text(
                        text = "AI Extracted Summary: $sum",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Dynamic Service Fields Card
        ZeromileCard(testTag = "service_dynamic_fields_card") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Required Service Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "This form is dynamically customized for ${formConfig.serviceName}. Fill required fields before proceeding to review.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                // Render each field dynamically
                formConfig.fields.forEach { field ->
                    DynamicFieldItem(
                        field = field,
                        currentValue = fieldValues[field.id] ?: field.defaultValue,
                        errorMessage = formErrors[field.id],
                        onValueChange = { onFieldValueChange(field.id, it) }
                    )
                }
            }
        }

        // Phase 9: Live GPS Location & Nagpur Ward Capture Card
        ComplaintLocationCaptureCard(
            locationData = capturedLocation,
            isCapturing = isCapturingLocation,
            errorMessage = locationError,
            onCaptureLocation = onCaptureLocation,
            modifier = Modifier.fillMaxWidth()
        )

        // Phase 9: Real Photo Evidence Attachment Field
        EvidenceAttachmentField(
            stagedItems = stagedEvidence,
            onAddStagedItem = onAddStagedEvidence,
            onRemoveStagedItem = onRemoveStagedEvidence,
            modifier = Modifier.fillMaxWidth()
        )

        // Citizen Verification & Contact Card
        ZeromileCard(testTag = "citizen_contact_info_card") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Citizen Verification & Jurisdiction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = profile?.fullName ?: "Rajesh Sharma",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Nagpur • Ward 32, Dharampeth",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = profile?.phone ?: "+91 98230 12345",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Review Complaint Action Button
        ZeromileButton(
            text = "Review Complaint",
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onReviewComplaint,
            variant = ButtonVariant.PRIMARY,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("review_complaint_button")
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicFieldItem(
    field: FormFieldConfig,
    currentValue: String,
    errorMessage: String?,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_container_${field.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label with required marker
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            if (field.isRequired) {
                Text(
                    text = " *",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        field.helpText?.let { help ->
            Text(
                text = help,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            )
        }

        when (field.type) {
            FormFieldType.LOCATION_LANDMARK -> {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    placeholder = { Text(field.placeholder.ifBlank { "e.g. Near Dharampeth, Nagpur" }) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Pre-filled from citizen's mention in Nagpur")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${field.id}"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Quick Local Area Suggestion Chips for Nagpur
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Dharampeth", "Sitabuldi", "Ramdaspeth", "Civil Lines", "Sadar", "VIP Road").forEach { loc ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { onValueChange("$loc, Nagpur") }
                                .testTag("chip_loc_$loc")
                        ) {
                            Text(
                                text = "+ $loc",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            FormFieldType.CHIPS -> {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    field.options.forEach { option ->
                        val isSelected = currentValue.equals(option, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onValueChange(option) },
                            label = {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .testTag("chip_${field.id}_$option")
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            FormFieldType.TEXT -> {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    placeholder = { Text(field.placeholder) },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${field.id}"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            FormFieldType.TEXT_AREA -> {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    placeholder = { Text(field.placeholder) },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${field.id}"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5
                )
            }

            FormFieldType.CHECKBOX -> {
                val isChecked = currentValue.toBooleanStrictOrNull() ?: false
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onValueChange((!isChecked).toString()) }
                        .testTag("checkbox_container_${field.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onValueChange(it.toString()) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = field.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            FormFieldType.EVIDENCE_ATTACHMENT -> {
                val isAttached = currentValue.isNotBlank() && currentValue != "false" && currentValue != "none"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAttached) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        if (isAttached) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("evidence_container_${field.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isAttached) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = if (isAttached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAttached) "Evidence Attached" else "No Evidence Attached",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            if (isAttached) {
                                IconButton(
                                    onClick = { onValueChange("none") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove evidence",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (isAttached) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentValue,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ZeromileButton(
                                    text = "Attach Photo",
                                    icon = Icons.Default.AddPhotoAlternate,
                                    onClick = { onValueChange("photo_evidence_nagpur.jpg (1.8 MB)") },
                                    variant = ButtonVariant.OUTLINE,
                                    modifier = Modifier.weight(1f),
                                    testTag = "attach_photo_button"
                                )
                                ZeromileButton(
                                    text = "Attach Audio",
                                    icon = Icons.Default.Audiotrack,
                                    onClick = { onValueChange("voice_note_evidence.aac (480 KB)") },
                                    variant = ButtonVariant.OUTLINE,
                                    modifier = Modifier.weight(1f),
                                    testTag = "attach_audio_button"
                                )
                            }
                        }
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            else -> {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    placeholder = { Text(field.placeholder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${field.id}"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
