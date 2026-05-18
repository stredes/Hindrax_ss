package com.hindrax.ss.features.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.data.entity.AuditSessionEntity
import com.hindrax.ss.data.repository.AuditRepository
import com.hindrax.ss.domain.RecommendationEngine
import com.hindrax.ss.reporting.MarkdownReportGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val session: AuditSessionEntity? = null,
    val results: List<AuditResultEntity> = emptyList(),
    val reportMarkdown: String = "",
    val recommendations: List<String> = emptyList()
)

class SessionDetailViewModel(
    private val sessionId: Long,
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSessionData()
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            val session = auditRepository.getSessionById(sessionId)
            _uiState.update { it.copy(session = session) }
            
            auditRepository.getResultsForSession(sessionId).collect { results ->
                val markdown = if (session != null) {
                    MarkdownReportGenerator.generate(session, results)
                } else ""
                
                val recommendations = RecommendationEngine.getRecommendations(results)
                
                _uiState.update { 
                    it.copy(
                        results = results, 
                        reportMarkdown = markdown,
                        recommendations = recommendations
                    ) 
                }
            }
        }
    }
}
