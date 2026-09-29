package com.example.zeromile.ui.components.evidence

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zeromile.data.evidence.EvidenceManager
import com.example.zeromile.data.model.ComplaintEvidenceRecord
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Gallery component for displaying attached evidence photos in Complaint Details.
 * Supports secure signed URL loading, lightbox full-screen zoom dialog,
 * post-submission upload by citizen, and administrative deletion.
 */
@Composable
fun EvidenceGalleryCard(
    evidence: List<ComplaintEvidenceRecord>,
    complaintId: String,
    currentUserId: String?,
    isAdmin: Boolean = false,
    onUploadAdditionalPhoto: ((fileName: String, mimeType: String, bytes: ByteArray) -> Unit)? = null,
    onDeletePhoto: ((evidenceId: String, storagePath: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = "evidence_gallery_card"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPhoto by remember { mutableStateOf<ComplaintEvidenceRecord?>(null) }
    var photoToDelete by remember { mutableStateOf<ComplaintEvidenceRecord?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // Additional photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && onUploadAdditionalPhoto != null) {
            coroutineScope.launch {
                isUploading = true
                val result = EvidenceManager.stageEvidenceFromUri(context, uri)
                result.onSuccess { item ->
                    item.bytes?.let { bytes ->
                        onUploadAdditionalPhoto(item.fileName, item.mimeType, bytes)
                        Toast.makeText(context, "Evidence uploaded successfully", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Failed to read image", Toast.LENGTH_LONG).show()
                }
                isUploading = false
            }
        }
    }

    ZeromileCard(
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Photo Evidence",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Photo Evidence",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (evidence.isNotEmpty()) "${evidence.size} verified attachment${if (evidence.size > 1) "s" else ""}"
                            else "No photo evidence attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Encrypted Bucket",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Photos Row / Grid
            if (evidence.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth().testTag("evidence_thumbnails_row")
                ) {
                    items(evidence, key = { it.id }) { record ->
                        EvidenceThumbnailCard(
                            record = record,
                            onClick = { selectedPhoto = record }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No photos were attached during grievance submission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Uploading state
            if (isUploading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Uploading to Nagpur Municipal Storage...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Action: Add additional photo if citizen or admin
            if (onUploadAdditionalPhoto != null && evidence.size < 5) {
                ZeromileButton(
                    text = "Add Additional Evidence Photo",
                    icon = Icons.Default.AddPhotoAlternate,
                    onClick = {
                        photoPickerLauncher.launch("image/*")
                    },
                    variant = ButtonVariant.OUTLINE,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    testTag = "add_more_evidence_button"
                )
            }
        }
    }

    // Lightbox / Fullscreen Image Dialog
    selectedPhoto?.let { photo ->
        Dialog(onDismissRequest = { selectedPhoto = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("evidence_lightbox_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = photo.fileName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            val sizeKb = photo.fileSize / 1024
                            val sizeText = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB"
                            Text(
                                text = "$sizeText • ${photo.mimeType} • ${photo.createdAt?.take(10) ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { selectedPhoto = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // High-res Image Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photo.signedUrl ?: photo.storagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = photo.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom Action Bar: Open in browser + Delete (if admin or owner)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val viewUrl = photo.signedUrl
                        if (!viewUrl.isNullOrBlank() && viewUrl.startsWith("http")) {
                            ZeromileButton(
                                text = "Open Full Size",
                                icon = Icons.Default.OpenInNew,
                                onClick = {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(viewUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(browserIntent)
                                },
                                variant = ButtonVariant.OUTLINE,
                                modifier = Modifier.weight(1f).height(38.dp)
                            )
                        }

                        if ((isAdmin || photo.uploadedBy == currentUserId) && onDeletePhoto != null) {
                            ZeromileButton(
                                text = "Delete",
                                icon = Icons.Default.Delete,
                                onClick = {
                                    photoToDelete = photo
                                    selectedPhoto = null
                                },
                                variant = ButtonVariant.DESTRUCTIVE,
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete Evidence Photo?") },
            text = { Text("Are you sure you want to permanently delete '${photo.fileName}' from municipal storage? This action cannot be undone.") },
            confirmButton = {
                ZeromileButton(
                    text = "Confirm Delete",
                    variant = ButtonVariant.DESTRUCTIVE,
                    onClick = {
                        onDeletePhoto?.invoke(photo.id, photo.storagePath)
                        photoToDelete = null
                        Toast.makeText(context, "Evidence deleted", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            dismissButton = {
                ZeromileButton(
                    text = "Cancel",
                    variant = ButtonVariant.OUTLINE,
                    onClick = { photoToDelete = null }
                )
            }
        )
    }
}

/**
 * Single evidence thumbnail card with metadata badge.
 */
@Composable
fun EvidenceThumbnailCard(
    record: ComplaintEvidenceRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sizeKb = record.fileSize / 1024
    val sizeLabel = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB"

    Box(
        modifier = modifier
            .size(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(record.signedUrl ?: record.storagePath)
                .crossfade(true)
                .build(),
            contentDescription = record.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom label
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(vertical = 3.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}
