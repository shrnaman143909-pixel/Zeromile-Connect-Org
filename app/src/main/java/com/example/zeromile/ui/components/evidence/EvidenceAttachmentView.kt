package com.example.zeromile.ui.components.evidence

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.zeromile.data.model.StagedEvidenceItem
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Interactive photo evidence attachment component for citizen complaint intake form.
 * Supports zero-permission Android PhotoPicker, camera capture, file validation,
 * thumbnail previewing, and max 5 files constraint.
 */
@Composable
fun EvidenceAttachmentField(
    stagedItems: List<StagedEvidenceItem>,
    onAddStagedItem: (StagedEvidenceItem) -> Unit,
    onRemoveStagedItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxPhotos: Int = 5,
    testTag: String = "evidence_attachment_field"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var previewItem by remember { mutableStateOf<StagedEvidenceItem?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Multi-photo picker using Android zero-permission PickVisualMedia
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxPhotos)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remainingSlots = maxPhotos - stagedItems.size
            val urisToProcess = uris.take(remainingSlots)
            coroutineScope.launch {
                isProcessing = true
                for (uri in urisToProcess) {
                    val result = EvidenceManager.stageEvidenceFromUri(context, uri)
                    result.onSuccess { item ->
                        onAddStagedItem(item)
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Invalid file", Toast.LENGTH_LONG).show()
                    }
                }
                isProcessing = false
            }
        }
    }

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            if (stagedItems.size >= maxPhotos) {
                Toast.makeText(context, "Maximum $maxPhotos photos allowed", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                isProcessing = true
                try {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                    val bytes = stream.toByteArray()
                    
                    // Cache temp file for URI representation
                    val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(tempFile).use { it.write(bytes) }
                    val uri = Uri.fromFile(tempFile)

                    val staged = StagedEvidenceItem(
                        id = UUID.randomUUID().toString(),
                        uriString = uri.toString(),
                        fileName = "photo_${System.currentTimeMillis().toString().takeLast(6)}.jpg",
                        mimeType = "image/jpeg",
                        fileSizeBytes = bytes.size.toLong(),
                        bytes = bytes
                    )
                    onAddStagedItem(staged)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to process captured image", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
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
            // Header Row: Title & Photo Counter
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Photo Evidence",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Attach Photo Evidence",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Optional • Helps municipal staff verify faster",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Counter badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (stagedItems.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${stagedItems.size} / $maxPhotos",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (stagedItems.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Staged photos preview horizontal row
            if (stagedItems.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth().testTag("staged_evidence_row")
                ) {
                    items(stagedItems, key = { it.id }) { item ->
                        StagedPhotoThumbnail(
                            item = item,
                            onClick = { previewItem = item },
                            onRemove = { onRemoveStagedItem(item.id) }
                        )
                    }
                }
            }

            // Processing indicator
            if (isProcessing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Validating image format & security bytes...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Buttons: Gallery Picker & Camera Capture
            val canAddMore = stagedItems.size < maxPhotos && !isProcessing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ZeromileButton(
                    text = "Pick from Gallery",
                    icon = Icons.Default.PhotoLibrary,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = canAddMore,
                    variant = ButtonVariant.OUTLINE,
                    modifier = Modifier.weight(1f).height(42.dp),
                    testTag = "pick_gallery_button"
                )

                ZeromileButton(
                    text = "Take Photo",
                    icon = Icons.Default.CameraAlt,
                    onClick = {
                        cameraLauncher.launch(null)
                    },
                    enabled = canAddMore,
                    variant = ButtonVariant.OUTLINE,
                    modifier = Modifier.weight(1f).height(42.dp),
                    testTag = "take_photo_button"
                )
            }

            // Privacy & security footnote
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Files stored in private Supabase bucket. Accessible only by you & authorized NMC officers.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }

    // Enlarged photo preview dialog
    previewItem?.let { item ->
        Dialog(onDismissRequest = { previewItem = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.fileName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "${item.fileSizeBytes / 1024} KB • ${item.mimeType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { previewItem = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close preview")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.uriString)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    ZeromileButton(
                        text = "Remove Photo",
                        icon = Icons.Default.Delete,
                        onClick = {
                            onRemoveStagedItem(item.id)
                            previewItem = null
                        },
                        variant = ButtonVariant.DESTRUCTIVE,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Thumbnail item for a staged image with file size and remove badge.
 */
@Composable
fun StagedPhotoThumbnail(
    item: StagedEvidenceItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sizeKb = item.fileSizeBytes / 1024

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uriString)
                .crossfade(true)
                .build(),
            contentDescription = item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // File size pill at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "${sizeKb} KB",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        // Remove icon button at top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove photo",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
