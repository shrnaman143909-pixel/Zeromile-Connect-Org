package com.example

import com.example.zeromile.data.model.CreateCivicUpdatePayload
import com.example.zeromile.data.repository.AdminRepository
import com.example.zeromile.data.repository.CivicRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase10UnitTests {

    private lateinit var civicRepository: CivicRepository
    private lateinit var adminRepository: AdminRepository

    @Before
    fun setup() {
        civicRepository = CivicRepository()
        adminRepository = AdminRepository(civicRepository = civicRepository)
    }

    @Test
    fun testEmergencyServicesRetrieval() = runBlocking {
        val result = civicRepository.getEmergencyServices()
        assertTrue(result.isSuccess)
        val services = result.getOrThrow()

        assertTrue("Emergency services should not be empty", services.isNotEmpty())
        val police = services.find { it.phoneNumber == "112" || it.phoneNumber == "100" || it.name.contains("Police", ignoreCase = true) }
        assertNotNull("Police emergency should exist", police)
        val fire = services.find { it.phoneNumber == "101" || it.name.contains("Fire", ignoreCase = true) }
        assertNotNull("Fire service emergency should exist", fire)
        val ambulance = services.find { it.phoneNumber == "108" || it.phoneNumber == "102" || it.name.contains("Ambulance", ignoreCase = true) }
        assertNotNull("Ambulance emergency should exist", ambulance)
        val nmcDisaster = services.find { it.name.contains("Disaster", ignoreCase = true) || it.name.contains("Control Room", ignoreCase = true) }
        assertNotNull("NMC Disaster / Control Room should exist", nmcDisaster)
    }

    @Test
    fun testNoisePollutionServiceExists() = runBlocking {
        val servicesResult = civicRepository.getServices()
        assertTrue(servicesResult.isSuccess)
        val services = servicesResult.getOrThrow()
        val noiseService = services.find { it.name.contains("Noise", ignoreCase = true) }
        assertNotNull("Noise pollution complaint service must be available in catalog", noiseService)
        assertTrue("Noise pollution complaint should be active", noiseService!!.isActive)
    }

    @Test
    fun testCivicUpdatesFiltering() = runBlocking {
        val updatesResult = civicRepository.getCivicUpdates(null)
        assertTrue(updatesResult.isSuccess)
        val updates = updatesResult.getOrThrow()
        assertTrue("Civic updates should have seed items", updates.isNotEmpty())

        // Test Ward specific filter
        val ward1Updates = civicRepository.getCivicUpdates("w-1").getOrThrow()
        assertTrue("City-wide or ward 1 alerts should be returned", ward1Updates.all { it.wardId == null || it.wardId == "w-1" })
    }

    @Test
    fun testAdminCivicUpdateLifecycleAndAudit() = runBlocking {
        // Authenticate admin first
        adminRepository.adminSignIn("shrnavan1439009@gmail.com", "admin123")

        // 1. Create a civic update
        val payload = CreateCivicUpdatePayload(
            title = "Nagpur Metro Line 2 Maintenance",
            description = "Maintenance operations between Sitabuldi and Prajapati Nagar from 11 PM to 5 AM.",
            category = "Traffic Alert",
            priority = "high",
            wardId = null,
            isActive = true
        )

        val createResult = adminRepository.createCivicUpdate(payload)
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrThrow()
        assertEquals("Nagpur Metro Line 2 Maintenance", created.title)
        assertTrue(created.isActive)

        // 2. Toggle Active to false
        val updateResult = adminRepository.updateCivicUpdate(created.id, mapOf("is_active" to false))
        assertTrue(updateResult.isSuccess)
        val updated = updateResult.getOrThrow()
        assertFalse(updated.isActive)

        // 3. Verify in Admin updates list
        val adminList = adminRepository.getCivicUpdates().getOrThrow()
        val found = adminList.find { it.id == created.id }
        assertNotNull(found)
        assertFalse(found!!.isActive)

        // 4. Verify audit log was recorded
        val auditLogs = adminRepository.getAuditLogs().getOrThrow()
        val creationLog = auditLogs.find { it.entityId == created.id && it.action == "create_civic_update" }
        assertNotNull("Audit log for create_civic_update must be recorded", creationLog)

        // 5. Delete civic update
        val deleteResult = adminRepository.deleteCivicUpdate(created.id, created.title)
        assertTrue(deleteResult.isSuccess)

        val deletionLog = adminRepository.getAuditLogs().getOrThrow().find { it.entityId == created.id && it.action == "delete_civic_update" }
        assertNotNull("Audit log for delete_civic_update must be recorded", deletionLog)
    }
}
