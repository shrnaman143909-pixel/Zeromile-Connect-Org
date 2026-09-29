package com.example.zeromile.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.zeromile.data.model.CivicService
import com.example.zeromile.data.model.Department
import com.example.zeromile.data.model.NotificationRecord
import com.example.zeromile.data.model.NotificationType
import com.example.zeromile.data.model.Profile
import com.example.zeromile.data.model.ServiceCategory
import com.example.zeromile.data.model.UpdateNotificationReadPayload
import com.example.zeromile.data.model.Ward
import com.example.zeromile.data.model.EmergencyService
import com.example.zeromile.data.model.CivicUpdate
import com.example.zeromile.data.model.CreateCivicUpdatePayload
import com.example.zeromile.data.remote.RealtimeConnectionState
import com.example.zeromile.data.remote.RealtimeEvent
import com.example.zeromile.data.remote.SupabaseApi
import com.example.zeromile.data.remote.SupabaseClientProvider
import com.example.zeromile.data.remote.SupabaseRealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DataSourceMode {
    SUPABASE_CLOUD,
    PROTOTYPE_FALLBACK
}

data class ConnectionStatus(
    val isConfigured: Boolean,
    val mode: DataSourceMode,
    val endpoint: String,
    val lastError: String? = null
)

class CivicRepository(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
) {
    private val tag = "CivicRepository"

    private val isConfigured: Boolean =
        supabaseUrl.isNotBlank() &&
        supabaseKey.isNotBlank() &&
        !supabaseUrl.contains("placeholder") &&
        !supabaseUrl.contains("your-project") &&
        supabaseUrl.startsWith("http")

    private val api: SupabaseApi? = if (isConfigured) {
        try {
            SupabaseClientProvider.create(supabaseUrl, supabaseKey)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Supabase client", e)
            null
        }
    } else {
        null
    }

    var lastError: String? = null
        private set

    var currentMode: DataSourceMode = if (isConfigured && api != null) {
        DataSourceMode.SUPABASE_CLOUD
    } else {
        DataSourceMode.PROTOTYPE_FALLBACK
    }
        private set

    fun getConnectionStatus(): ConnectionStatus {
        return ConnectionStatus(
            isConfigured = isConfigured,
            mode = currentMode,
            endpoint = if (isConfigured) supabaseUrl else "Prototype Seed (Nagpur)",
            lastError = lastError
        )
    }

    suspend fun getServiceCategories(): Result<List<ServiceCategory>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val categories = api.getServiceCategories()
                if (categories.isNotEmpty()) {
                    currentMode = DataSourceMode.SUPABASE_CLOUD
                    lastError = null
                    return@withContext Result.success(categories)
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase fetch failed for service categories; falling back to seed", e)
                lastError = e.localizedMessage ?: "Failed to connect to Supabase"
            }
        }
        currentMode = DataSourceMode.PROTOTYPE_FALLBACK
        Result.success(SeedData.categories)
    }

    suspend fun getServices(categoryId: String? = null): Result<List<CivicService>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val queryParam = categoryId?.let { "eq.$it" }
                val services = api.getServices(queryParam)
                if (services.isNotEmpty()) {
                    currentMode = DataSourceMode.SUPABASE_CLOUD
                    lastError = null
                    return@withContext Result.success(services)
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase fetch failed for services; falling back to seed", e)
                lastError = e.localizedMessage ?: "Failed to connect to Supabase"
            }
        }
        currentMode = DataSourceMode.PROTOTYPE_FALLBACK
        val list = if (categoryId.isNullOrBlank()) {
            SeedData.services
        } else {
            SeedData.services.filter { it.categoryId == categoryId }
        }
        Result.success(list)
    }

    suspend fun getDepartments(): Result<List<Department>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val depts = api.getDepartments()
                if (depts.isNotEmpty()) {
                    return@withContext Result.success(depts)
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase fetch failed for departments", e)
            }
        }
        Result.success(SeedData.departments)
    }

    suspend fun getWards(): Result<List<Ward>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val wards = api.getWards()
                if (wards.isNotEmpty()) {
                    return@withContext Result.success(wards)
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase fetch failed for wards", e)
            }
        }
        Result.success(SeedData.wards)
    }

    suspend fun getProfile(userId: String): Result<Profile> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val profiles = api.getProfiles("eq.$userId")
                if (profiles.isNotEmpty()) {
                    return@withContext Result.success(profiles.first())
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase fetch failed for profile", e)
            }
        }
        // Return default citizen profile for this specific user
        Result.success(
            Profile(
                id = userId,
                userId = userId,
                fullName = "Nagpur Citizen",
                phone = null,
                city = "Nagpur",
                preferredLanguage = "en",
                role = "citizen"
            )
        )
    }

    // In-memory complaint and activity store for local session persistence
    private val localComplaints = mutableListOf<com.example.zeromile.data.model.ComplaintRecord>()
    private val localStatusHistory = mutableListOf<com.example.zeromile.data.model.ComplaintStatusHistoryRecord>()
    private val localComplaintUpdates = mutableListOf<com.example.zeromile.data.model.ComplaintUpdateRecord>()
    private val localNotifications = mutableListOf<NotificationRecord>()
    private val localEvidence = mutableListOf<com.example.zeromile.data.model.ComplaintEvidenceRecord>()
    private var sequenceCounter = 1250

    // Phase 8: Supabase Realtime Client & Event Stream
    val realtimeClient: SupabaseRealtimeClient by lazy {
        SupabaseRealtimeClient(
            supabaseUrl = supabaseUrl,
            apiKey = supabaseKey,
            authTokenProvider = { SupabaseClientProvider.getAuthToken() }
        )
    }

    private val _notificationsFlow = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationRecord>> = _notificationsFlow.asStateFlow()

    private val _realtimeEvents = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val realtimeEvents: SharedFlow<RealtimeEvent> = _realtimeEvents.asSharedFlow()

    /**
     * Submit a real complaint to Supabase.
     * Enforces required constraints, controlled status ('Submitted'), and retrieves server-generated complaint number.
     * Falls back to high-fidelity in-memory storage adhering to the exact NMC-YEAR-6DIGIT format if offline/placeholder.
     */
    suspend fun submitComplaint(payload: com.example.zeromile.data.model.CreateComplaintPayload): Result<com.example.zeromile.data.model.ComplaintRecord> = withContext(Dispatchers.IO) {
        // Database-aligned security & integrity input validation
        val desc = payload.description.trim()
        if (desc.length < 5 || desc.length > 4000) {
            return@withContext Result.failure(IllegalArgumentException("Grievance description must be between 5 and 4000 characters."))
        }
        val transcript = payload.originalTranscript.trim()
        if (transcript.length < 3 || transcript.length > 4000) {
            return@withContext Result.failure(IllegalArgumentException("Original transcript must be between 3 and 4000 characters."))
        }
        val location = payload.locationText.trim()
        if (location.length < 3 || location.length > 500) {
            return@withContext Result.failure(IllegalArgumentException("Location must be between 3 and 500 characters."))
        }
        if (payload.latitude != null && (payload.latitude < -90.0 || payload.latitude > 90.0)) {
            return@withContext Result.failure(IllegalArgumentException("Latitude must be between -90.0 and 90.0 degrees."))
        }
        if (payload.longitude != null && (payload.longitude < -180.0 || payload.longitude > 180.0)) {
            return@withContext Result.failure(IllegalArgumentException("Longitude must be between -180.0 and 180.0 degrees."))
        }
        if (payload.locationAccuracyMeters != null && payload.locationAccuracyMeters < 0.0) {
            return@withContext Result.failure(IllegalArgumentException("Location accuracy cannot be negative."))
        }
        if (payload.serviceId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Service ID cannot be blank."))
        }

        if (api != null) {
            try {
                val insertedList = api.createComplaint(payload)
                if (insertedList.isNotEmpty()) {
                    val record = insertedList.first()
                    currentMode = DataSourceMode.SUPABASE_CLOUD
                    lastError = null
                    // Also cache locally for instant Activity availability
                    synchronized(localComplaints) {
                        localComplaints.removeAll { it.id == record.id || it.complaintNumber == record.complaintNumber }
                        localComplaints.add(0, record)
                    }
                    return@withContext Result.success(record)
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase complaint insertion failed; recording error and using concurrency-safe local persistence", e)
                lastError = e.localizedMessage ?: "Failed to insert into Supabase"
            }
        }

        // Concurrency-safe local generation adhering to strict format NMC-{YEAR}-{6 DIGITS}
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val generatedNumber: String
        val complaintId = java.util.UUID.randomUUID().toString()

        synchronized(localComplaints) {
            val num = sequenceCounter++
            generatedNumber = "NMC-$currentYear-${String.format(java.util.Locale.US, "%06d", num)}"
        }

        val service = SeedData.services.find { it.id == payload.serviceId }
        val category = SeedData.categories.find { it.id == payload.categoryId || it.id == service?.categoryId }
        val department = SeedData.departments.find { it.id == payload.departmentId || it.id == service?.departmentId }
        val ward = SeedData.wards.find { it.id == payload.wardId }

        val record = com.example.zeromile.data.model.ComplaintRecord(
            id = complaintId,
            complaintNumber = generatedNumber,
            userId = payload.userId,
            serviceId = payload.serviceId,
            categoryId = payload.categoryId ?: service?.categoryId,
            departmentId = payload.departmentId ?: service?.departmentId,
            description = payload.description,
            originalTranscript = payload.originalTranscript,
            inputMode = payload.inputMode,
            language = payload.language,
            locationText = payload.locationText,
            wardId = payload.wardId,
            latitude = payload.latitude,
            longitude = payload.longitude,
            locationAccuracyMeters = payload.locationAccuracyMeters,
            locationSource = payload.locationSource ?: "manual",
            priority = payload.priority,
            aiConfidence = payload.aiConfidence ?: 0.90,
            aiSummary = payload.aiSummary,
            aiReason = payload.aiReason,
            status = "Submitted",
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date()),
            service = service,
            category = category,
            department = department,
            ward = ward
        )

        synchronized(localComplaints) {
            localComplaints.add(0, record)
        }

        // Add initial status history record for the newly submitted complaint
        val initialHistory = com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = java.util.UUID.randomUUID().toString(),
            complaintId = record.id,
            status = "Submitted",
            note = "Grievance registered in Nagpur Municipal Corporation ledger.",
            assignedTeam = null,
            changedBy = record.userId,
            createdAt = record.createdAt ?: ""
        )
        synchronized(localStatusHistory) {
            localStatusHistory.add(initialHistory)
        }

        Result.success(record)
    }

    /**
     * Get complaints for Activity.
     * Enforces user ownership via user_id filter.
     */
    suspend fun getComplaints(userId: String? = null): Result<List<com.example.zeromile.data.model.ComplaintRecord>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val queryParam = userId?.let { "eq.$it" }
                val cloudComplaints = api.getComplaints(queryParam)
                currentMode = DataSourceMode.SUPABASE_CLOUD
                lastError = null
                return@withContext Result.success(cloudComplaints)
            } catch (e: Exception) {
                Log.w(tag, "Supabase fetch failed for complaints; using local complaints store", e)
                lastError = e.localizedMessage ?: "Failed to connect to Supabase"
            }
        }

        val list = synchronized(localComplaints) {
            if (userId != null) {
                localComplaints.filter { it.userId == userId }
            } else {
                localComplaints.toList()
            }
        }
        Result.success(list)
    }

    suspend fun getComplaintById(id: String): Result<com.example.zeromile.data.model.ComplaintRecord?> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val list = api.getComplaintById("eq.$id")
                if (list.isNotEmpty()) {
                    return@withContext Result.success(list.first())
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase fetch failed for complaint $id", e)
            }
        }

        val found = synchronized(localComplaints) {
            localComplaints.find { it.id == id || it.complaintNumber == id }
        } ?: SeedData.initialComplaints.find { it.id == id || it.complaintNumber == id }
        Result.success(found)
    }

    /**
     * Fetch complete complaint tracking details:
     * 1. Verified complaint record.
     * 2. Chronological status history from complaint_status_history.
     * 3. Official municipal updates from complaint_updates.
     */
    suspend fun getComplaintTrackingDetails(complaintIdOrNumber: String): Result<com.example.zeromile.data.model.ComplaintTrackingDetails?> = withContext(Dispatchers.IO) {
        // Step 1: Fetch complaint
        var complaint: com.example.zeromile.data.model.ComplaintRecord? = null
        if (api != null) {
            try {
                // Try searching by ID or complaint_number
                val byId = api.getComplaintById("eq.$complaintIdOrNumber")
                complaint = byId.firstOrNull()
            } catch (e: Exception) {
                Log.w(tag, "Supabase fetch complaint failed for $complaintIdOrNumber", e)
            }
        }

        if (complaint == null) {
            complaint = synchronized(localComplaints) {
                localComplaints.find { it.id == complaintIdOrNumber || it.complaintNumber == complaintIdOrNumber }
            } ?: SeedData.initialComplaints.find { it.id == complaintIdOrNumber || it.complaintNumber == complaintIdOrNumber }
        }

        if (complaint == null) {
            return@withContext Result.success(null)
        }

        val targetComplaintId = complaint.id
        var historyList: List<com.example.zeromile.data.model.ComplaintStatusHistoryRecord> = emptyList()
        var updateList: List<com.example.zeromile.data.model.ComplaintUpdateRecord> = emptyList()

        if (api != null) {
            try {
                historyList = api.getStatusHistory("eq.$targetComplaintId")
            } catch (e: Exception) {
                Log.w(tag, "Supabase status history fetch failed for $targetComplaintId", e)
            }
            try {
                updateList = api.getComplaintUpdates("eq.$targetComplaintId")
            } catch (e: Exception) {
                Log.w(tag, "Supabase updates fetch failed for $targetComplaintId", e)
            }
        }

        if (historyList.isEmpty()) {
            historyList = synchronized(localStatusHistory) {
                localStatusHistory.filter { it.complaintId == targetComplaintId }
                    .sortedBy { it.createdAt }
            }
        }
        if (historyList.isEmpty()) {
            historyList = SeedData.initialStatusHistory.filter { it.complaintId == targetComplaintId }
                .sortedBy { it.createdAt }
        }

        if (updateList.isEmpty()) {
            updateList = synchronized(localComplaintUpdates) {
                localComplaintUpdates.filter { it.complaintId == targetComplaintId }
                    .sortedBy { it.createdAt }
            }
        }
        if (updateList.isEmpty()) {
            updateList = SeedData.initialComplaintUpdates.filter { it.complaintId == targetComplaintId }
                .sortedBy { it.createdAt }
        }

        // If history is still empty (e.g. freshly inserted without history trigger in memory), ensure a Submitted event exists
        if (historyList.isEmpty()) {
            historyList = listOf(
                com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
                    id = "h-${complaint.id}",
                    complaintId = complaint.id,
                    status = "Submitted",
                    note = "Grievance registered in municipal ledger.",
                    assignedTeam = null,
                    changedBy = complaint.userId,
                    createdAt = complaint.createdAt ?: ""
                )
            )
        }

        // Find assigned team from history if any
        val assignedTeam = historyList.lastOrNull { !it.assignedTeam.isNullOrBlank() }?.assignedTeam

        // Find resolution note and time if resolved
        val resolvedRecord = historyList.lastOrNull { it.status.equals("Resolved", ignoreCase = true) }
        val resolutionNote = resolvedRecord?.note
        val resolvedAt = resolvedRecord?.createdAt

        // Phase 9: Fetch evidence attached to this complaint
        var evidenceList: List<com.example.zeromile.data.model.ComplaintEvidenceRecord> = emptyList()
        if (api != null) {
            try {
                val remoteEv = api.getEvidenceByComplaintId("eq.$targetComplaintId")
                if (remoteEv.isNotEmpty()) {
                    evidenceList = remoteEv.map { ev ->
                        val signedUrl = try {
                            val signRes = api.createSignedUrl(ev.storagePath, com.example.zeromile.data.model.SignUrlPayload(3600))
                            val raw = signRes.signedURL
                            if (raw.startsWith("http")) raw else "${supabaseUrl.trimEnd('/')}/storage/v1${if (raw.startsWith("/")) "" else "/"}$raw"
                        } catch (e: Exception) {
                            ev.signedUrl
                        }
                        ev.copy(signedUrl = signedUrl ?: ev.signedUrl)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase evidence query failed for $targetComplaintId", e)
            }
        }
        if (evidenceList.isEmpty()) {
            evidenceList = synchronized(localEvidence) {
                localEvidence.filter { it.complaintId == targetComplaintId }.sortedByDescending { it.createdAt }
            }
        }

        val details = com.example.zeromile.data.model.ComplaintTrackingDetails(
            complaint = complaint,
            history = historyList,
            updates = updateList,
            evidence = evidenceList,
            assignedTeam = assignedTeam,
            resolutionNote = resolutionNote,
            resolvedAt = resolvedAt
        )

        Result.success(details)
    }

    // Phase 9 Evidence Management Methods
    suspend fun getEvidenceForComplaint(complaintId: String): Result<List<com.example.zeromile.data.model.ComplaintEvidenceRecord>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val remoteList = api.getEvidenceByComplaintId("eq.$complaintId")
                if (remoteList.isNotEmpty()) {
                    val resolved = remoteList.map { item ->
                        val signedUrl = try {
                            val signResponse = api.createSignedUrl(item.storagePath, com.example.zeromile.data.model.SignUrlPayload(3600))
                            val raw = signResponse.signedURL
                            if (raw.startsWith("http")) raw else "${supabaseUrl.trimEnd('/')}/storage/v1${if (raw.startsWith("/")) "" else "/"}$raw"
                        } catch (e: Exception) {
                            item.signedUrl
                        }
                        item.copy(signedUrl = signedUrl ?: item.signedUrl)
                    }
                    synchronized(localEvidence) {
                        localEvidence.removeAll { it.complaintId == complaintId }
                        localEvidence.addAll(resolved)
                    }
                    return@withContext Result.success(resolved)
                }
            } catch (e: Exception) {
                Log.w(tag, "Supabase evidence query failed for $complaintId", e)
            }
        }
        val local = synchronized(localEvidence) {
            localEvidence.filter { it.complaintId == complaintId }.sortedByDescending { it.createdAt }
        }
        Result.success(local)
    }

    suspend fun uploadEvidence(
        complaintId: String,
        userId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<com.example.zeromile.data.model.ComplaintEvidenceRecord> = withContext(Dispatchers.IO) {
        val result = com.example.zeromile.data.evidence.EvidenceManager.uploadToSupabaseStorage(
            api = api,
            supabaseUrl = supabaseUrl,
            complaintId = complaintId,
            userId = userId,
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
        result.onSuccess { record ->
            synchronized(localEvidence) {
                localEvidence.removeAll { it.id == record.id }
                localEvidence.add(0, record)
            }
        }
        result
    }

    suspend fun uploadStagedEvidenceList(
        complaintId: String,
        userId: String,
        stagedItems: List<com.example.zeromile.data.model.StagedEvidenceItem>
    ): List<com.example.zeromile.data.model.ComplaintEvidenceRecord> = withContext(Dispatchers.IO) {
        val uploadedRecords = mutableListOf<com.example.zeromile.data.model.ComplaintEvidenceRecord>()
        stagedItems.forEach { staged ->
            val bytes = staged.bytes ?: byteArrayOf()
            if (bytes.isNotEmpty()) {
                val res = uploadEvidence(
                    complaintId = complaintId,
                    userId = userId,
                    fileName = staged.fileName,
                    mimeType = staged.mimeType,
                    bytes = bytes
                )
                res.getOrNull()?.let { uploadedRecords.add(it) }
            }
        }
        uploadedRecords
    }

    suspend fun deleteEvidence(evidenceId: String, storagePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                api.deleteEvidence("eq.$evidenceId")
            } catch (e: Exception) {
                Log.w(tag, "Failed remote delete evidence row", e)
            }
            try {
                api.deleteStorageObject(storagePath)
            } catch (e: Exception) {
                Log.w(tag, "Failed remote delete storage object", e)
            }
        }
        synchronized(localEvidence) {
            localEvidence.removeAll { it.id == evidenceId || it.storagePath == storagePath }
        }
        Result.success(true)
    }

    suspend fun deleteEvidenceForComplaint(complaintId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val toDelete = synchronized(localEvidence) { localEvidence.filter { it.complaintId == complaintId } }
        if (api != null) {
            try {
                api.deleteEvidenceByComplaintId("eq.$complaintId")
            } catch (e: Exception) {
                Log.w(tag, "Failed remote delete evidence by complaint_id", e)
            }
            toDelete.forEach { item ->
                try {
                    api.deleteStorageObject(item.storagePath)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to delete storage object ${item.storagePath}", e)
                }
            }
        }
        synchronized(localEvidence) {
            localEvidence.removeAll { it.complaintId == complaintId }
        }
        Result.success(true)
    }

    // Phase 7 Administrative synchronization helpers
    fun getLocalComplaints(): List<com.example.zeromile.data.model.ComplaintRecord> = synchronized(localComplaints) { localComplaints.toList() }

    fun updateLocalComplaint(updated: com.example.zeromile.data.model.ComplaintRecord) = synchronized(localComplaints) {
        val index = localComplaints.indexOfFirst { it.id == updated.id || it.complaintNumber == updated.complaintNumber }
        if (index >= 0) {
            localComplaints[index] = updated
        } else {
            localComplaints.add(0, updated)
        }
    }

    fun deleteLocalComplaint(complaintId: String): Boolean = synchronized(localComplaints) {
        val removed = localComplaints.removeAll { it.id == complaintId || it.complaintNumber == complaintId }
        synchronized(localStatusHistory) { localStatusHistory.removeAll { it.complaintId == complaintId } }
        synchronized(localComplaintUpdates) { localComplaintUpdates.removeAll { it.complaintId == complaintId } }
        synchronized(localEvidence) { localEvidence.removeAll { it.complaintId == complaintId } }
        removed
    }

    fun addLocalStatusHistory(record: com.example.zeromile.data.model.ComplaintStatusHistoryRecord) = synchronized(localStatusHistory) {
        localStatusHistory.add(record)
    }

    fun addLocalComplaintUpdate(record: com.example.zeromile.data.model.ComplaintUpdateRecord) = synchronized(localComplaintUpdates) {
        localComplaintUpdates.add(record)
    }

    fun getLocalStatusHistory(): List<com.example.zeromile.data.model.ComplaintStatusHistoryRecord> = synchronized(localStatusHistory) { localStatusHistory.toList() }

    fun getLocalComplaintUpdates(): List<com.example.zeromile.data.model.ComplaintUpdateRecord> = synchronized(localComplaintUpdates) { localComplaintUpdates.toList() }

    // Phase 8: Notifications Management & Synchronization
    suspend fun getNotifications(userId: String): Result<List<NotificationRecord>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val remoteList = api.getNotifications(userIdFilter = "eq.$userId", limit = 50)
                synchronized(localNotifications) {
                    localNotifications.removeAll { it.userId == userId }
                    localNotifications.addAll(remoteList)
                }
                val filtered = synchronized(localNotifications) { localNotifications.filter { it.userId == userId }.sortedByDescending { it.createdAt } }
                _notificationsFlow.value = filtered
                return@withContext Result.success(remoteList)
            } catch (e: Exception) {
                Log.w(tag, "Supabase notifications fetch failed; falling back to local store", e)
            }
        }
        val userNotifs = synchronized(localNotifications) {
            localNotifications.filter { it.userId == userId }
                .sortedByDescending { it.createdAt }
        }
        _notificationsFlow.value = userNotifs
        Result.success(userNotifs)
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                api.markNotificationRead("eq.$notificationId", UpdateNotificationReadPayload(true))
            } catch (e: Exception) {
                Log.w(tag, "Supabase markNotificationRead failed; updating local cache", e)
            }
        }
        synchronized(localNotifications) {
            val idx = localNotifications.indexOfFirst { it.id == notificationId }
            if (idx >= 0) {
                val current = localNotifications[idx]
                localNotifications[idx] = current.copy(read = true)
            }
        }
        _notificationsFlow.value = synchronized(localNotifications) { localNotifications.toList() }
        Result.success(true)
    }

    suspend fun markAllNotificationsAsRead(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                api.markAllNotificationsRead()
            } catch (e: Exception) {
                Log.w(tag, "Supabase markAllNotificationsRead failed; updating local cache", e)
            }
        }
        var updatedCount = 0
        synchronized(localNotifications) {
            for (i in localNotifications.indices) {
                val notif = localNotifications[i]
                if (notif.userId == userId) {
                    if (!notif.read) {
                        localNotifications[i] = notif.copy(read = true)
                        updatedCount++
                    }
                }
            }
        }
        _notificationsFlow.value = synchronized(localNotifications) { localNotifications.toList() }
        Result.success(updatedCount)
    }

    fun addNotificationFromEvent(notification: NotificationRecord) {
        synchronized(localNotifications) {
            // Idempotency check
            if (notification.idempotencyKey != null && localNotifications.any { it.idempotencyKey == notification.idempotencyKey }) {
                return
            }
            if (localNotifications.none { it.id == notification.id }) {
                localNotifications.add(0, notification)
            }
        }
        _notificationsFlow.value = synchronized(localNotifications) { localNotifications.toList() }
    }

    fun deleteNotificationsForComplaint(complaintId: String) = synchronized(localNotifications) {
        localNotifications.removeAll { it.complaintId == complaintId }
        _notificationsFlow.value = localNotifications.toList()
    }

    fun getLocalNotifications(): List<NotificationRecord> = synchronized(localNotifications) { localNotifications.toList() }

    // Phase 10: Emergency Services & Civic Updates
    private val localEmergencyServices = java.util.Collections.synchronizedList(SeedData.initialEmergencyServices.toMutableList())
    private val localCivicUpdates = java.util.Collections.synchronizedList(SeedData.initialCivicUpdates.toMutableList())

    suspend fun getEmergencyServices(): Result<List<EmergencyService>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val list = api.getEmergencyServices()
                if (list.isNotEmpty()) {
                    return@withContext Result.success(list)
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to load emergency services from Supabase, using seed: ${e.message}")
            }
        }
        Result.success(synchronized(localEmergencyServices) { localEmergencyServices.toList() })
    }

    suspend fun getCivicUpdates(wardId: String? = null): Result<List<CivicUpdate>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val list = api.getCivicUpdates(wardId)
                if (list.isNotEmpty()) {
                    return@withContext Result.success(list)
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to load civic updates from Supabase, using seed: ${e.message}")
            }
        }
        val filtered = synchronized(localCivicUpdates) {
            localCivicUpdates.filter { update ->
                update.isActive && (wardId == null || update.wardId == null || update.wardId == wardId)
            }.toList()
        }
        Result.success(filtered)
    }

    suspend fun getAllCivicUpdatesAdmin(): Result<List<CivicUpdate>> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val list = api.getAllCivicUpdatesAdmin()
                return@withContext Result.success(list)
            } catch (e: Exception) {
                Log.w(tag, "Failed to load admin civic updates from Supabase, using seed: ${e.message}")
            }
        }
        Result.success(synchronized(localCivicUpdates) { localCivicUpdates.toList() })
    }

    suspend fun createCivicUpdate(payload: CreateCivicUpdatePayload): Result<CivicUpdate> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val created = api.createCivicUpdate(payload)
                if (created.isNotEmpty()) {
                    return@withContext Result.success(created.first())
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to create civic update on Supabase, applying locally: ${e.message}")
            }
        }
        val ward = SeedData.wards.find { it.id == payload.wardId }
        val newUpdate = CivicUpdate(
            id = java.util.UUID.randomUUID().toString(),
            title = payload.title,
            description = payload.description,
            category = payload.category ?: "General",
            priority = payload.priority,
            wardId = payload.wardId,
            publishedAt = payload.publishedAt ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date()),
            expiresAt = payload.expiresAt,
            isActive = payload.isActive,
            createdBy = payload.createdBy,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date()),
            ward = ward
        )
        synchronized(localCivicUpdates) {
            localCivicUpdates.add(0, newUpdate)
        }
        Result.success(newUpdate)
    }

    suspend fun updateCivicUpdate(id: String, payload: Map<String, Any?>): Result<CivicUpdate> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                val updated = api.updateCivicUpdate("eq.$id", payload)
                if (updated.isNotEmpty()) {
                    return@withContext Result.success(updated.first())
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to update civic update on Supabase, applying locally: ${e.message}")
            }
        }
        synchronized(localCivicUpdates) {
            val index = localCivicUpdates.indexOfFirst { it.id == id }
            if (index != -1) {
                val existing = localCivicUpdates[index]
                val wardId = if (payload.containsKey("ward_id")) payload["ward_id"] as? String else existing.wardId
                val ward = SeedData.wards.find { it.id == wardId }
                val modified = existing.copy(
                    title = (payload["title"] as? String) ?: existing.title,
                    description = if (payload.containsKey("description")) payload["description"] as? String else existing.description,
                    category = (payload["category"] as? String) ?: existing.category,
                    priority = (payload["priority"] as? String) ?: existing.priority,
                    wardId = wardId,
                    publishedAt = (payload["published_at"] as? String) ?: existing.publishedAt,
                    expiresAt = if (payload.containsKey("expires_at")) payload["expires_at"] as? String else existing.expiresAt,
                    isActive = (payload["is_active"] as? Boolean) ?: existing.isActive,
                    ward = ward
                )
                localCivicUpdates[index] = modified
                return@withContext Result.success(modified)
            }
        }
        Result.failure(Exception("Civic update not found"))
    }

    suspend fun deleteCivicUpdate(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (api != null) {
            try {
                api.deleteCivicUpdate("eq.$id")
            } catch (e: Exception) {
                Log.w(tag, "Failed to delete civic update on Supabase, deleting locally: ${e.message}")
            }
        }
        synchronized(localCivicUpdates) {
            localCivicUpdates.removeAll { it.id == id }
        }
        Result.success(Unit)
    }

    fun getApi(): SupabaseApi? = api
}

