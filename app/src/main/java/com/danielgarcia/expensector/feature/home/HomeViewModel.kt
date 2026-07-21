package com.danielgarcia.expensector.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import com.danielgarcia.expensector.domain.Stage2HomeSummary
import com.danielgarcia.expensector.domain.Stage2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val profile: LocalOwnerProfile? = null,
    val summary: Stage2HomeSummary? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: LocalOwnerProfileRepository,
    stage2Repository: Stage2Repository,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        profileRepository.observeProfile(),
        stage2Repository.observeHomeSummary(LocalDate.now()),
    ) { profile, summary -> HomeUiState(loading = false, profile = profile, summary = summary) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
