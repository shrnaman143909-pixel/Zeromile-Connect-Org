package com.example.zeromile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.zeromile.data.model.AdminSortBy
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.data.model.DateRangeFilter
import com.example.zeromile.data.model.PagedResult
import com.example.zeromile.ui.components.BadgeStatus
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileStatusBadge
import com.example.zeromile.ui.viewmodel.AdminUiState
import com.example.zeromile.ui.viewmodel.AdminViewModel
import com.example.zeromile.ui.viewmodel.UiState

/**
 * Phase 7 Administrative Complaints Management Table
 * Filter, search, sort, and paginated table with real Supabase database records.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminComplaintsView(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val tableHorizontalScroll = rememberScrollState()
    val filter = adminUiState.complaintFilter

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 768.dp
        val horizontalPadding = if (isWide) 24.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPadding, vertical = 20.dp)
                .testTag("admin_complaints_view"),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        // Top Heading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Citizen Complaints Management",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Manage municipal grievances, assignments, and resolution workflows",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
            }
        }

        // Search & Filter Controls Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_complaints_filters_panel"),
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = filter.searchQuery,
                    onValueChange = { adminViewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by ID, keyword, location, ward, citizen...", color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                    },
                    trailingIcon = {
                        if (filter.searchQuery.isNotBlank()) {
                            IconButton(onClick = { adminViewModel.setSearchQuery("") }) {
                                Text("✕", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_complaints_search_input")
                )

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.width(64.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statuses = listOf("ALL", "Submitted", "Assigned", "In Progress", "Resolved", "Closed")
                        statuses.forEach { st ->
                            val isSelected = if (st == "ALL") filter.status.isNullOrBlank() || filter.status.equals("ALL", true)
                            else filter.status.equals(st, true)

                            FilterChip(
                                selected = isSelected,
                                onClick = { adminViewModel.setStatusFilter(if (st == "ALL") null else st) },
                                label = { Text(st, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFFCBD5E1)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF334155),
                                    selectedBorderColor = Color(0xFF38BDF8),
                                    enabled = true,
                                    selected = isSelected
                                ),
                                modifier = Modifier.testTag("admin_filter_status_${st.lowercase()}")
                            )
                        }
                    }
                }

                // Priority and Date Range Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Priority:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.width(64.dp)
                        )

                        val priorities = listOf("ALL", "Critical", "High", "Medium", "Low")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            priorities.forEach { prio ->
                                val isSelected = if (prio == "ALL") filter.priority.isNullOrBlank() || filter.priority.equals("ALL", true)
                                else filter.priority.equals(prio, true)

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { adminViewModel.setPriorityFilter(if (prio == "ALL") null else prio) },
                                    label = { Text(prio, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0284C7),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFCBD5E1)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color(0xFF334155),
                                        selectedBorderColor = Color(0xFF38BDF8),
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    modifier = Modifier.testTag("admin_filter_priority_${prio.lowercase()}")
                                )
                            }
                        }
                    }

                    // Sort By Chips
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sort:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        AdminSortBy.values().forEach { sortBy ->
                            val isSelected = filter.sortBy == sortBy
                            FilterChip(
                                selected = isSelected,
                                onClick = { adminViewModel.setSortBy(sortBy) },
                                label = { Text(sortBy.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFFCBD5E1)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF334155),
                                    selectedBorderColor = Color(0xFF38BDF8),
                                    enabled = true,
                                    selected = isSelected
                                ),
                                modifier = Modifier.testTag("admin_sort_${sortBy.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Complaints Table Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_complaints_table_card"),
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column {
                when (val state = adminUiState.complaintsState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = "Failed to load complaints: ${state.message}",
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    is UiState.Success -> {
                        val paged = state.data
                        if (paged.items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No complaints match the specified filters",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ZeromileButton(
                                        text = "Reset All Filters",
                                        onClick = {
                                            adminViewModel.setStatusFilter(null)
                                            adminViewModel.setPriorityFilter(null)
                                            adminViewModel.setSearchQuery("")
                                        },
                                        variant = ButtonVariant.OUTLINE
                                    )
                                }
                            }
                        } else {
                            if (isWide) {
                                // Wide Screen: Full Tabular View with Horizontal Scroll
                                Box(modifier = Modifier.horizontalScroll(tableHorizontalScroll)) {
                                    Column(modifier = Modifier.width(960.dp)) {
                                        // Header
                                        TableHeaderRow()

                                        // Rows
                                        paged.items.forEachIndexed { index, complaint ->
                                            TableDataRow(
                                                complaint = complaint,
                                                isEven = index % 2 == 0,
                                                onViewDetail = { adminViewModel.openComplaintDetail(complaint.id) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Mobile Screen: Responsive Card Stack
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    paged.items.forEach { complaint ->
                                        AdminComplaintMobileCard(
                                            complaint = complaint,
                                            onViewDetail = { adminViewModel.openComplaintDetail(complaint.id) }
                                        )
                                    }
                                }
                            }

                            // Pagination Controls Footer
                            PaginationFooter(
                                paged = paged,
                                onPageChange = { page -> adminViewModel.setPage(page) }
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
private fun AdminComplaintMobileCard(
    complaint: ComplaintRecord,
    onViewDetail: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_complaint_row_${complaint.complaintNumber}"),
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: ID, Date, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = complaint.complaintNumber,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    )
                    Text(
                        text = complaint.createdAt?.take(10) ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    )
                }

                val badgeStatus = when (complaint.status.lowercase()) {
                    "submitted" -> BadgeStatus.WARNING
                    "assigned", "in progress" -> BadgeStatus.INFO
                    "resolved" -> BadgeStatus.SUCCESS
                    else -> BadgeStatus.DEFAULT
                }
                ZeromileStatusBadge(text = complaint.status, status = badgeStatus)
            }

            // Row 2: Service & Category + Priority Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = complaint.service?.name ?: "Civic Grievance",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = complaint.category?.name ?: "Municipal",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8)
                        )
                    )
                }

                val prioColor = when (complaint.priority.lowercase()) {
                    "critical" -> Color(0xFFEF4444)
                    "high" -> Color(0xFFF97316)
                    "medium" -> Color(0xFFFBBF24)
                    else -> Color(0xFF10B981)
                }
                Surface(
                    color = prioColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, prioColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = complaint.priority.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = prioColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Location & Ward
            if (complaint.locationText.isNotBlank() || complaint.ward != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = listOfNotNull(complaint.locationText.ifBlank { null }, complaint.ward?.name).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1)),
                        maxLines = 1
                    )
                }
            }

            // Description preview if available
            if (complaint.description.isNotBlank()) {
                Text(
                    text = complaint.description.lines().firstOrNull() ?: complaint.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        lineHeight = 18.sp
                    ),
                    maxLines = 2
                )
            }

            // Footer with Input Mode and View Details button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isVoice = complaint.inputMode.equals("voice", true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isVoice) Icons.Default.Mic else Icons.Default.TextFields,
                        contentDescription = complaint.inputMode,
                        tint = if (isVoice) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVoice) "Voice (${complaint.language})" else "Text Input",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    )
                }

                ZeromileButton(
                    text = "View Details",
                    icon = Icons.Default.Visibility,
                    onClick = onViewDetail,
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.testTag("admin_view_detail_btn_${complaint.complaintNumber}")
                )
            }
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Complaint #", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(150.dp))
        Text("Service & Category", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(190.dp))
        Text("Mode / Input", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(110.dp))
        Text("Location & Ward", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(170.dp))
        Text("Priority", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(90.dp))
        Text("Status", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(120.dp))
        Text("Action", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(130.dp))
    }
}

@Composable
private fun TableDataRow(
    complaint: ComplaintRecord,
    isEven: Boolean,
    onViewDetail: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isEven) Color(0xFF1E293B) else Color(0xFF182234))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("admin_complaint_row_${complaint.complaintNumber}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Complaint Number & Date
        Column(modifier = Modifier.width(150.dp)) {
            Text(
                text = complaint.complaintNumber,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            )
            Text(
                text = complaint.createdAt?.take(10) ?: "",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            )
        }

        // Service & Category
        Column(modifier = Modifier.width(190.dp)) {
            Text(
                text = complaint.service?.name ?: "Civic Grievance",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 1
            )
            Text(
                text = complaint.category?.name ?: "Municipal",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                ),
                maxLines = 1
            )
        }

        // Input Mode (Voice / Text)
        Row(
            modifier = Modifier.width(110.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isVoice = complaint.inputMode.equals("voice", true)
            Icon(
                imageVector = if (isVoice) Icons.Default.Mic else Icons.Default.TextFields,
                contentDescription = complaint.inputMode,
                tint = if (isVoice) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isVoice) "Voice (${complaint.language})" else "Text",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
            )
        }

        // Location & Ward
        Column(modifier = Modifier.width(170.dp)) {
            Text(
                text = complaint.locationText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFE2E8F0)
                ),
                maxLines = 1
            )
            Text(
                text = complaint.ward?.name ?: "Ward",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                ),
                maxLines = 1
            )
        }

        // Priority
        Box(modifier = Modifier.width(90.dp)) {
            val prioColor = when (complaint.priority.lowercase()) {
                "critical" -> Color(0xFFEF4444)
                "high" -> Color(0xFFF97316)
                "medium" -> Color(0xFFFBBF24)
                else -> Color(0xFF10B981)
            }
            Surface(
                color = prioColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, prioColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = complaint.priority,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = prioColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Status
        Box(modifier = Modifier.width(120.dp)) {
            val badgeStatus = when (complaint.status.lowercase()) {
                "submitted" -> BadgeStatus.WARNING
                "assigned", "in progress" -> BadgeStatus.INFO
                "resolved" -> BadgeStatus.SUCCESS
                else -> BadgeStatus.DEFAULT
            }
            ZeromileStatusBadge(text = complaint.status, status = badgeStatus)
        }

        // Actions: View Details
        Box(modifier = Modifier.width(130.dp)) {
            ZeromileButton(
                text = "View Details",
                icon = Icons.Default.Visibility,
                onClick = onViewDetail,
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.testTag("admin_view_detail_btn_${complaint.complaintNumber}")
            )
        }
    }
}

@Composable
private fun PaginationFooter(
    paged: PagedResult<ComplaintRecord>,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val startItem = if (paged.totalCount == 0) 0 else ((paged.page - 1) * paged.pageSize) + 1
        val endItem = kotlin.math.min(paged.page * paged.pageSize, paged.totalCount)

        Text(
            text = "Showing $startItem to $endItem of ${paged.totalCount} complaints",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onPageChange(paged.page - 1) },
                enabled = paged.hasPrevPage,
                modifier = Modifier.testTag("admin_pagination_prev_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous Page",
                    tint = if (paged.hasPrevPage) Color(0xFF38BDF8) else Color(0xFF475569)
                )
            }

            Text(
                text = "Page ${paged.page} of ${paged.totalPages}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )

            IconButton(
                onClick = { onPageChange(paged.page + 1) },
                enabled = paged.hasNextPage,
                modifier = Modifier.testTag("admin_pagination_next_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next Page",
                    tint = if (paged.hasNextPage) Color(0xFF38BDF8) else Color(0xFF475569)
                )
            }
        }
    }
}