object SeedData {
    val teams = listOf(
        com.example.zeromile.data.model.Team(
            id = "t1111111-1111-1111-1111-111111111111",
            name = "Civic Enforcement Team",
            departmentId = "d1111111-1111-1111-1111-111111111111",
            description = "Noise control, anti-encroachment, and municipal regulations compliance",
            active = true
        ),
        com.example.zeromile.data.model.Team(
            id = "t2222222-2222-2222-2222-222222222222",
            name = "Zone Road Repair Squad",
            departmentId = "d4444444-4444-4444-4444-444444444444",
            description = "Asphalt pothole repair, road surface patching, and curb restoration",
            active = true
        ),
        com.example.zeromile.data.model.Team(
            id = "t3333333-3333-3333-3333-333333333333",
            name = "Electrical Maintenance Section",
            departmentId = "d1111111-1111-1111-1111-111111111111",
            description = "Streetlight repair, luminaire replacement, and junction pole safety",
            active = true
        ),
        com.example.zeromile.data.model.Team(
            id = "t4444444-4444-4444-4444-444444444444",
            name = "Conservancy Division",
            departmentId = "d2222222-2222-2222-2222-222222222222",
            description = "Secondary waste clearing, market area sanitation, and dump truck dispatch",
            active = true
        ),
        com.example.zeromile.data.model.Team(
            id = "t5555555-5555-5555-5555-555555555555",
            name = "Water Pipeline Squad",
            departmentId = "d3333333-3333-3333-3333-333333333333",
            description = "Water pipeline repair, pressure diagnostics, and emergency tanker coordination",
            active = true
        )
    )
    val departments = listOf(
        Department("d1111111-1111-1111-1111-111111111111", "Municipal Corporation", "Nagpur Municipal Corporation central administration and general civic affairs"),
        Department("d2222222-2222-2222-2222-222222222222", "Waste Management", "Solid waste collection, street cleaning, sanitation and dumping operations"),
        Department("d3333333-3333-3333-3333-333333333333", "Water", "Drinking water supply, pipeline maintenance, tanker allocation and drainage"),
        Department("d4444444-4444-4444-4444-444444444444", "Roads", "Road construction, pothole repairs, footpaths, bridges and infrastructure"),
        Department("d5555555-5555-5555-5555-555555555555", "Traffic", "Traffic signal management, parking regulations, signages and congestion monitoring"),
        Department("d6666666-6666-6666-6666-666666666666", "Pollution", "Air quality, noise control compliance, industrial emissions and environmental protection"),
        Department("d7777777-7777-7777-7777-777777777777", "Food Safety", "Hygiene inspections, food vendor licenses and public health safety standards"),
        Department("d8888888-8888-8888-8888-888888888888", "Emergency Services", "Fire rescue, disaster management, rapid civic response and fallen trees removal")
    )

