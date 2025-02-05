package edu.vt.cs5254.bucketlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.bucketlist.databinding.ListItemGoalBinding
import java.util.UUID

class GoalHolder(
    private val binding: ListItemGoalBinding
) : RecyclerView.ViewHolder(binding.root) {

    lateinit var boundGoal: Goal
        private set

    fun bind(goal: Goal, onGoalClicked: (goalId: UUID) -> Unit) {
        boundGoal = goal

        binding.root.setOnClickListener {
            onGoalClicked(goal.id)
        }

        binding.listItemTitle.text = goal.title

        val progressCount = goal.notes.count { it.type == GoalNoteType.PROGRESS }
        binding.listItemProgressCount.text = binding.root.context.getString(R.string.progress_count, progressCount)

        when {
            goal.isCompleted -> {
                binding.listItemImage.setImageResource(R.drawable.ic_goal_completed)
                binding.listItemImage.visibility = View.VISIBLE
            }
            goal.isPaused -> {
                binding.listItemImage.setImageResource(R.drawable.ic_goal_paused)
                binding.listItemImage.visibility = View.VISIBLE
            }
            else -> {
                binding.listItemImage.visibility = View.GONE
            }
        }
    }
}

class GoalListAdapter (
    private val goals: List<Goal>,
    private val onGoalClicked: (goalId: UUID) -> Unit
) : RecyclerView.Adapter<GoalHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) : GoalHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemGoalBinding.inflate(inflater, parent, false)
        return GoalHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalHolder, position: Int) {
        val goal = goals[position]
        holder.bind(goal, onGoalClicked)
    }

    override fun getItemCount() = goals.size
}
