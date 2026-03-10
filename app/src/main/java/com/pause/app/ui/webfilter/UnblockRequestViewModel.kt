package com.pause.app.ui.webfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.PendingReview
import com.pause.app.data.db.dao.PendingReviewDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnblockRequestViewModel @Inject constructor(
    private val pendingReviewDao: PendingReviewDao
) : ViewModel() {

    private val _submitted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val submitted: SharedFlow<Unit> = _submitted

    fun submitRequest(domain: String, childNote: String?) {
        viewModelScope.launch {
            pendingReviewDao.insert(
                PendingReview(
                    domain = domain,
                    triggerKeyword = null,
                    flaggedAt = System.currentTimeMillis(),
                    status = "PENDING",
                    childNote = childNote?.takeIf { it.isNotBlank() }
                )
            )
            _submitted.emit(Unit)
        }
    }
}
