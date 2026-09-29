package com.example.zeromile.ui.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeromile.data.model.AdminAuditLogRecord
import com.example.zeromile.ui.viewmodel.AdminUiState
import com.example.zeromile.ui.viewmodel.AdminViewModel
import com.example.zeromile.ui.viewmodel.UiState

/**
 * Phase 7 Admin Audit Logs View
 * Immutable administrative log tracking sensitive actions like assignment, status transitions, and complaint deletions.
 */
@Composable
fun AdminAuditLogsView(
    adminUiState: AdminUiState,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val tableHorizontalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
            .testTag("admin_audit_logs_view"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Administrative Audit Logs",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Text(
                    text = "Immutable audit ledger of municipal administrative actions & security events",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
            }

            IconButton(
                onClick = { adminViewModel.loadAuditLogs() },
                modifier = Modifier.testTag("admin_refresh_audit_logs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color(0xFF38BDF8)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_audit_logs_table_card"),
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column {
                when (val state = adminUiState.auditLogsState) {
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
                            text = "Failed to load audit logs: ${state.message}",
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    is UiState.Success -> {
                        val logs = state.data
                        if (logs.isEmpty()) {
                            Text(
                                text = "No audit log entries recorded.",
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(24.dp)
                            )
                        } else {
                            Box(modifier = Modifier.horizontalScroll(tableHorizontalScroll)) {
                                Column(modifier = Modifier.width(900.dp)) {
                                    // Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F172A))
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Timestamp", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(160.dp))
                                        Text("Action", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(170.dp))
                                        Text("Admin Email", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(220.dp))
                                        Text("Entity", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(100.dp))
                                        Text("Metadata / Parameters", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)), modifier = Modifier.width(250.dp))
                                    }

                                    // Rows
                                    logs.forEachIndexed { index, log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (index % 2 == 0) Color(0xFF1E293B) else Color(0xFF182234))
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .testTag("audit_log_row_${log.id}"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = log.createdAt?.take(19)?.replace("T", " ") ?: "",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp),
                                                modifier = Modifier.width(160.dp)
                                            )

                                            val actionColor = when {
                                                log.action.contains("DELETE", true) -> Color(0xFFEF4444)
                                                log.action.contains("ASSIGN", true) -> Color(0xFF38BDF8)
                                                log.action.contains("RESOLVE", true) -> Color(0xFF10B981)
                                                log.action.contains("AUTH", true) || log.action.contains("SIGN", true) -> Color(0xFFF59E0B)
                                                else -> Color(0xFFE2E8F0)
                                            }
                                            Text(
                                                text = log.action,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = actionColor,
                                                    fontSize = 12.sp
                                                ),
                                                modifier = Modifier.width(170.dp)
                                            )

                                            Text(
                                                text = log.adminEmail ?: "system",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.sp),
                                                modifier = Modifier.width(220.dp)
                                            )

                                            Text(
                                                text = "${log.entityType}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp),
                                                modifier = Modifier.width(100.dp)
                                            )

                                            Text(
                                                text = log.metadata ?: "—",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0), fontSize = 11.sp),
                                                modifier = Modifier.width(250.dp),
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
