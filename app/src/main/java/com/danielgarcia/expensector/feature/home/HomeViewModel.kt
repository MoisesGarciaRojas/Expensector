package com.danielgarcia.expensector.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val profile: LocalOwnerProfile? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: LocalOwnerProfileRepository,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = profileRepository.observeProfile()
        .map { HomeUiState(loading = false, profile = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
