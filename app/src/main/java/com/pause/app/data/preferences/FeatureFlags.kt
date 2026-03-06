package com.pause.app.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlags @Inject constructor(
    private val preferencesManager: PreferencesManager
) {

    val currentPhase: Flow<Int> = preferencesManager.currentPhase

    val isPhase2Enabled: Flow<Boolean> = preferencesManager.currentPhase.map {
        it >= PreferencesManager.PHASE_2
    }

    val isPhase3Enabled: Flow<Boolean> = preferencesManager.currentPhase.map {
        it >= PreferencesManager.PHASE_3
    }
}
