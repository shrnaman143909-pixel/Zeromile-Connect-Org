package com.example

import com.example.zeromile.data.model.CreateComplaintPayload
import com.example.zeromile.data.repository.AdminRepository
import com.example.zeromile.data.repository.CivicRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase11SecurityTests {

    private lateinit var civicRepository: CivicRepository
    private lateinit var adminRepository: AdminRepository

    @Before
    fun setup() {
        civicRepository = CivicRepository()
        adminRepository = AdminRepository(civicRepository = civicRepository)
    }

    @Test
    fun testUnauthorizedCitizenDeniedAdminAccess() = runBlocking {
        // Citizen email should NOT have admin role access
        val citizenLoginResult = adminRepository.adminSignIn("citizen.user@gmail.com", "password123")
        assertTrue("Unauthorized user must not be granted admin credentials", citizenLoginResult.isFailure)
        assertTrue(citizenLoginResult.exceptionOrNull() is IllegalAccessException)

        // Operations without active admin session must fail
        val assignResult = adminRepository.assignTeam("test-id", "team-1", "Civic Squad", "Deploy squad")
        assertTrue(assignResult.isFailure)
        assertTrue(assignResult.exceptionOrNull() is IllegalAccessException)
    }

    @Test
    fun testAuthorizedAdminAccessGranted() = runBlocking {
        val adminLoginResult = adminRepository.adminSignIn("shrnavan1439009@gmail.com", "secure123")
        assertTrue("Designated admin email must successfully authenticate", adminLoginResult.isSuccess)
        val admin = adminLoginResult.getOrThrow()
        assertEquals("admin", admin.role)
    }

    @Test
    fun testInputValidationComplaintSubmission() = runBlocking {
        // 1. Too short description (< 5 chars)
        val shortDescPayload = CreateComplaintPayload(
            userId = "test-user",
            serviceId = "s1111111-1111-1111-1111-111111111111",
            description = "bad",
            originalTranscript = "bad noise",
            locationText = "Dharampeth, Nagpur"
        )
        val shortDescResult = civicRepository.submitComplaint(shortDescPayload)
        assertTrue("Description shorter than 5 chars must be rejected", shortDescResult.isFailure)

        // 2. Latitude out of range (> 90.0)
        val badLatPayload = CreateComplaintPayload(
            userId = "test-user",
            serviceId = "s1111111-1111-1111-1111-111111111111",
            description = "Loud midnight loudspeakers near temple complex",
            originalTranscript = "Loud midnight loudspeakers",
            locationText = "Dharampeth, Nagpur",
            latitude = 95.5,
            longitude = 79.08
        )
        val badLatResult = civicRepository.submitComplaint(badLatPayload)
        assertTrue("Latitude > 90.0 must be rejected", badLatResult.isFailure)

        // 3. Valid payload succeeds
        val validPayload = CreateComplaintPayload(
            userId = "test-user",
            serviceId = "s1111111-1111-1111-1111-111111111111",
            description = "Loud midnight loudspeakers near temple complex",
            originalTranscript = "Loud midnight loudspeakers near temple complex",
            locationText = "Ward 32, Dharampeth, Nagpur",
            latitude = 21.1458,
            longitude = 79.0882
        )
        val validResult = civicRepository.submitComplaint(validPayload)
        assertTrue("Valid complaint payload must succeed", validResult.isSuccess)
        val complaint = validResult.getOrThrow()
        assertTrue(complaint.complaintNumber.startsWith("NMC-"))
    }

    @Test
    fun testLegalStatusTransitionsEnforced() = runBlocking {
        adminRepository.adminSignIn("shrnavan1439009@gmail.com", "secure123")

        // Create a test complaint in Submitted state
        val validPayload = CreateComplaintPayload(
            userId = "test-user",
            serviceId = "s1111111-1111-1111-1111-111111111111",
            description = "Deep crater on Central Avenue near railway crossing",
            originalTranscript = "Deep crater on Central Avenue",
            locationText = "Central Avenue, Ward 12, Nagpur"
        )
        val complaint = civicRepository.submitComplaint(validPayload).getOrThrow()
        assertEquals("Submitted", complaint.status)

        // Legal: Submitted -> Assigned
        val assignResult = adminRepository.assignTeam(
            complaintId = complaint.id,
            teamId = "t2222222-2222-2222-2222-222222222222",
            teamName = "Zone Road Repair Squad",
            note = "Assigned for patch inspection"
        )
        assertTrue(assignResult.isSuccess)

        // Legal: Assigned -> In Progress
        val inProgressResult = adminRepository.updateStatus(
            complaintId = complaint.id,
            newStatus = "In Progress",
            note = "Bitumen mixer deployed"
        )
        assertTrue(inProgressResult.isSuccess)

        // Legal: In Progress -> Resolved
        val resolveResult = adminRepository.updateStatus(
            complaintId = complaint.id,
            newStatus = "Resolved",
            note = "Asphalt jetpatching complete",
            resolutionText = "Pothole filled and sealed with hot mix asphalt."
        )
        assertTrue(resolveResult.isSuccess)

        // Legal: Resolved -> Closed
        val closeResult = adminRepository.updateStatus(
            complaintId = complaint.id,
            newStatus = "Closed",
            note = "Citizen confirmed resolution"
        )
        assertTrue(closeResult.isSuccess)

        // Illegal: Closed -> Submitted (must be rejected!)
        val illegalRevertResult = adminRepository.updateStatus(
            complaintId = complaint.id,
            newStatus = "Submitted",
            note = "Attempting illegal reversion"
        )
        assertTrue("Illegal transition from Closed to Submitted must be rejected", illegalRevertResult.isFailure)
        assertTrue(illegalRevertResult.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testAdminDeleteWithAuditTrail() = runBlocking {
        adminRepository.adminSignIn("shrnavan1439009@gmail.com", "secure123")

        val validPayload = CreateComplaintPayload(
            userId = "test-user",
            serviceId = "s1111111-1111-1111-1111-111111111111",
            description = "Duplicate noise ticket for testing administrative purge",
            originalTranscript = "Duplicate noise ticket",
            locationText = "Sitabuldi, Nagpur"
        )
        val complaint = civicRepository.submitComplaint(validPayload).getOrThrow()

        val deleteResult = adminRepository.deleteComplaint(
            complaintId = complaint.id,
            reason = "Duplicate complaint filed in error"
        )
        assertTrue("Admin delete must succeed", deleteResult.isSuccess)

        // Complaint should no longer exist in repository
        val trackingDetails = civicRepository.getComplaintTrackingDetails(complaint.complaintNumber).getOrNull()
        org.junit.Assert.assertNull("Deleted complaint should not be found", trackingDetails)

        // Audit log must contain the DELETE_COMPLAINT record
        val auditLogs = adminRepository.getAuditLogs().getOrThrow()
        val deleteLog = auditLogs.find { it.action == "DELETE_COMPLAINT" && it.entityId == complaint.id }
        assertNotNull("Audit log must record DELETE_COMPLAINT action", deleteLog)
    }
}
