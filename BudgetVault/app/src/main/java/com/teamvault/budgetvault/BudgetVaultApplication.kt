package com.teamvault.budgetvault

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings

class BudgetVaultApplication : Application() {

    override fun attachBaseContext(base: Context) {
        // Apply saved language before app starts
        super.attachBaseContext(LanguageManager.applyLanguage(base))
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize notifications
        NotificationsManager.setNotificationsEnabled(this, true)

        // Schedule recurring notifications
        NotificationScheduler.scheduleAllNotifications(this)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Enable Firestore offline persistence
        val firestore = FirebaseFirestore.getInstance()
        val settings = firestoreSettings {
            isPersistenceEnabled = true
        }
        firestore.firestoreSettings = settings
    }
}
