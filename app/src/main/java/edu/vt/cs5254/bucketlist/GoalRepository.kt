package edu.vt.cs5254.bucketlist

import android.content.Context
import androidx.room.Room
import edu.vt.cs5254.bucketlist.database.GoalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

private const val DATABASE_NAME = "goal-database"

class GoalRepository private constructor(
    context: Context,
    private val coroutineScope: CoroutineScope = GlobalScope
) {

    private val database: GoalDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            GoalDatabase::class.java,
            DATABASE_NAME
        )
        .createFromAsset(DATABASE_NAME)
        .build()

    // Transform the DAO multimap into a list of goals, each with its notes:
    fun getGoals(): Flow<List<Goal>> {
        val goalMapFlow = database.goalDao().getGoals()
        return goalMapFlow.map { goalMap ->
            goalMap.keys.map { goal ->
                goal.apply { notes = goalMap.getValue(goal) }
            }
        }
    }

    // Call the DAO transaction function, to get a single goal with its notes:
    suspend fun getGoal(id: UUID): Goal = database.goalDao().getGoalAndNotes(id)

    fun updateGoal(goal: Goal) {
        coroutineScope.launch {
            database.goalDao().updateGoalAndNotes(goal)
        }
    }

    suspend fun addGoal(goal: Goal) = database.goalDao().insertGoalAndNotes(goal)

    suspend fun deleteGoal(goal: Goal) {
        database.goalDao().deleteGoalAndNotes(goal)
    }

    companion object {
        private var INSTANCE: GoalRepository? = null

        fun initialize(context: Context) {
            check(INSTANCE == null) { "GoalRepository is ALREADY initialized!" }
            INSTANCE = GoalRepository(context)
        }

        fun get(): GoalRepository {
            checkNotNull(INSTANCE) { "GoalRepository MUST be initialized!" }
            return INSTANCE ?:
            throw IllegalStateException("GoalRepository must be initialized")
        }
    }
}