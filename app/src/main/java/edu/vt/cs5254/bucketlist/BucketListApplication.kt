package edu.vt.cs5254.bucketlist

import android.app.Application

class BucketListApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        GoalRepository.initialize(this)
    }
}