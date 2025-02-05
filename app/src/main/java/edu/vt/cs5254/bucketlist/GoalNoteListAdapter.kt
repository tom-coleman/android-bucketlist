package edu.vt.cs5254.bucketlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.bucketlist.databinding.ListItemGoalNoteBinding
import java.util.UUID

class GoalNoteHolder(
    private val binding: ListItemGoalNoteBinding
) : RecyclerView.ViewHolder(binding.root) {

    lateinit var boundNote: GoalNote
        private set

    fun bind(note: GoalNote) {
        boundNote = note

        binding.goalNoteButton.apply {
            visibility = View.VISIBLE
            text = note.type.toString()

            when(note.type) {
                GoalNoteType.PAUSED -> {
                    setBackgroundWithContrastingText("gray")
                }
                GoalNoteType.COMPLETED -> {
                    setBackgroundWithContrastingText("green")
                }
                GoalNoteType.PROGRESS -> {
                    text = note.text
                    setBackgroundWithContrastingText("navy")
                }
            }
        }
    }
}

class GoalNoteListAdapter (
    private val notes: List<GoalNote>
) : RecyclerView.Adapter<GoalNoteHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) : GoalNoteHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemGoalNoteBinding.inflate(inflater, parent, false)
        return GoalNoteHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalNoteHolder, position: Int) {
        val note = notes[position]
        holder.bind(note)
    }

    override fun getItemCount() = notes.size
}
