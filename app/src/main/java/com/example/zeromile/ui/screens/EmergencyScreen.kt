package com.example.zeromile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.EmergencyService
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.ZeromilePageHeader
import com.example.zeromile.ui.components.ZeromileSkeletonCard
import com.example.zeromile.ui.viewmodel.CivicUiState
import com.example.zeromile.ui.viewmodel.UiState

@Composable
fun EmergencyScreen(
    uiState: CivicUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("emergency_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("emergency_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            ZeromilePageHeader(
                title = "Emergency Services",
                subtitle = "24x7 Verified Nagrik & National Helplines",
                testTag = "emergency_page_header"
            )
        }

        // Advisory Banner: For immediate life threat, call 112
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFEBEE),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("emergency_alert_banner")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Critical Emergency? Dial 112",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                    )
                    Text(
                        text = "Police, Fire, and Ambulance response across Nagpur & Maharashtra.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC62828))
                    )
                }
                ZeromileButton(
                    text = "112",
                    icon = Icons.Default.Call,
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                        context.startActivity(dialIntent)
                    },
                    variant = ButtonVariant.PRIMARY,
                    testTag = "quick_dial_112_button"
                )
            }
        }

        // Quick Category Filter Chips
        val categories = listOf("All", "National", "Civic Disaster", "Police", "Medical", "Women & Child")
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 6.dp)
                .testTag("emergency_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedFilter == cat,
                    onClick = { selectedFilter = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_${cat.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Emergency Services Content
        when (val state = uiState.emergencyServicesState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("emergency_loading_container"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        ZeromileSkeletonCard(height = 90)
                    }
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("emergency_error_container"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Failed to load emergency contacts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ZeromileButton(
                        text = "Try Again",
                        icon = Icons.Default.Refresh,
                        onClick = onRetry,
                        variant = ButtonVariant.PRIMARY,
                        testTag = "emergency_retry_button"
                    )
                }
            }
            is UiState.Success -> {
                val filteredList = state.data.filter { s ->
                    when (selectedFilter) {
                        "National" -> s.phoneNumber == "112" || s.name.contains("National", ignoreCase = true)
                        "Civic Disaster" -> s.name.contains("Disaster", ignoreCase = true) || s.name.contains("NMC", ignoreCase = true) || s.name.contains("DDMA", ignoreCase = true)
                        "Police" -> s.name.contains("Police", ignoreCase = true) || s.phoneNumber == "100"
                        "Medical" -> s.name.contains("Ambulance", ignoreCase = true) || s.name.contains("Medical", ignoreCase = true) || s.phoneNumber == "108"
                        "Women & Child" -> s.name.contains("Women", ignoreCase = true) || s.name.contains("Child", ignoreCase = true)
                        else -> true
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No emergency contacts in this category.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("emergency_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList, key = { it.id }) { service ->
                            EmergencyServiceCard(
                                service = service,
                                onDial = { number ->
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.replace("-", "").replace(" ", "")}"))
                                    context.startActivity(dialIntent)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyServiceCard(
    service: EmergencyService,
    onDial: (String) -> Unit
) {
    ZeromileCard(
        testTag = "emergency_card_${service.id}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(getEmergencyIconBg(service.icon)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getEmergencyIcon(service.icon),
                    contentDescription = null,
                    tint = getEmergencyIconTint(service.icon),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                if (!service.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = service.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary dial pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        onClick = { onDial(service.phoneNumber) },
                        modifier = Modifier.testTag("dial_primary_${service.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = service.phoneNumber,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // Alternate phone pill if present
                    if (!service.alternatePhone.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            onClick = { onDial(service.alternatePhone) },
                            modifier = Modifier.testTag("dial_alt_${service.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Alt Phone",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = service.alternatePhone,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getEmergencyIcon(iconName: String?): ImageVector {
    return when (iconName?.lowercase()) {
        "shield" -> Icons.Default.Shield
        "emergency" -> Icons.Default.Emergency
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "medical_services" -> Icons.Default.MedicalServices
        "local_police" -> Icons.Default.LocalPolice
        "female" -> Icons.Default.Female
        "child_care" -> Icons.Default.ChildCare
        "warning" -> Icons.Default.Warning
        else -> Icons.Default.Emergency
    }
}

private fun getEmergencyIconBg(iconName: String?): Color {
    return when (iconName?.lowercase()) {
        "shield" -> Color(0xFFE8F5E9)
        "local_fire_department" -> Color(0xFFFFEBEE)
        "medical_services" -> Color(0xFFE3F2FD)
        "local_police" -> Color(0xFFEDE7F6)
        "female" -> Color(0xFFFCE4EC)
        "child_care" -> Color(0xFFFFF3E0)
        "warning" -> Color(0xFFFFF8E1)
        else -> Color(0xFFFFEBEE)
    }
}

private fun getEmergencyIconTint(iconName: String?): Color {
    return when (iconName?.lowercase()) {
        "shield" -> Color(0xFF2E7D32)
        "local_fire_department" -> Color(0xFFC62828)
        "medical_services" -> Color(0xFF1565C0)
        "local_police" -> Color(0xFF4527A0)
        "female" -> Color(0xFFAD1457)
        "child_care" -> Color(0xFFE65100)
        "warning" -> Color(0xFFF57F17)
        else -> Color(0xFFD32F2F)
    }
}
