package edu.vt.cs5254.bucketlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "goal")
data class Goal(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String = "",
    val lastUpdated: Date = Date(),
) {
    @Ignore
    var notes = emptyList<GoalNote>()
    val isCompleted get() = notes.any { it.type == GoalNoteType.COMPLETED }
    val isPaused get() = notes.any { it.type == GoalNoteType.PAUSED }
    val photoFileName get() = "IMG_$id.JPG"
}

@Entity(tableName = "goal_note")
data class GoalNote(
    @PrimaryKey @ColumnInfo(name = "noteId") val id: UUID = UUID.randomUUID(),
    val text: String = "",
    val type: GoalNoteType,
    val goalId: UUID
)

enum class GoalNoteType {
    PROGRESS,
    PAUSED,
    COMPLETED,
}