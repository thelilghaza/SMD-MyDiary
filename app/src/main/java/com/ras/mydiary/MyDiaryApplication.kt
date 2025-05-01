package com.ras.mydiary

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.onesignal.OneSignal

class MyDiaryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseAuth.getInstance()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true) // Optional: enables offline persistence

        OneSignal.initWithContext(this,"a3838d33-5b9f-4f54-ad93-4b0ac2474988")

        // Other initializations can go here if needed
    }
}
