package edu.vt.cs5254.bucketlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoalListViewModel : ViewModel() {
    private val goalRepository = GoalRepository.get()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals get() = _goals.asStateFlow()

    init {
        viewModelScope.launch {
            goalRepository.getGoals().collect {
                _goals.value = it
            }
        }
    }

    suspend fun addGoal(goal: Goal) {
        goalRepository.addGoal(goal)
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }
}