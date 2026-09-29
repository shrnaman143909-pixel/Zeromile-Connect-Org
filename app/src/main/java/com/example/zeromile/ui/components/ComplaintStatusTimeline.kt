package com.example.zeromile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.ComplaintStatus
import com.example.zeromile.data.model.ComplaintStatusHistoryRecord
import com.example.ui.theme.CivicAmberAccent
import com.example.ui.theme.CivicSuccess

/**
 * State of a timeline step relative to actual database history.
 */
enum class StepState {
    COMPLETED, // Event actually occurred in history
    ACTIVE,    // Current active status
    PENDING    // Future step not yet reached
}

private data class TimelineStepDefinition(
    val status: ComplaintStatus,
    val title: String,
    val pendingDescription: String
)

/**
 * Visual timeline reflecting actual database information.
 * Enforces strict order: Submitted -> Assigned -> In Progress -> Resolved (or Closed).
 * Never claims a future status occurred.
 */
@Composable
fun ComplaintStatusTimeline(
    currentStatusText: String,
    history: List<ComplaintStatusHistoryRecord>,
    modifier: Modifier = Modifier,
    testTag: String = "complaint_status_timeline"
) {
    val currentStatus = ComplaintStatus.fromString(currentStatusText)

    // Standard municipal workflow sequence
    val steps = listOf(
        TimelineStepDefinition(
            status = ComplaintStatus.SUBMITTED,
            title = "Submitted",
            pendingDescription = "Waiting for intake"
        ),
        TimelineStepDefinition(
            status = ComplaintStatus.ASSIGNED,
            title = "Assigned",
            pendingDescription = "Waiting for municipal assignment"
        ),
        TimelineStepDefinition(
            status = ComplaintStatus.IN_PROGRESS,
            title = "In Progress",
            pendingDescription = "Not started yet"
        ),
        TimelineStepDefinition(
            status = ComplaintStatus.RESOLVED,
            title = "Resolved",
            pendingDescription = "Pending"
        )
    )

    // If complaint is closed, optionally present closed or append it
    val isClosed = currentStatus == ComplaintStatus.CLOSED
    val effectiveSteps = if (isClosed) {
        steps + TimelineStepDefinition(
            status = ComplaintStatus.CLOSED,
            title = "Closed",
            pendingDescription = "Pending closure"
        )
    } else {
        steps
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        effectiveSteps.forEachIndexed { index, step ->
            val matchingHistory = history.find { it.status.equals(step.status.rawValue, ignoreCase = true) }
            val hasOccurred = matchingHistory != null

            // Determine state
            val state = when {
                step.status == currentStatus && currentStatus != ComplaintStatus.RESOLVED && currentStatus != ComplaintStatus.CLOSED -> StepState.ACTIVE
                hasOccurred -> StepState.COMPLETED
                else -> StepState.PENDING
            }

            val isLast = index == effectiveSteps.lastIndex

            TimelineStepRow(
                step = step,
                state = state,
                historyRecord = matchingHistory,
                isLast = isLast,
                stepIndex = index
            )
        }
    }
}

@Composable
private fun TimelineStepRow(
    step: TimelineStepDefinition,
    state: StepState,
    historyRecord: ComplaintStatusHistoryRecord?,
    isLast: Boolean,
    stepIndex: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_step_${step.status.rawValue.lowercase().replace(" ", "_")}")
    ) {
        // Left Column: Node icon + connecting line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Node circle
            TimelineNodeIcon(state = state)

            // Connecting line to next step
            if (!isLast) {
                val lineColor = if (state == StepState.COMPLETED) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp)
                        .background(lineColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Column: Step details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 0.dp else 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (state == StepState.PENDING) FontWeight.Normal else FontWeight.Bold,
                        color = when (state) {
                            StepState.COMPLETED -> MaterialTheme.colorScheme.onSurface
                            StepState.ACTIVE -> CivicAmberAccent
                            StepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                )

                if (state == StepState.ACTIVE) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CivicAmberAccent.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Current State",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CivicAmberAccent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            when (state) {
                StepState.COMPLETED, StepState.ACTIVE -> {
                    if (historyRecord != null) {
                        // Display assigned team if present on this step
                        if (!historyRecord.assignedTeam.isNullOrBlank()) {
                            Text(
                                text = historyRecord.assignedTeam,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Display status change note/details if present
                        if (!historyRecord.note.isNullOrBlank()) {
                            Text(
                                text = historyRecord.note,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        // Display formatted timestamp
                        Text(
                            text = formatTimestamp(historyRecord.createdAt),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                StepState.PENDING -> {
                    Text(
                        text = step.pendingDescription,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineNodeIcon(state: StepState) {
    val nodeColor by animateColorAsState(
        targetValue = when (state) {
            StepState.COMPLETED -> CivicSuccess
            StepState.ACTIVE -> CivicAmberAccent
            StepState.PENDING -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        },
        label = "node_color"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                when (state) {
                    StepState.COMPLETED -> nodeColor
                    StepState.ACTIVE -> nodeColor.copy(alpha = 0.16f)
                    StepState.PENDING -> Color.Transparent
                }
            )
            .border(
                width = 2.dp,
                color = nodeColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            StepState.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            StepState.ACTIVE -> {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = "In progress",
                    tint = CivicAmberAccent,
                    modifier = Modifier.size(13.dp)
                )
            }
            StepState.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        }
    }
}

/**
 * Format ISO-8601 timestamp string into human readable citizen format:
 * e.g., "19 Sep 2026, 3:42 PM"
 */
private fun formatTimestamp(isoString: String): String {
    if (isoString.isBlank()) return ""
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val cleanIso = isoString.replace("Z", "").substringBefore(".")
        val date = parser.parse(cleanIso) ?: return isoString
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy, h:mm a", java.util.Locale.US)
        formatter.format(date)
    } catch (e: Exception) {
        // Fallback or truncated string
        if (isoString.length >= 16) isoString.substring(0, 10) + " " + isoString.substring(11, 16) else isoString
    }
}
