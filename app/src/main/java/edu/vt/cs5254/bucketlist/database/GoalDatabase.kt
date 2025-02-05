package edu.vt.cs5254.bucketlist.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.vt.cs5254.bucketlist.Goal
import edu.vt.cs5254.bucketlist.GoalNote

@Database(entities = [Goal::class, GoalNote::class], version = 1)
@TypeConverters(GoalTypeConverters::class)
abstract class GoalDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
}