package edu.vt.cs5254.bucketlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class GoalDetailViewModel(goalId: UUID) : ViewModel() {

    private val goalRepository = GoalRepository.get()

    private val _goal: MutableStateFlow<Goal?> = MutableStateFlow(null)
    val goal: StateFlow<Goal?> = _goal.asStateFlow()

    init {
        viewModelScope.launch {
            _goal.value = goalRepository.getGoal(goalId)
        }
    }

    fun updateGoal(onUpdate: (Goal) -> Goal) {
        _goal.update { oldGoal->
            val newGoal = oldGoal?.let { onUpdate(it) } ?: return
            if (newGoal == oldGoal && newGoal.notes == oldGoal.notes) return
            newGoal.copy(lastUpdated = Date()).apply { notes = newGoal.notes }
        }
    }

    override fun onCleared() {
        super.onCleared()

        goal.value?.let { goalRepository.updateGoal(it) }
    }
}

class GoalDetailViewModelFactory(
    private val goalId: UUID
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoalDetailViewModel(goalId) as T
    }
}