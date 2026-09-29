package com.example

import com.example.zeromile.data.model.AdminComplaintFilter
import com.example.zeromile.data.model.AdminSortBy
import com.example.zeromile.data.repository.AdminRepository
import com.example.zeromile.data.repository.CivicRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminUnitTests {

    private lateinit var civicRepository: CivicRepository
    private lateinit var adminRepository: AdminRepository

    @Before
    fun setup() {
        civicRepository = CivicRepository()
        adminRepository = AdminRepository(civicRepository = civicRepository)
    }

    @Test
    fun testDashboardMetricsCalculation() = runBlocking {
        val metricsResult = adminRepository.getAdminMetrics()
        assertTrue(metricsResult.isSuccess)
        val metrics = metricsResult.getOrThrow()

        assertTrue(metrics.total >= 0)
        assertTrue(metrics.resolved >= 0)
    }

    @Test
    fun testComplaintsFilteringAndSorting() = runBlocking {
        val filter = AdminComplaintFilter(
            status = null,
            sortBy = AdminSortBy.NEWEST,
            page = 1,
            pageSize = 10
        )
        val pagedResult = adminRepository.getAdminComplaints(filter)
        assertTrue(pagedResult.isSuccess)
        val paged = pagedResult.getOrThrow()

        assertTrue(paged.page == 1)
        assertTrue(paged.pageSize == 10)
        assertTrue(paged.items.isNotEmpty())
    }

    @Test
    fun testAssignTeamAndStatusTransition() = runBlocking {
        // Authenticate admin first
        adminRepository.adminSignIn("shrnavan1439009@gmail.com", "admin123")

        val filter = AdminComplaintFilter(page = 1, pageSize = 5)
        val complaints = adminRepository.getAdminComplaints(filter).getOrThrow().items
        val targetComplaint = complaints.first()

        val assignResult = adminRepository.assignTeam(
            complaintId = targetComplaint.id,
            teamId = "team-roads-01",
            teamName = "Zone Road Repair Squad",
            note = "Deploy road roller squad"
        )
        assertTrue(assignResult.isSuccess)
        val updated = assignResult.getOrThrow()
        assertEquals("Assigned", updated.status)
        assertEquals("Zone Road Repair Squad", updated.assignedTeamName)

        val statusUpdateResult = adminRepository.updateStatus(
            complaintId = targetComplaint.id,
            newStatus = "Resolved",
            note = "Road potholes filled with asphalt",
            resolutionText = "Potholes filled and bitumen compacted."
        )
        assertTrue(statusUpdateResult.isSuccess)
        val resolved = statusUpdateResult.getOrThrow()
        assertEquals("Resolved", resolved.status)
        assertEquals("Potholes filled and bitumen compacted.", resolved.resolutionText)
    }

    @Test
    fun testAuditLogsRecorded() = runBlocking {
        adminRepository.adminSignIn("shrnavan1439009@gmail.com", "admin123")
        val logsResult = adminRepository.getAuditLogs()
        assertTrue(logsResult.isSuccess)
        val logs = logsResult.getOrThrow()
        assertTrue(logs.isNotEmpty())
    }
}
