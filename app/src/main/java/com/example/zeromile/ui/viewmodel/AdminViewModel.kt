package com.example.zeromile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromile.data.model.AdminAuditLogRecord
import com.example.zeromile.data.model.AdminComplaintFilter
import com.example.zeromile.data.model.AdminMetrics
import com.example.zeromile.data.model.AdminSortBy
import com.example.zeromile.data.model.AdminUser
import com.example.zeromile.data.model.CivicService
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.data.model.ComplaintTrackingDetails
import com.example.zeromile.data.model.DateRangeFilter
import com.example.zeromile.data.model.PagedResult
import com.example.zeromile.data.model.Team
import com.example.zeromile.data.repository.AdminRepository
import com.example.zeromile.data.repository.CivicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminSection(val displayName: String) {
    DASHBOARD("Dashboard"),
    COMPLAINTS("Complaints"),
    COMPLAINT_DETAIL("Complaint Details"),
    TEAMS("Teams"),
    SERVICES("Services"),
    UPDATES("Civic Updates"),
    AUDIT_LOGS("Audit Logs")
}

data class AdminUiState(
    val adminUser: AdminUser? = null,
    val isAuthenticated: Boolean = false,
    val isAuthenticating: Boolean = false,
    val authError: String? = null,
    val currentSection: AdminSection = AdminSection.DASHBOARD,
    val metricsState: UiState<AdminMetrics> = UiState.Loading,
    val complaintsState: UiState<PagedResult<ComplaintRecord>> = UiState.Loading,
    val complaintFilter: AdminComplaintFilter = AdminComplaintFilter(),
    val selectedComplaintId: String? = null,
    val complaintDetailState: UiState<ComplaintTrackingDetails> = UiState.Loading,
    val teamsState: UiState<List<Team>> = UiState.Loading,
    val servicesState: UiState<List<CivicService>> = UiState.Loading,
    val auditLogsState: UiState<List<AdminAuditLogRecord>> = UiState.Loading,
    // Dialog and Action states
    val showAssignTeamDialog: Boolean = false,
    val showStatusUpdateDialog: Boolean = false,
    val showAddUpdateDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showProfileDialog: Boolean = false,
    val actionInProgress: Boolean = false,
    val actionFeedbackMessage: String? = null,
    val actionErrorMessage: String? = null
)

