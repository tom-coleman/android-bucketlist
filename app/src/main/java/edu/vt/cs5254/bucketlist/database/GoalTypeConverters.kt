package edu.vt.cs5254.bucketlist.database

import androidx.room.TypeConverter
import java.util.Date

class GoalTypeConverters {

    @TypeConverter
    fun longToDate(millis: Long): Date = Date(millis)

    @TypeConverter
    fun dateToLong(date: Date): Long = date.time
}