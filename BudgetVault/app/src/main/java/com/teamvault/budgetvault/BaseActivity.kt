package com.teamvault.budgetvault

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Apply the saved language before the activity is created
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before creating the activity
        ThemeManager.applyTheme(this)

        // Then proceed with normal activity creation
        super.onCreate(savedInstanceState)
    }
}