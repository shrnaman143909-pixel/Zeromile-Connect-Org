package com.example.zeromile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.zeromile.data.model.AdminMetrics
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.data.model.PagedResult
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.viewmodel.AdminSection
import com.example.zeromile.ui.viewmodel.AdminUiState
import com.example.zeromile.ui.viewmodel.AdminViewModel
import com.example.zeromile.ui.viewmodel.UiState

/**
 * Phase 7 Admin Dashboard Home
 * Real-time operational metrics, category/ward/priority breakdowns, and recent complaints.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardView(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp
        val horizontalPadding = if (isWide) 24.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPadding, vertical = 20.dp)
                .testTag("admin_dashboard_view"),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section Title & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Operational Overview",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Nagpur Municipal Corporation • Live Complaints Telemetry",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }

                IconButton(
                    onClick = {
                        adminViewModel.loadDashboardData()
                        adminViewModel.loadComplaints()
                    },
                    modifier = Modifier.testTag("admin_refresh_metrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Telemetry",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }

            // Metrics Grid (Calculated from real data, 0 if empty)
            when (val state = adminUiState.metricsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                }
                is UiState.Error -> {
                    Surface(
                        color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Telemetry Error: ${state.message}",
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val metrics = state.data
                    MetricsCardsGrid(metrics = metrics)

                    // Breakdowns by Ward & Priority & Category
                    BreakdownSection(metrics = metrics)
                }
            }

            // Recent Complaints Panel
            RecentComplaintsPanel(
                complaintsState = adminUiState.complaintsState,
                onSelectComplaint = { id -> adminViewModel.openComplaintDetail(id) },
                onViewAllComplaints = { adminViewModel.setSection(AdminSection.COMPLAINTS) }
            )
        }
    }
}

@Composable
private fun MetricsCardsGrid(metrics: AdminMetrics) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Total Complaints",
                        count = metrics.total,
                        icon = Icons.Default.Assignment,
                        color = Color(0xFF38BDF8),
                        testTag = "metric_card_total",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Submitted / Pending",
                        count = metrics.submitted,
                        icon = Icons.Default.PendingActions,
                        color = Color(0xFFF59E0B),
                        testTag = "metric_card_submitted",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Assigned",
                        count = metrics.assigned,
                        icon = Icons.Default.HourglassBottom,
                        color = Color(0xFF818CF8),
                        testTag = "metric_card_assigned",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "In Progress",
                        count = metrics.inProgress,
                        icon = Icons.Default.PendingActions,
                        color = Color(0xFF38BDF8),
                        testTag = "metric_card_in_progress",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Resolved",
                        count = metrics.resolved,
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF10B981),
                        testTag = "metric_card_resolved",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Closed",
                        count = metrics.closed,
                        icon = Icons.Default.DoneAll,
                        color = Color(0xFF94A3B8),
                        testTag = "metric_card_closed",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Mobile: 2 columns per row, perfectly balanced
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total",
                        count = metrics.total,
                        icon = Icons.Default.Assignment,
                        color = Color(0xFF38BDF8),
                        testTag = "metric_card_total",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pending",
                        count = metrics.submitted,
                        icon = Icons.Default.PendingActions,
                        color = Color(0xFFF59E0B),
                        testTag = "metric_card_submitted",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Assigned",
                        count = metrics.assigned,
                        icon = Icons.Default.HourglassBottom,
                        color = Color(0xFF818CF8),
                        testTag = "metric_card_assigned",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "In Progress",
                        count = metrics.inProgress,
                        icon = Icons.Default.PendingActions,
                        color = Color(0xFF38BDF8),
                        testTag = "metric_card_in_progress",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Resolved",
                        count = metrics.resolved,
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF10B981),
                        testTag = "metric_card_resolved",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Closed",
                        count = metrics.closed,
                        icon = Icons.Default.DoneAll,
                        color = Color(0xFF94A3B8),
                        testTag = "metric_card_closed",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag(testTag),
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun BreakdownSection(metrics: AdminMetrics) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 640.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CategoryBreakdownCard(metrics = metrics, modifier = Modifier.weight(1f))
                PriorityBreakdownCard(metrics = metrics, modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CategoryBreakdownCard(metrics = metrics, modifier = Modifier.fillMaxWidth())
                PriorityBreakdownCard(metrics = metrics, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(metrics: AdminMetrics, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("admin_category_breakdown_card"),
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Grievances by Category",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (metrics.byCategory.isEmpty()) {
                Text("No records", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
            } else {
                metrics.byCategory.entries.take(5).forEach { (cat, cnt) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                        Text(cnt.toString(), color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityBreakdownCard(metrics: AdminMetrics, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("admin_priority_breakdown_card"),
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Complaints by Priority",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (metrics.byPriority.isEmpty()) {
                Text("No records", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
            } else {
                metrics.byPriority.entries.forEach { (prio, cnt) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(prio, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                        Text(cnt.toString(), color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentComplaintsPanel(
    complaintsState: UiState<PagedResult<ComplaintRecord>>,
    onSelectComplaint: (String) -> Unit,
    onViewAllComplaints: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_recent_complaints_panel"),
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Grievances",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Live entries from citizens of Nagpur",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }

                Text(
                    text = "View All →",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { onViewAllComplaints() }
                        .padding(8.dp)
                        .testTag("view_all_complaints_link")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (complaintsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                }
                is UiState.Error -> {
                    Text("Failed to load recent complaints: ${complaintsState.message}", color = Color(0xFFFCA5A5))
                }
                is UiState.Success -> {
                    val list = complaintsState.data.items.take(5)
                    if (list.isEmpty()) {
                        Text("No complaints found in database.", color = Color(0xFF64748B), modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            list.forEach { complaint ->
                                RecentComplaintRow(
                                    complaint = complaint,
                                    onClick = { onSelectComplaint(complaint.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentComplaintRow(
    complaint: ComplaintRecord,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_complaint_row_${complaint.complaintNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = complaint.complaintNumber,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = complaint.service?.name ?: "Civic Issue",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${complaint.locationText} • ${complaint.description.take(65)}...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                val badgeStatus = when (complaint.status.lowercase()) {
                    "submitted" -> BadgeStatus.WARNING
                    "assigned", "in progress" -> BadgeStatus.INFO
                    "resolved" -> BadgeStatus.SUCCESS
                    else -> BadgeStatus.DEFAULT
                }
                ZeromileStatusBadge(text = complaint.status, status = badgeStatus)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = complaint.priority,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (complaint.priority.equals("High", true)) Color(0xFFF87171) else Color(0xFF94A3B8)
                    )
                )
            }
        }
    }
}
