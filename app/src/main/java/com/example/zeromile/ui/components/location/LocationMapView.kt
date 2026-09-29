package com.example.zeromile.ui.components.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import java.util.Locale

/**
 * Interactive Nagpur Civic Location and Map Display Component.
 * Visualizes coordinates, accuracy confidence, ward classification, and offers external navigation intent.
 */
@Composable
fun ComplaintLocationCard(
    locationText: String,
    latitude: Double? = null,
    longitude: Double? = null,
    accuracyMeters: Double? = null,
    source: String? = "manual",
    wardName: String? = null,
    wardNumber: String? = null,
    modifier: Modifier = Modifier,
    showExternalMapButton: Boolean = true,
    testTag: String = "complaint_location_card"
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val effectiveLat = latitude ?: 21.1436
    val effectiveLng = longitude ?: 79.0688
    val formattedCoords = String.format(Locale.US, "%.5f° N, %.5f° E", effectiveLat, effectiveLng)

    ZeromileCard(
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Location Title + Source Pill
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
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location pin",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Grievance Location",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (wardNumber != null) "Nagpur Ward $wardNumber" else "Nagpur Municipal Area",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Source badge
                val sourceLabel = when (source?.lowercase()) {
                    "gps" -> if (accuracyMeters != null) "GPS ±${accuracyMeters.toInt()}m" else "GPS Captured"
                    "address" -> "Address Match"
                    "ai_detected" -> "AI Identified"
                    else -> "Manual Entry"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (source == "gps") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (source == "gps") Icons.Default.GpsFixed else Icons.Outlined.Explore,
                            contentDescription = null,
                            tint = if (source == "gps") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (source == "gps") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Primary Location Description
            Text(
                text = locationText.ifBlank { "Dharampeth, Nagpur" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Stylized Nagpur Municipal Map Canvas Preview
            NagpurMapCanvasPreview(
                lat = effectiveLat,
                lng = effectiveLng,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("location_map_canvas")
            )

            // Coordinates Badge and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coordinates text with copy affordance
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString("$effectiveLat, $effectiveLng"))
                            Toast.makeText(context, "Coordinates copied: $effectiveLat, $effectiveLng", Toast.LENGTH_SHORT).show()
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy coordinates",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formattedCoords,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (showExternalMapButton) {
                    ZeromileButton(
                        text = "Open in Maps",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            val geoUri = Uri.parse("geo:$effectiveLat,$effectiveLng?q=$effectiveLat,$effectiveLng(Grievance+Location)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webMapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$effectiveLat,$effectiveLng")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webMapUri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        },
                        variant = ButtonVariant.OUTLINE,
                        modifier = Modifier.height(34.dp),
                        testTag = "open_in_maps_button"
                    )
                }
            }
        }
    }
}

/**
 * Native Compose Vector Map Canvas displaying Nagpur arterial road network,
 * zone grid, and pulsating marker at the exact complaint coordinates.
 */
@Composable
fun NagpurMapCanvasPreview(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val roadColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Box(modifier = modifier.background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background terrain grid
            val gridStep = 24.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            // Nagpur arterial roads (Stylized West High Court Rd, Amravati Rd, Wardha Rd)
            val roadStroke = 3.dp.toPx()
            val highwayStroke = 5.dp.toPx()

            // Main East-West Corridor (Amravati Rd)
            drawLine(
                color = roadColor,
                start = Offset(0f, height * 0.45f),
                end = Offset(width, height * 0.4f),
                strokeWidth = highwayStroke
            )

            // North-South Arterial (WHC Road)
            drawLine(
                color = roadColor,
                start = Offset(width * 0.45f, 0f),
                end = Offset(width * 0.55f, height),
                strokeWidth = highwayStroke
            )

            // Diagonal Ring Road
            drawLine(
                color = roadColor.copy(alpha = 0.7f),
                start = Offset(width * 0.1f, height * 0.85f),
                end = Offset(width * 0.9f, height * 0.15f),
                strokeWidth = roadStroke
            )

            // Radial junction circle
            drawCircle(
                color = roadColor,
                radius = 12.dp.toPx(),
                center = Offset(width * 0.5f, height * 0.5f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Center Pin Marker with Animated GPS Pulse
            val center = Offset(width * 0.5f, height * 0.5f)

            // Pulsing GPS ripple circle
            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = center
            )

            // Outer pin halo
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = center
            )

            // Inner pin core
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx(),
                center = center
            )
        }

        // Map overlay labels
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Nagpur Civic GeoGrid",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Interactive card used in the complaint intake form allowing citizens to capture their live GPS
 * or review their pre-selected location with Ward classification.
 */
@Composable
fun ComplaintLocationCaptureCard(
    locationData: com.example.zeromile.data.model.ComplaintLocationData?,
    isCapturing: Boolean,
    errorMessage: String?,
    onCaptureLocation: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "location_capture_card"
) {
    val context = LocalContext.current
    val effectiveLoc = locationData ?: com.example.zeromile.data.model.ComplaintLocationData(
        latitude = 21.1436,
        longitude = 79.0688,
        accuracyMeters = 15.0,
        source = "address",
        locationText = "Dharampeth, Nagpur",
        wardName = "Dharampeth Ward",
        wardNumber = "32"
    )

    ZeromileCard(
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Pin",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Nagpur Ward & Location",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val wardTitle: String = effectiveLoc.wardName?.let { wName ->
                            "$wName (Ward ${effectiveLoc.wardNumber ?: "32"})"
                        } ?: "Ward 32, Dharampeth Zone"
                        Text(
                            text = wardTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // GPS status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (locationData?.source == "gps") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (locationData?.source == "gps") Icons.Default.GpsFixed else Icons.Outlined.Explore,
                            contentDescription = null,
                            tint = if (locationData?.source == "gps") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (locationData?.source == "gps") {
                                if (locationData.accuracyMeters != null) "GPS ±${locationData.accuracyMeters.toInt()}m" else "GPS Locked"
                            } else "Nagpur Default",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (locationData?.source == "gps") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Location text label
            Text(
                text = effectiveLoc.locationText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Map preview canvas
            NagpurMapCanvasPreview(
                lat = effectiveLoc.latitude ?: 21.1436,
                lng = effectiveLoc.longitude ?: 79.0688,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            // Action: Detect GPS Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "%.4f° N, %.4f° E", effectiveLoc.latitude, effectiveLoc.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ZeromileButton(
                    text = if (isCapturing) "Detecting GPS..." else "Use Current GPS",
                    icon = Icons.Default.GpsFixed,
                    onClick = onCaptureLocation,
                    enabled = !isCapturing,
                    variant = ButtonVariant.OUTLINE,
                    modifier = Modifier.height(36.dp),
                    testTag = "capture_gps_button"
                )
            }

            // Error notice if any
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