    val wards = listOf(
        Ward("w1111111-1111-1111-1111-111111111111", 32, "Dharampeth", "Nagpur"),
        Ward("w2222222-2222-2222-2222-222222222222", 12, "Sitabuldi", "Nagpur"),
        Ward("w3333333-3333-3333-3333-333333333333", 18, "Ramdaspeth", "Nagpur"),
        Ward("w4444444-4444-4444-4444-444444444444", 25, "Civil Lines", "Nagpur"),
        Ward("w5555555-5555-5555-5555-555555555555", 44, "Sadar", "Nagpur"),
        Ward("w6666666-6666-6666-6666-666666666666", 58, "Manish Nagar", "Nagpur")
    )

    val categories = listOf(
        ServiceCategory("c1111111-1111-1111-1111-111111111111", "Road", "Potholes, resurfacing, streetlights, pavements, and dividers", "road"),
        ServiceCategory("c2222222-2222-2222-2222-222222222222", "Traffic", "Signals, illegal parking, lane marking, and signboards", "traffic"),
        ServiceCategory("c3333333-3333-3333-3333-333333333333", "Garbage", "Door-to-door collection, street sweeping, and overflowing bins", "delete"),
        ServiceCategory("c4444444-4444-4444-4444-444444444444", "Water", "Pipeline bursts, water contamination, low pressure, tanker supply", "water_drop"),
        ServiceCategory("c5555555-5555-5555-5555-555555555555", "Pollution", "Loud noise, factory smoke, dust emissions, open burning", "air"),
        ServiceCategory("c6666666-6666-6666-6666-666666666666", "Food", "Restaurant hygiene, adulteration, street vendor food safety", "restaurant"),
        ServiceCategory("c7777777-7777-7777-7777-777777777777", "Certificates", "Birth, death, marriage, trade certificates and municipal licenses", "description"),
        ServiceCategory("c8888888-8888-8888-8888-888888888888", "Housing", "Property tax queries, unauthorized construction, and encroachment", "home"),
        ServiceCategory("c9999999-9999-9999-9999-999999999999", "Recycling", "E-waste disposal, plastic collection drives, composting units", "recycling"),
        ServiceCategory("caaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "Emergency", "Disaster rescue, fallen trees, flooding waterlogging, fire hazards", "warning"),
        ServiceCategory("cbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "Other", "General municipal feedback, community halls, public gardens", "more_horiz")
    )

    val services = listOf(
        CivicService(
            id = "s1111111-1111-1111-1111-111111111111",
            categoryId = "c5555555-5555-5555-5555-555555555555",
            departmentId = "d1111111-1111-1111-1111-111111111111",
            name = "Noise Pollution",
            description = "Report loud music, illegal loudspeakers, commercial generator noise beyond permissible decibels in residential areas",
            serviceType = "complaint",
            isActive = true,
            category = categories.find { it.id == "c5555555-5555-5555-5555-555555555555" },
            department = departments.find { it.id == "d1111111-1111-1111-1111-111111111111" }
        ),
        CivicService(
            id = "s2222222-2222-2222-2222-222222222222",
            categoryId = "c1111111-1111-1111-1111-111111111111",
            departmentId = "d4444444-4444-4444-4444-444444444444",
            name = "Pothole Complaint",
            description = "Report dangerous road depressions, potholes, broken tar, or uneven surface risking two-wheeler accidents",
            serviceType = "complaint",
            isActive = true,
            category = categories.find { it.id == "c1111111-1111-1111-1111-111111111111" },
            department = departments.find { it.id == "d4444444-4444-4444-4444-444444444444" }
        ),
        CivicService(
            id = "s3333333-3333-3333-3333-333333333333",
            categoryId = "c3333333-3333-3333-3333-333333333333",
            departmentId = "d2222222-2222-2222-2222-222222222222",
            name = "Garbage Collection",
            description = "Request waste cleanup for overflowing community bins, skipped daily door-to-door pickup, or illegal street dumps",
            serviceType = "complaint",
            isActive = true,
            category = categories.find { it.id == "c3333333-3333-3333-3333-333333333333" },
            department = departments.find { it.id == "d2222222-2222-2222-2222-222222222222" }
        ),
        CivicService(
            id = "s4444444-4444-4444-4444-444444444444",
            categoryId = "c4444444-4444-4444-4444-444444444444",
            departmentId = "d3333333-3333-3333-3333-333333333333",
            name = "Water Tanker Complaint",
            description = "Report delays, quality issues, or irregular scheduling for municipal emergency drinking water tanker deliveries",
            serviceType = "complaint",
            isActive = true,
            category = categories.find { it.id == "c4444444-4444-4444-4444-444444444444" },
            department = departments.find { it.id == "d3333333-3333-3333-3333-333333333333" }
        ),
        CivicService(
            id = "s5555555-5555-5555-5555-555555555555",
            categoryId = "c7777777-7777-7777-7777-777777777777",
            departmentId = "d1111111-1111-1111-1111-111111111111",
            name = "Birth Certificate",
            description = "Apply for new birth registration extract or request corrections in existing civic registry records",
            serviceType = "service",
            isActive = true,
            category = categories.find { it.id == "c7777777-7777-7777-7777-777777777777" },
            department = departments.find { it.id == "d1111111-1111-1111-1111-111111111111" }
        ),
        CivicService(
            id = "s6666666-6666-6666-6666-666666666666",
            categoryId = "c6666666-6666-6666-6666-666666666666",
            departmentId = "d7777777-7777-7777-7777-777777777777",
            name = "Food Safety Complaint",
            description = "Report unhygienic eateries, food adulteration, stale ingredients, or pest infestation in food businesses",
            serviceType = "complaint",
            isActive = true,
            category = categories.find { it.id == "c6666666-6666-6666-6666-666666666666" },
            department = departments.find { it.id == "d7777777-7777-7777-7777-777777777777" }
        ),
        CivicService(
            id = "s7777777-7777-7777-7777-777777777777",
            categoryId = "c1111111-1111-1111-1111-111111111111",
            departmentId = "d4444444-4444-4444-4444-444444444444",
            name = "Road Maintenance",
            description = "Request asphalt recarpeting, curb repair, median beautification, and footpath tiling restoration",
            serviceType = "complaint",
            isActive = true,
            category = categories.find { it.id == "c1111111-1111-1111-1111-111111111111" },
            department = departments.find { it.id == "d4444444-4444-4444-4444-444444444444" }
        ),
        CivicService(
            id = "s8888888-8888-8888-8888-888888888888",
            categoryId = "c2222222-2222-2222-2222-222222222222",
            departmentId = "d5555555-5555-5555-5555-555555555555",
            name = "Traffic Complaint",
            description = "Report non-functioning traffic signal timers, hazardous turning points, or obstructed pedestrian zebra crossings",
            serviceType = "complaint",
            isActive = true,
            category = categories.find { it.id == "c2222222-2222-2222-2222-222222222222" },
            department = departments.find { it.id == "d5555555-5555-5555-5555-555555555555" }
        ),
        CivicService(
            id = "s9999999-9999-9999-9999-999999999999",
            categoryId = "c4444444-4444-4444-4444-444444444444",
            departmentId = "d3333333-3333-3333-3333-333333333333",
            name = "Sewage Overflow",
            description = "Report blocked municipal drainage lines, backflow in residential areas, or broken manhole covers",
            serviceType = "complaint",
            isActive = true,
            category = categories.find { it.id == "c4444444-4444-4444-4444-444444444444" },
            department = departments.find { it.id == "d3333333-3333-3333-3333-333333333333" }
        ),
        CivicService(
            id = "saaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            categoryId = "caaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            departmentId = "d8888888-8888-8888-8888-888888888888",
            name = "Emergency Services",
            description = "Rapid municipal assistance for fallen trees blocking main thoroughfares, flash waterlogging, and safety hazards",
            serviceType = "emergency",
            isActive = true,
            category = categories.find { it.id == "caaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" },
            department = departments.find { it.id == "d8888888-8888-8888-8888-888888888888" }
        )
    )

    val defaultProfile = Profile(
        id = "p1111111-1111-1111-1111-111111111111",
        userId = "demo-citizen-nagpur",
        fullName = "Nagpur Citizen",
        phone = "+91 98230 12345",
        city = "Nagpur",
        preferredLanguage = "en",
        role = "citizen"
    )

    val initialComplaints = listOf(
        com.example.zeromile.data.model.ComplaintRecord(
            id = "b2222222-2222-2222-2222-222222222222",
            complaintNumber = "NMC-2026-001245",
            userId = "demo-citizen-nagpur",
            serviceId = "s1111111-1111-1111-1111-111111111111",
            categoryId = "c5555555-5555-5555-5555-555555555555",
            departmentId = "d1111111-1111-1111-1111-111111111111",
            description = "माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो. Heavy loudspeaker distortion past midnight near West High Court Road.",
            originalTranscript = "माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो.",
            inputMode = "voice",
            language = "mr-IN",
            locationText = "Dharampeth, Ward 32",
            wardId = "w1111111-1111-1111-1111-111111111111",
            latitude = 21.1436,
            longitude = 79.0688,
            locationAccuracyMeters = 12.5,
            locationSource = "gps",
            priority = "High",
            aiConfidence = 0.96,
            aiSummary = "Late-night commercial sound violation in residential Dharampeth",
            aiReason = "Resident reported recurring high decibel loudspeaker activity violating silent hours.",
            status = "In Progress",
            createdAt = "2026-09-19T02:30:00Z",
            service = services.find { it.id == "s1111111-1111-1111-1111-111111111111" },
            department = departments.find { it.id == "d1111111-1111-1111-1111-111111111111" },
            category = categories.find { it.id == "c5555555-5555-5555-5555-555555555555" },
            ward = wards.find { it.id == "w1111111-1111-1111-1111-111111111111" }
        ),
        com.example.zeromile.data.model.ComplaintRecord(
            id = "b3333333-3333-3333-3333-333333333333",
            complaintNumber = "NMC-2026-001246",
            userId = "demo-citizen-nagpur",
            serviceId = "s2222222-2222-2222-2222-222222222222",
            categoryId = "c1111111-1111-1111-1111-111111111111",
            departmentId = "d4444444-4444-4444-4444-444444444444",
            description = "Deep tire-damaging pothole cluster near VNIT Gate road bend causing severe two-wheeler skidding hazards.",
            originalTranscript = "VNIT gate samore mothe khadde padlet, gadi phisleli.",
            inputMode = "voice",
            language = "mr-IN",
            locationText = "South Ambazari Road, Ward 38",
            wardId = "w3333333-3333-3333-3333-333333333333",
            latitude = 21.1245,
            longitude = 79.0512,
            locationAccuracyMeters = 8.0,
            locationSource = "gps",
            priority = "Urgent",
            aiConfidence = 0.94,
            aiSummary = "Hazardous pothole cluster near university transit corridor",
            aiReason = "Traffic safety risk verified on arterial road.",
            status = "Assigned",
            createdAt = "2026-09-19T01:45:00Z",
            service = services.find { it.id == "s2222222-2222-2222-2222-222222222222" },
            department = departments.find { it.id == "d4444444-4444-4444-4444-444444444444" },
            category = categories.find { it.id == "c1111111-1111-1111-1111-111111111111" },
            ward = wards.find { it.id == "w3333333-3333-3333-3333-333333333333" }
        ),
        com.example.zeromile.data.model.ComplaintRecord(
            id = "b4444444-4444-4444-4444-444444444444",
            complaintNumber = "NMC-2026-001247",
            userId = "demo-citizen-nagpur",
            serviceId = "s4444444-4444-4444-4444-444444444444",
            categoryId = "c3333333-3333-3333-3333-333333333333",
            departmentId = "d2222222-2222-2222-2222-222222222222",
            description = "Streetlight pole #42 flickering and non-operational for 3 nights, dark corner creating safety concerns.",
            originalTranscript = "Street light band aahe 3 divas zale.",
            inputMode = "voice",
            language = "mr-IN",
            locationText = "Medical Square, Ward 24",
            wardId = "w2222222-2222-2222-2222-222222222222",
            latitude = 21.1310,
            longitude = 79.0980,
            locationAccuracyMeters = 15.0,
            locationSource = "gps",
            priority = "Medium",
            aiConfidence = 0.92,
            aiSummary = "Dark street illumination failure at hospital junction",
            aiReason = "Night-time pedestrian safety issue.",
            status = "Resolved",
            createdAt = "2026-09-18T18:00:00Z",
            service = services.find { it.id == "s4444444-4444-4444-4444-444444444444" },
            department = departments.find { it.id == "d2222222-2222-2222-2222-222222222222" },
            category = categories.find { it.id == "c3333333-3333-3333-3333-333333333333" },
            ward = wards.find { it.id == "w2222222-2222-2222-2222-222222222222" }
        ),
        com.example.zeromile.data.model.ComplaintRecord(
            id = "b5555555-5555-5555-5555-555555555555",
            complaintNumber = "NMC-2026-001248",
            userId = "demo-citizen-nagpur",
            serviceId = "s3333333-3333-3333-3333-333333333333",
            categoryId = "c4444444-4444-4444-4444-444444444444",
            departmentId = "d3333333-3333-3333-3333-333333333333",
            description = "Overflowing solid waste container near community vegetable market attracting stray cattle and bad odor.",
            originalTranscript = "Kachra peti bharun vahat ahe bazarachya bajula.",
            inputMode = "voice",
            language = "mr-IN",
            locationText = "Gokulpeth Market, Ward 32",
            wardId = "w1111111-1111-1111-1111-111111111111",
            latitude = 21.1415,
            longitude = 79.0620,
            locationAccuracyMeters = 10.0,
            locationSource = "gps",
            priority = "High",
            aiConfidence = 0.95,
            aiSummary = "Sanitation overflow at commercial daily market",
            aiReason = "Public health and hygiene concern.",
            status = "In Progress",
            createdAt = "2026-09-18T20:10:00Z",
            service = services.find { it.id == "s3333333-3333-3333-3333-333333333333" },
            department = departments.find { it.id == "d3333333-3333-3333-3333-333333333333" },
            category = categories.find { it.id == "c4444444-4444-4444-4444-444444444444" },
            ward = wards.find { it.id == "w1111111-1111-1111-1111-111111111111" }
        ),
        com.example.zeromile.data.model.ComplaintRecord(
            id = "b6666666-6666-6666-6666-666666666666",
            complaintNumber = "NMC-2026-001249",
            userId = "demo-citizen-nagpur",
            serviceId = "s5555555-5555-5555-5555-555555555555",
            categoryId = "c4444444-4444-4444-4444-444444444444",
            departmentId = "d3333333-3333-3333-3333-333333333333",
            description = "Low water supply pressure during morning schedule and muddy water received in household connection.",
            originalTranscript = "Panyacha dab kami ahe ani ghadul pani yet ahe.",
            inputMode = "voice",
            language = "mr-IN",
            locationText = "Sitabuldi Main Road, Ward 24",
            wardId = "w2222222-2222-2222-2222-222222222222",
            latitude = 21.1480,
            longitude = 79.0820,
            locationAccuracyMeters = 20.0,
            locationSource = "address",
            priority = "Medium",
            aiConfidence = 0.91,
            aiSummary = "Turbid municipal drinking water pipeline issue",
            aiReason = "Potable water pipeline contamination risk.",
            status = "Submitted",
            createdAt = "2026-09-19T03:00:00Z",
            service = services.find { it.id == "s5555555-5555-5555-5555-555555555555" },
            department = departments.find { it.id == "d3333333-3333-3333-3333-333333333333" },
            category = categories.find { it.id == "c4444444-4444-4444-4444-444444444444" },
            ward = wards.find { it.id == "w2222222-2222-2222-2222-222222222222" }
        )
    )

    val initialEvidence = listOf(
        com.example.zeromile.data.model.ComplaintEvidenceRecord(
            id = "e2222222-2222-2222-2222-222222222221",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            uploadedBy = "demo-citizen-nagpur",
            storagePath = "demo-citizen-nagpur/b2222222-2222-2222-2222-222222222222/dharampeth_noise_dj_setup.jpg",
            fileName = "dharampeth_noise_dj_setup.jpg",
            mimeType = "image/jpeg",
            fileSize = 1420500L,
            createdAt = "2026-09-19T02:32:00Z",
            signedUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&auto=format&fit=crop&q=80"
        ),
        com.example.zeromile.data.model.ComplaintEvidenceRecord(
            id = "e3333333-3333-3333-3333-333333333331",
            complaintId = "b3333333-3333-3333-3333-333333333333",
            uploadedBy = "demo-citizen-nagpur",
            storagePath = "demo-citizen-nagpur/b3333333-3333-3333-3333-333333333333/vnit_road_pothole.jpg",
            fileName = "vnit_road_pothole.jpg",
            mimeType = "image/jpeg",
            fileSize = 2100400L,
            createdAt = "2026-09-19T01:47:00Z",
            signedUrl = "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=800&auto=format&fit=crop&q=80"
        ),
        com.example.zeromile.data.model.ComplaintEvidenceRecord(
            id = "e4444444-4444-4444-4444-444444444441",
            complaintId = "b4444444-4444-4444-4444-444444444444",
            uploadedBy = "demo-citizen-nagpur",
            storagePath = "demo-citizen-nagpur/b4444444-4444-4444-4444-444444444444/medical_sq_streetlight_pole42.jpg",
            fileName = "medical_sq_streetlight_pole42.jpg",
            mimeType = "image/jpeg",
            fileSize = 980200L,
            createdAt = "2026-09-18T18:05:00Z",
            signedUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=80"
        ),
        com.example.zeromile.data.model.ComplaintEvidenceRecord(
            id = "e5555555-5555-5555-5555-555555555551",
            complaintId = "b5555555-5555-5555-5555-555555555555",
            uploadedBy = "demo-citizen-nagpur",
            storagePath = "demo-citizen-nagpur/b5555555-5555-5555-5555-555555555555/gokulpeth_overflowing_bin.jpg",
            fileName = "gokulpeth_overflowing_bin.jpg",
            mimeType = "image/jpeg",
            fileSize = 1650300L,
            createdAt = "2026-09-18T20:12:00Z",
            signedUrl = "https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?w=800&auto=format&fit=crop&q=80"
        )
    )

    val initialStatusHistory = listOf(
        // For NMC-2026-001245 (Submitted -> Assigned -> In Progress)
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h2222222-2222-2222-2222-222222222221",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            status = "Submitted",
            note = "Citizen filed voice grievance via Zeromile Connect.",
            assignedTeam = null,
            changedBy = "demo-citizen-nagpur",
            createdAt = "2026-09-19T02:30:00Z"
        ),
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h2222222-2222-2222-2222-222222222222",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            status = "Assigned",
            note = "Assigned to Dharampeth Zone Sanitary & Noise Enforcement Unit.",
            assignedTeam = "Civic Enforcement Team",
            changedBy = null,
            createdAt = "2026-09-19T02:45:00Z"
        ),
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h2222222-2222-2222-2222-222222222223",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            status = "In Progress",
            note = "Nodal officer visited site for decibel audit and issued notice to commercial venue.",
            assignedTeam = "Civic Enforcement Team",
            changedBy = null,
            createdAt = "2026-09-19T03:00:00Z"
        ),

