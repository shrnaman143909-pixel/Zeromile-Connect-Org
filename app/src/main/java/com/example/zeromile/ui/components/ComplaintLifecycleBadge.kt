package com.example.zeromile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.ComplaintStatus
import com.example.ui.theme.CivicAmberAccent
import com.example.ui.theme.CivicSuccess

/**
 * Visual styling definition for complaint lifecycle badges.
 * Combines distinct background, foreground, and a clear symbolic icon
 * so status identification does not rely solely on color.
 */
data class StatusBadgeVisuals(
    val backgroundColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val label: String
)

/**
 * Returns distinct visual styling for each of the 5 municipal complaint statuses.
 */
@Composable
fun getComplaintStatusVisuals(statusText: String): StatusBadgeVisuals {
    val status = ComplaintStatus.fromString(statusText)
    return when (status) {
        ComplaintStatus.SUBMITTED -> StatusBadgeVisuals(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Default.Send,
            label = "Submitted"
        )
        ComplaintStatus.ASSIGNED -> StatusBadgeVisuals(
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            icon = Icons.Default.PendingActions,
            label = "Assigned"
        )
        ComplaintStatus.IN_PROGRESS -> StatusBadgeVisuals(
            backgroundColor = CivicAmberAccent.copy(alpha = 0.16f),
            contentColor = CivicAmberAccent,
            icon = Icons.Default.HourglassTop,
            label = "In Progress"
        )
        ComplaintStatus.RESOLVED -> StatusBadgeVisuals(
            backgroundColor = CivicSuccess.copy(alpha = 0.16f),
            contentColor = CivicSuccess,
            icon = Icons.Default.CheckCircle,
            label = "Resolved"
        )
        ComplaintStatus.CLOSED -> StatusBadgeVisuals(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Default.Lock,
            label = "Closed"
        )
    }
}

/**
 * Reusable complaint status badge adhering to Phase 6 requirements:
 * - Supports Submitted, Assigned, In Progress, Resolved, Closed.
 * - Distinct styling maintaining the dark/teal civic aesthetic.
 * - Uses both iconic and textual indicators (does not rely on color alone).
 * - High readability with minimum 48dp touch-target compatibility when embedded.
 */
@Composable
fun ComplaintLifecycleBadge(
    status: String,
    modifier: Modifier = Modifier,
    testTag: String = "complaint_lifecycle_badge"
) {
    val visuals = getComplaintStatusVisuals(status)

    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .background(visuals.backgroundColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
        ) {
            Icon(
                imageVector = visuals.icon,
                contentDescription = null,
                tint = visuals.contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = visuals.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = visuals.contentColor,
                    letterSpacing = 0.3.sp
                )
            )
        }
    }
}