class AdminViewModel(
    private val civicRepository: CivicRepository = CivicRepository(),
    private val adminRepository: AdminRepository = AdminRepository(civicRepository)
) : ViewModel() {

    val adminRepo: AdminRepository get() = adminRepository

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, authError = null) }
            val result = adminRepository.adminSignIn(email, password)
            result.onSuccess { admin ->
                _uiState.update {
                    it.copy(
                        adminUser = admin,
                        isAuthenticated = true,
                        isAuthenticating = false,
                        authError = null,
                        currentSection = AdminSection.DASHBOARD
                    )
                }
                loadDashboardData()
                loadComplaints()
                loadTeams()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = error.message ?: "Admin authentication failed. Admin access required."
                    )
                }
            }
        }
    }

    fun signOut() {
        adminRepository.adminSignOut()
        _uiState.update {
            AdminUiState(
                adminUser = null,
                isAuthenticated = false,
                currentSection = AdminSection.DASHBOARD
            )
        }
    }

    fun setSection(section: AdminSection) {
        _uiState.update { it.copy(currentSection = section) }
        when (section) {
            AdminSection.DASHBOARD -> loadDashboardData()
            AdminSection.COMPLAINTS -> loadComplaints()
            AdminSection.TEAMS -> loadTeams()
            AdminSection.SERVICES -> loadServices()
            AdminSection.UPDATES -> { /* Handled within AdminCivicUpdatesScreen */ }
            AdminSection.AUDIT_LOGS -> loadAuditLogs()
            AdminSection.COMPLAINT_DETAIL -> { /* handled by openComplaintDetail */ }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(metricsState = UiState.Loading) }
            val result = adminRepository.getAdminMetrics()
            result.onSuccess { metrics ->
                _uiState.update { it.copy(metricsState = UiState.Success(metrics)) }
            }.onFailure { err ->
                _uiState.update { it.copy(metricsState = UiState.Error(err.message ?: "Failed to load metrics")) }
            }
        }
    }

    fun loadComplaints() {
        viewModelScope.launch {
            _uiState.update { it.copy(complaintsState = UiState.Loading) }
            val filter = _uiState.value.complaintFilter
            val result = adminRepository.getAdminComplaints(filter)
            result.onSuccess { paged ->
                _uiState.update { it.copy(complaintsState = UiState.Success(paged)) }
            }.onFailure { err ->
                _uiState.update { it.copy(complaintsState = UiState.Error(err.message ?: "Failed to load complaints")) }
            }
        }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(complaintFilter = it.complaintFilter.copy(status = status, page = 1)) }
        loadComplaints()
    }

    fun setPriorityFilter(priority: String?) {
        _uiState.update { it.copy(complaintFilter = it.complaintFilter.copy(priority = priority, page = 1)) }
        loadComplaints()
    }

    fun setDepartmentFilter(deptId: String?) {
        _uiState.update { it.copy(complaintFilter = it.complaintFilter.copy(departmentId = deptId, page = 1)) }
        loadComplaints()
    }

    fun setWardFilter(wardId: String?) {
        _uiState.update { it.copy(complaintFilter = it.complaintFilter.copy(wardId = wardId, page = 1)) }
        loadComplaints()
    }

    fun setDateRangeFilter(range: DateRangeFilter) {
        _uiState.update { it.copy(complaintFilter = it.complaintFilter.copy(dateRange = range, page = 1)) }
        loadComplaints()
    }

    fun setSortBy(sort: AdminSortBy) {
        _uiState.update { it.copy(complaintFilter = it.complaintFilter.copy(sortBy = sort, page = 1)) }
        loadComplaints()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(complaintFilter = it.complaintFilter.copy(searchQuery = query, page = 1)) }
        loadComplaints()
    }

    fun setPage(page: Int) {
        _uiState.update { it.copy(complaintFilter = it.complaintFilter.copy(page = page)) }
        loadComplaints()
    }

    fun openComplaintDetail(complaintId: String) {
        _uiState.update {
            it.copy(
                selectedComplaintId = complaintId,
                currentSection = AdminSection.COMPLAINT_DETAIL,
                complaintDetailState = UiState.Loading
            )
        }
        loadComplaintDetail(complaintId)
    }

    fun closeComplaintDetail() {
        _uiState.update {
            it.copy(
                selectedComplaintId = null,
                currentSection = AdminSection.COMPLAINTS
            )
        }
        loadComplaints()
    }

    fun loadComplaintDetail(complaintId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(complaintDetailState = UiState.Loading) }
            val result = adminRepository.getAdminComplaintDetails(complaintId)
            result.onSuccess { details ->
                _uiState.update { it.copy(complaintDetailState = UiState.Success(details)) }
            }.onFailure { err ->
                _uiState.update { it.copy(complaintDetailState = UiState.Error(err.message ?: "Failed to load complaint details")) }
            }
        }
    }

    fun refreshCurrentDetail() {
        val id = _uiState.value.selectedComplaintId ?: return
        loadComplaintDetail(id)
    }

    // Modal Visibility Controls
    fun setAssignTeamDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAssignTeamDialog = visible, actionErrorMessage = null) }
    }

    fun setStatusUpdateDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showStatusUpdateDialog = visible, actionErrorMessage = null) }
    }

    fun setAddUpdateDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddUpdateDialog = visible, actionErrorMessage = null) }
    }

    fun setDeleteConfirmDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmDialog = visible, actionErrorMessage = null) }
    }

    fun setProfileDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showProfileDialog = visible) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(actionFeedbackMessage = null, actionErrorMessage = null) }
    }

    // Admin Operations
    fun assignTeam(teamId: String, teamName: String, note: String) {
        val complaintId = _uiState.value.selectedComplaintId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionErrorMessage = null) }
            val result = adminRepository.assignTeam(complaintId, teamId, teamName, note)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        showAssignTeamDialog = false,
                        actionFeedbackMessage = "Operational team '$teamName' assigned successfully."
                    )
                }
                loadComplaintDetail(complaintId)
                loadDashboardData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        actionErrorMessage = err.message ?: "Failed to assign team"
                    )
                }
            }
        }
    }

    fun updateStatus(newStatus: String, note: String, resolutionText: String? = null) {
        val complaintId = _uiState.value.selectedComplaintId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionErrorMessage = null) }
            val result = adminRepository.updateStatus(complaintId, newStatus, note, resolutionText)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        showStatusUpdateDialog = false,
                        actionFeedbackMessage = "Status updated to '$newStatus' with official ledger audit."
                    )
                }
                loadComplaintDetail(complaintId)
                loadDashboardData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        actionErrorMessage = err.message ?: "Failed to update status"
                    )
                }
            }
        }
    }

    fun addOfficialUpdate(message: String, officialName: String) {
        val complaintId = _uiState.value.selectedComplaintId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionErrorMessage = null) }
            val result = adminRepository.addOfficialUpdate(complaintId, message, officialName)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        showAddUpdateDialog = false,
                        actionFeedbackMessage = "Official municipal update published to citizen timeline."
                    )
                }
                loadComplaintDetail(complaintId)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        actionErrorMessage = err.message ?: "Failed to publish update"
                    )
                }
            }
        }
    }

    fun deleteComplaint(reason: String) {
        val complaintId = _uiState.value.selectedComplaintId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionErrorMessage = null) }
            val result = adminRepository.deleteComplaint(complaintId, reason)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        showDeleteConfirmDialog = false,
                        selectedComplaintId = null,
                        currentSection = AdminSection.COMPLAINTS,
                        actionFeedbackMessage = "Complaint permanently deleted and recorded in permanent audit logs."
                    )
                }
                loadComplaints()
                loadDashboardData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        actionErrorMessage = err.message ?: "Failed to delete complaint"
                    )
                }
            }
        }
    }

    fun loadTeams() {
        viewModelScope.launch {
            _uiState.update { it.copy(teamsState = UiState.Loading) }
            val result = adminRepository.getTeams()
            result.onSuccess { teams ->
                _uiState.update { it.copy(teamsState = UiState.Success(teams)) }
            }.onFailure { err ->
                _uiState.update { it.copy(teamsState = UiState.Error(err.message ?: "Failed to load teams")) }
            }
        }
    }

    fun loadServices() {
        viewModelScope.launch {
            _uiState.update { it.copy(servicesState = UiState.Loading) }
            val result = adminRepository.getAllServices()
            result.onSuccess { services ->
                _uiState.update { it.copy(servicesState = UiState.Success(services)) }
            }.onFailure { err ->
                _uiState.update { it.copy(servicesState = UiState.Error(err.message ?: "Failed to load services")) }
            }
        }
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(auditLogsState = UiState.Loading) }
            val result = adminRepository.getAuditLogs()
            result.onSuccess { logs ->
                _uiState.update { it.copy(auditLogsState = UiState.Success(logs)) }
            }.onFailure { err ->
                _uiState.update { it.copy(auditLogsState = UiState.Error(err.message ?: "Failed to load audit logs")) }
            }
        }
    }

    // Phase 9: Admin Evidence Management
    fun uploadEvidence(complaintId: String, fileName: String, mimeType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            val userId = _uiState.value.adminUser?.id ?: "admin-nagpur"
            civicRepository.uploadEvidence(
                complaintId = complaintId,
                userId = userId,
                fileName = fileName,
                mimeType = mimeType,
                bytes = bytes
            )
            _uiState.update { it.copy(actionInProgress = false) }
            loadComplaintDetail(complaintId)
        }
    }

    fun deleteEvidence(complaintId: String, evidenceId: String, storagePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            civicRepository.deleteEvidence(evidenceId, storagePath)
            _uiState.update { it.copy(actionInProgress = false) }
            loadComplaintDetail(complaintId)
        }
    }
}