        // For NMC-2026-001246 (Pothole: Submitted -> Assigned)
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h3333333-3333-3333-3333-333333333331",
            complaintId = "b3333333-3333-3333-3333-333333333333",
            status = "Submitted",
            note = "Urgent road safety complaint logged.",
            assignedTeam = null,
            changedBy = "demo-citizen-nagpur",
            createdAt = "2026-09-19T01:45:00Z"
        ),
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h3333333-3333-3333-3333-333333333332",
            complaintId = "b3333333-3333-3333-3333-333333333333",
            status = "Assigned",
            note = "Transferred to West Zone Asphalt Patching Division.",
            assignedTeam = "Zone Road Repair Squad",
            changedBy = null,
            createdAt = "2026-09-19T02:00:00Z"
        ),

        // For NMC-2026-001247 (Streetlight: Submitted -> Assigned -> In Progress -> Resolved)
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h4444444-4444-4444-4444-444444444441",
            complaintId = "b4444444-4444-4444-4444-444444444444",
            status = "Submitted",
            note = "Reported via voice intake.",
            assignedTeam = null,
            changedBy = "demo-citizen-nagpur",
            createdAt = "2026-09-18T18:00:00Z"
        ),
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h4444444-4444-4444-4444-444444444442",
            complaintId = "b4444444-4444-4444-4444-444444444444",
            status = "Assigned",
            note = "Dispatched to Electrical Maintenance Section.",
            assignedTeam = "Electrical Maintenance Section",
            changedBy = null,
            createdAt = "2026-09-18T19:30:00Z"
        ),
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h4444444-4444-4444-4444-444444444443",
            complaintId = "b4444444-4444-4444-4444-444444444444",
            status = "In Progress",
            note = "Electrician crew on-site inspecting pole and wiring.",
            assignedTeam = "Electrical Maintenance Section",
            changedBy = null,
            createdAt = "2026-09-18T21:00:00Z"
        ),
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h4444444-4444-4444-4444-444444444444",
            complaintId = "b4444444-4444-4444-4444-444444444444",
            status = "Resolved",
            note = "Faulty LED luminaire and junction circuit replaced. Street illumination restored.",
            assignedTeam = "Electrical Maintenance Section",
            changedBy = null,
            createdAt = "2026-09-18T22:30:00Z"
        ),

        // For NMC-2026-001248 (Garbage: Submitted -> Assigned -> In Progress)
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h5555555-5555-5555-5555-555555555551",
            complaintId = "b5555555-5555-5555-5555-555555555555",
            status = "Submitted",
            note = "Sanitation request registered.",
            assignedTeam = null,
            changedBy = "demo-citizen-nagpur",
            createdAt = "2026-09-18T20:10:00Z"
        ),
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h5555555-5555-5555-5555-555555555552",
            complaintId = "b5555555-5555-5555-5555-555555555555",
            status = "Assigned",
            note = "Assigned to Dharampeth Conservancy Ward Crew.",
            assignedTeam = "Conservancy Division",
            changedBy = null,
            createdAt = "2026-09-18T21:30:00Z"
        ),
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h5555555-5555-5555-5555-555555555553",
            complaintId = "b5555555-5555-5555-5555-555555555555",
            status = "In Progress",
            note = "Tipper truck and loader dispatched for secondary clearing.",
            assignedTeam = "Conservancy Division",
            changedBy = null,
            createdAt = "2026-09-19T01:00:00Z"
        ),

        // For NMC-2026-001249 (Water: Submitted)
        com.example.zeromile.data.model.ComplaintStatusHistoryRecord(
            id = "h6666666-6666-6666-6666-666666666661",
            complaintId = "b6666666-6666-6666-6666-666666666666",
            status = "Submitted",
            note = "Low pressure and turbidity complaint registered.",
            assignedTeam = null,
            changedBy = "demo-citizen-nagpur",
            createdAt = "2026-09-19T03:00:00Z"
        )
    )

    val initialComplaintUpdates = listOf(
        com.example.zeromile.data.model.ComplaintUpdateRecord(
            id = "u2222222-2222-2222-2222-222222222221",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            message = "Civic Enforcement Team has been assigned to review the complaint.",
            officialName = "Nagpur Municipal Corporation",
            createdAt = "2026-09-19T02:45:00Z"
        ),
        com.example.zeromile.data.model.ComplaintUpdateRecord(
            id = "u2222222-2222-2222-2222-222222222222",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            message = "Sanitary inspector conducted on-site decibel assessment and issued statutory regulation notice.",
            officialName = "Dharampeth Zone Office",
            createdAt = "2026-09-19T03:05:00Z"
        ),
        com.example.zeromile.data.model.ComplaintUpdateRecord(
            id = "u4444444-4444-4444-4444-444444444441",
            complaintId = "b4444444-4444-4444-4444-444444444444",
            message = "Driver module replaced. Luminaire lighting test passed successfully.",
            officialName = "NMC Electrical Dept",
            createdAt = "2026-09-18T22:30:00Z"
        )
    )

    val initialNotifications = listOf(
        NotificationRecord(
            id = "n1111111-1111-1111-1111-111111111111",
            userId = "demo-citizen-nagpur",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            type = "assignment",
            title = "Complaint Assigned",
            message = "Your complaint NMC-2026-001245 has been assigned to the Civic Enforcement Team.",
            read = false,
            createdAt = "2026-09-19T02:45:00Z",
            idempotencyKey = "b2222222-2222-2222-2222-222222222222_assignment_Civic Enforcement Team"
        ),
        NotificationRecord(
            id = "n2222222-2222-2222-2222-222222222222",
            userId = "demo-citizen-nagpur",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            type = "status_update",
            title = "Complaint Status Updated",
            message = "Your complaint NMC-2026-001245 is now In Progress.",
            read = false,
            createdAt = "2026-09-19T02:50:00Z",
            idempotencyKey = "b2222222-2222-2222-2222-222222222222_status_InProgress"
        ),
        NotificationRecord(
            id = "n3333333-3333-3333-3333-333333333333",
            userId = "demo-citizen-nagpur",
            complaintId = "b2222222-2222-2222-2222-222222222222",
            type = "official_update",
            title = "New Municipal Update",
            message = "A new official update has been added to your complaint NMC-2026-001245.",
            read = true,
            createdAt = "2026-09-19T03:05:00Z",
            idempotencyKey = "b2222222-2222-2222-2222-222222222222_official_update_u2222222-2222-2222-2222-222222222222"
        )
    )

    val initialEmergencyServices = listOf(
        EmergencyService(
            id = "e1111111-1111-1111-1111-111111111111",
            name = "National Emergency Helpline",
            description = "Unified national emergency helpline for all police, fire, and medical crises across India",
            phoneNumber = "112",
            alternatePhone = "100",
            icon = "shield",
            isActive = true,
            sortOrder = 1
        ),
        EmergencyService(
            id = "e2222222-2222-2222-2222-222222222222",
            name = "NMC 24x7 Disaster & Civic Control Room",
            description = "Nagpur Municipal Corporation central control room for waterlogging, fallen trees, building collapse, and flash flooding",
            phoneNumber = "0712-2567030",
            alternatePhone = "1800-233-3764",
            icon = "emergency",
            isActive = true,
            sortOrder = 2
        ),
        EmergencyService(
            id = "e3333333-3333-3333-3333-333333333333",
            name = "Fire & Emergency Rescue Services",
            description = "Nagpur Municipal Fire Brigade stations across Civil Lines, Ganjipeth, Narendra Nagar, and Kalamna",
            phoneNumber = "101",
            alternatePhone = "0712-2567777",
            icon = "local_fire_department",
            isActive = true,
            sortOrder = 3
        ),
        EmergencyService(
            id = "e4444444-4444-4444-4444-444444444444",
            name = "Emergency Medical Ambulance (EMS)",
            description = "Maharashtra State 108 Emergency Medical Service with advanced life support ambulances",
            phoneNumber = "108",
            alternatePhone = "102",
            icon = "medical_services",
            isActive = true,
            sortOrder = 4
        ),
        EmergencyService(
            id = "e5555555-5555-5555-5555-555555555555",
            name = "Nagpur City Police Control Room",
            description = "Rapid police dispatch, night patrolling response, and highway safety across Nagpur commissionerate",
            phoneNumber = "100",
            alternatePhone = "0712-2561222",
            icon = "local_police",
            isActive = true,
            sortOrder = 5
        ),
        EmergencyService(
            id = "e6666666-6666-6666-6666-666666666666",
            name = "Women in Distress Helpline",
            description = "24x7 toll-free crisis response, legal assistance, and safety transit for women in Maharashtra",
            phoneNumber = "1091",
            alternatePhone = "181",
            icon = "female",
            isActive = true,
            sortOrder = 6
        ),
        EmergencyService(
            id = "e7777777-7777-7777-7777-777777777777",
            name = "Childline Emergency Care",
            description = "National emergency 24-hour phone outreach service for children in need of care and protection",
            phoneNumber = "1098",
            alternatePhone = null,
            icon = "child_care",
            isActive = true,
            sortOrder = 7
        ),
        EmergencyService(
            id = "e8888888-8888-8888-8888-888888888888",
            name = "Disaster Management Helpline (DDMA)",
            description = "Nagpur District Disaster Management Authority flood, storm, and chemical hazard response",
            phoneNumber = "1077",
            alternatePhone = "0712-2562668",
            icon = "warning",
            isActive = true,
            sortOrder = 8
        )
    )

    val initialCivicUpdates = listOf(
        CivicUpdate(
            id = "u1111111-1111-1111-1111-111111111111",
            title = "Scheduled Water Supply Maintenance in Dharampeth & West Nagpur",
            description = "NMC Water Works Department will conduct scheduled feeder pipeline interconnection at Gorewada Water Treatment Plant on Thursday from 08:00 AM to 06:00 PM. Water supply with low pressure will resume by 08:00 PM.",
            category = "Water Supply",
            priority = "high",
            wardId = "w1111111-1111-1111-1111-111111111111",
            publishedAt = "2026-09-19T04:00:00Z",
            expiresAt = "2026-09-22T18:00:00Z",
            isActive = true,
            ward = wards.find { it.id == "w1111111-1111-1111-1111-111111111111" }
        ),
        CivicUpdate(
            id = "u2222222-2222-2222-2222-222222222222",
            title = "Monsoon Road Repair & Asphalt Pothole Campaign on Wardha Road",
            description = "NMC Public Works Department has deployed jetpatcher machines along Wardha Road and Outer Ring Road junctions. Commuters are advised to expect single-lane traffic between 11:00 PM and 05:00 AM.",
            category = "Road Works",
            priority = "normal",
            wardId = null,
            publishedAt = "2026-09-18T10:00:00Z",
            expiresAt = "2026-09-25T18:00:00Z",
            isActive = true,
            ward = null
        ),
        CivicUpdate(
            id = "u3333333-3333-3333-3333-333333333333",
            title = "Urgent Alert: Zero Mile Metro Precinct Traffic Diversion",
            description = "Temporary vehicular diversion implemented around Zero Mile Stone monument for flyover expansion. Heavy goods vehicles diverted via Central Avenue and Great Nag Road.",
            category = "Traffic Alert",
            priority = "urgent",
            wardId = "w2222222-2222-2222-2222-222222222222",
            publishedAt = "2026-09-19T06:00:00Z",
            expiresAt = "2026-09-21T23:59:59Z",
            isActive = true,
            ward = wards.find { it.id == "w2222222-2222-2222-2222-222222222222" }
        ),
        CivicUpdate(
            id = "u4444444-4444-4444-4444-444444444444",
            title = "Special E-Waste Collection & Segregated Waste Drive this Weekend",
            description = "Nagpur Municipal Corporation solid waste division is setting up free recycling kiosks across Dharampeth, Ramdaspeth, and Sitabuldi public gardens this Saturday 9 AM - 4 PM.",
            category = "Sanitation",
            priority = "low",
            wardId = null,
            publishedAt = "2026-09-19T02:00:00Z",
            expiresAt = "2026-09-24T18:00:00Z",
            isActive = true,
            ward = null
        )
    )
}
