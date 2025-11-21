package com.teamvault.budgetvault

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MoreActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_more)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Initialize buttons
        val settingsButton = findViewById<Button>(R.id.settingsButton)
        val feedbackButton = findViewById<Button>(R.id.feedbackButton)
        val rateUsButton = findViewById<Button>(R.id.rateUsButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        val aiChatbotButton = findViewById<Button>(R.id.aiChatbotButton)

        // Set up button click listeners
        settingsButton.setOnClickListener {
            // Navigate to Settings screen
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        feedbackButton.setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java)
            startActivity(intent)
            finish()
        }

        rateUsButton.setOnClickListener {
            // Open app rating dialog or redirect to Play Store
        }

        signOutButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }

        aiChatbotButton.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)

        }

        // Set up bottom navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_more  // Highlight more icon

        // Handle bottom navigation item selection
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_budget -> {
                    // Navigate to budget
                    val intent = Intent(this, BudgetActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_add -> {
                    // Show the Add options dialog
                    showAddOptionsDialog()
                    true
                }
                R.id.navigation_advices -> {
                    // Navigate to advices screen
                    val intent = Intent(this, AdvicesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_more -> {
                    // Already on more screen
                    true
                }
                else -> false
            }
        }
    }

    // Method to show the Add options dialog with blurred background
    private fun showAddOptionsDialog() {
        // Create the dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_options)

        // Set dialog width to match parent and position at bottom
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.gravity = Gravity.BOTTOM
        dialog.window?.attributes = layoutParams

        // Set background to transparent for rounded corners and dim amount for blur effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f) // This creates the dim/blur effect

        // Set up button click listeners
        dialog.findViewById<Button>(R.id.addIncomeButton).setOnClickListener {
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            // Just dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }
}