package com.teamvault.budgetvault

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.teamvault.budgetvault.data.database.AppDatabase
import com.teamvault.budgetvault.data.model.Expense
import com.teamvault.budgetvault.data.repository.ExpenseRepository
import com.teamvault.budgetvault.data.repository.IncomeRepository
import io.grpc.android.BuildConfig
import kotlinx.coroutines.launch

class DashboardActivity : BaseActivity() {

    private lateinit var expenseItemsContainer: LinearLayout

    // Gamification components
    private lateinit var gamificationManager: GamificationManager
    private lateinit var progressBar: ProgressBar
    private lateinit var levelTextView: TextView
    private lateinit var achievementsText: TextView
    private lateinit var rewardsButton: Button

    // Firebase repositories
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var incomeRepository: IncomeRepository

    // Track sync state with cooldown
    private var lastSyncTime = 0L
    private val SYNC_COOLDOWN = 5 * 60 * 1000L // 5 minutes cooldown

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageManager.applyLanguage(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase repositories with context
        val database = AppDatabase.getDatabase(this)
        expenseRepository = ExpenseRepository(database.expenseDao(), this)
        incomeRepository = IncomeRepository(database.incomeDao())

        // Check notification permission
        checkAndRequestNotificationPermission()

        // Check if the user is a guest and trial validity
        handleGuestMode()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the expense list container
        expenseItemsContainer = findViewById(R.id.expenseItemsContainer)

        // Initialize dashboard components
        initializeUI()

        // Clear duplicates if in debug mode
        if (BuildConfig.DEBUG) {
            clearDuplicateExpenses()
        }

        // Display user-specific budget limits
        displayUserBudgetLimits()

        // Load user-specific expenses data
        loadUserExpensesData()

        // Check if sync is needed
        checkAndSyncFromFirebase()
    }

    private fun clearDuplicateExpenses() {
        lifecycleScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                val duplicatesRemoved = expenseRepository.clearDuplicateExpenses(currentUserId)

                if (duplicatesRemoved > 0) {
                    Log.d("DashboardActivity", "Removed $duplicatesRemoved duplicate expenses")
                    Toast.makeText(this@DashboardActivity,
                        "Cleaned up $duplicatesRemoved duplicate expenses",
                        Toast.LENGTH_SHORT).show()

                    // Refresh UI after cleanup
                    loadUserExpensesData()
                    loadUserRecentExpenses()
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error clearing duplicates: ${e.message}")
            }
        }
    }

    private fun handleGuestMode() {
        val sharedPref = getSharedPreferences("BudgetVaultPrefs", MODE_PRIVATE)
        val guestStartDate = sharedPref.getLong("guestStartDate", 0L)
        val currentTime = System.currentTimeMillis()
        val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
        val isGuest = intent.getBooleanExtra("isGuest", false)

        if (isGuest) {
            if (guestStartDate != 0L && currentTime - guestStartDate > sevenDaysMillis) {
                showTrialExpiredDialog()
            } else {
                val daysLeft = 7 - ((currentTime - guestStartDate) / (24 * 60 * 60 * 1000))
                Toast.makeText(this, "Guest Trial: $daysLeft days left", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    NotificationsManager.setNotificationsEnabled(this, true)
                    Toast.makeText(this, "Notifications enabled! You'll receive budget alerts.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notifications disabled. You can enable them in settings.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initializeUI() {
        try {
            val addExpenseButton = findViewById<Button>(R.id.addExpenseButton)
            val addIncomeButton = findViewById<Button>(R.id.addIncomeButton)
            val reportBtn = findViewById<ImageButton>(R.id.reportBtn)
            val settingsBtn = findViewById<ImageButton>(R.id.settingsBtn)
            val chatbotBtn = findViewById<ImageButton>(R.id.aiBtn)
            val viewdetBtn = findViewById<Button>(R.id.viewDetailsButton)

            // Initialize user-specific gamification components
            gamificationManager = GamificationManager.getInstance(context = this)
            progressBar = findViewById(R.id.goalProgressBar)
            levelTextView = findViewById(R.id.levelText)
            achievementsText = findViewById(R.id.achievementsText)
            rewardsButton = findViewById(R.id.rewardsButton)

            // Update user-specific gamification UI
            updateUserGamificationUI()

            // Set up button click listeners
            settingsBtn.setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            reportBtn.setOnClickListener {
                val intent = Intent(this, ExpenseReportActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            chatbotBtn.setOnClickListener {
                val intent = Intent(this, ChatbotActivity::class.java)
                startActivity(intent)
            }

            viewdetBtn.setOnClickListener {
                val intent = Intent(this, ExpenseListActivity::class.java)
                startActivity(intent)
            }

            addExpenseButton.setOnClickListener {
                val intent = Intent(this, AddExpenseActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_ADD_EXPENSE)
            }

            addIncomeButton.setOnClickListener {
                val intent = Intent(this, AddIncomeActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_ADD_INCOME)
            }

            rewardsButton.setOnClickListener {
                val intent = Intent(this, RewardsActivity::class.java)
                startActivity(intent)
            }

            setupBottomNavigation()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in initializeUI: ${e.message}")
            e.printStackTrace()
        }
    }

    // Update gamification UI for current user
    private fun updateUserGamificationUI() {
        try {
            val currentUserId = getCurrentUserId()

            // Get user-specific XP and level
            val currentXP = gamificationManager.getCurrentXP()
            val currentLevel = gamificationManager.getCurrentLevel()
            val progressPercent = gamificationManager.getProgressToNextLevel()

            // Update progress bar
            progressBar.progress = progressPercent

            // Update level text
            levelTextView.text = "Level $currentLevel"

            // Update the achievements text with user-specific data
            val totalBadges = 8
            val unlockedBadges = gamificationManager.getUnlockedBadges().size
            achievementsText.text = "🎉 You've unlocked $unlockedBadges out of $totalBadges badges!"

            Log.d("DashboardActivity", "Updated gamification for user: $currentUserId, Level: $currentLevel, XP: $currentXP")
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in updateUserGamificationUI: ${e.message}")
            e.printStackTrace()
        }
    }

    // Display user-specific budget limits
    private fun displayUserBudgetLimits() {
        try {
            val minSpendingText = findViewById<TextView>(R.id.minSpendingText)
            val maxSpendingText = findViewById<TextView>(R.id.maxSpendingText)

            val currentUserId = getCurrentUserId()

            // Get user-specific budget limits from SharedPreferences
            val sharedPreferences = getSharedPreferences("BudgetVaultPrefs_$currentUserId", MODE_PRIVATE)
            val minSpending = sharedPreferences.getFloat("minSpending", 0.0f)
            val maxSpending = sharedPreferences.getFloat("maxSpending", 0.0f)

            // Display the values with proper formatting
            minSpendingText.text = "R ${String.format("%.2f", minSpending)}"
            maxSpendingText.text = "R ${String.format("%.2f", maxSpending)}"

            Log.d("DashboardActivity", "Budget limits for user $currentUserId - Min: R$minSpending, Max: R$maxSpending")
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in displayUserBudgetLimits: ${e.message}")
            e.printStackTrace()
        }
    }

    // Load user-specific expenses data
    private fun loadUserExpensesData() {
        try {
            val totalExpensesText = findViewById<TextView>(R.id.totalExpensesText)

            lifecycleScope.launch {
                try {
                    // Get total expenses for current user only
                    val totalExpenses = expenseRepository.getTotalExpensesLocal() ?: 0.0
                    totalExpensesText.text = "R ${String.format("%.2f", totalExpenses)}"

                    val currentUserId = getCurrentUserId()
                    Log.d("DashboardActivity", "Total expenses for user $currentUserId: R$totalExpenses")

                    // Check if expenses are within user's budget
                    checkUserBudgetStatus(totalExpenses)

                    // Trigger notification check
                    NotificationsManager.checkBudgetStatus(this@DashboardActivity)

                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error loading user expense data: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in loadUserExpensesData: ${e.message}")
            e.printStackTrace()
        }
    }

    // Check if sync is needed with cooldown
    private fun checkAndSyncFromFirebase() {
        val currentTime = System.currentTimeMillis()

        // Check if enough time has passed since last sync
        if (currentTime - lastSyncTime < SYNC_COOLDOWN) {
            Log.d("DashboardActivity", "Skipping sync - cooldown period active")
            return
        }

        // Check if last sync was recent
        val sharedPrefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
        val lastAppSync = sharedPrefs.getLong("lastAppSync", 0L)

        if (currentTime - lastAppSync > SYNC_COOLDOWN) {
            syncDataFromFirebase()
            sharedPrefs.edit().putLong("lastAppSync", currentTime).apply()
        }
    }

    // Sync data from Firebase with improved duplicate prevention
    private fun syncDataFromFirebase() {
        lifecycleScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                Log.d("DashboardActivity", "Starting Firebase sync for user: ${getCurrentUserId()}")

                // Show loading indicator (optional)
                // progressBar.visibility = View.VISIBLE

                // Sync expenses from Firebase with duplicate prevention
                val expenseResult = expenseRepository.syncExpensesFromFirebase()

                // Sync income from Firebase
                val incomeResult = incomeRepository.syncIncomeFromFirebase()

                if (expenseResult.isSuccess && incomeResult.isSuccess) {
                    val newExpenses = expenseResult.getOrNull() ?: emptyList()
                    Log.d("DashboardActivity", "Firebase sync completed successfully. New expenses: ${newExpenses.size}")

                    lastSyncTime = currentTime

                    // Only reload data if new data was synced
                    if (newExpenses.isNotEmpty()) {
                        loadUserExpensesData()
                        loadUserRecentExpenses()
                    }
                } else {
                    Log.w("DashboardActivity", "Firebase sync had issues but continuing with local data")
                }

            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error syncing data from Firebase: ${e.message}")
                // Continue with local data even if sync fails
            } finally {
                // Hide loading indicator
                // progressBar.visibility = View.GONE
            }
        }
    }

    // Load and display recent expenses for current user
    private fun loadUserRecentExpenses() {
        try {
            lifecycleScope.launch {
                try {
                    expenseItemsContainer.removeAllViews()

                    val titleTextView = TextView(this@DashboardActivity).apply {
                        text = getString(R.string.expense_list_title)
                        textSize = 16f
                        setTextColor(resources.getColor(android.R.color.black, null))
                        setTypeface(null, Typeface.BOLD)
                        setPadding(0, 0, 0, 8)
                    }
                    expenseItemsContainer.addView(titleTextView)

                    val currentUserId = getCurrentUserId()

                    val categories = listOf("Food", "Entertainment", "Transport", "Clothing", "Rent")
                    val iconMap = mapOf(
                        "food" to R.drawable.food,
                        "entertainment" to R.drawable.entertainment,
                        "transport" to R.drawable.transportation,
                        "transportation" to R.drawable.transportation,
                        "clothing" to R.drawable.clothing,
                        "rent" to R.drawable.rent
                    )

                    // Get recent expenses for current user only
                    val recentExpenses = expenseRepository.getRecentExpensesLocal(currentUserId, 5)

                    if (recentExpenses.isNotEmpty()) {
                        Log.d("DashboardActivity", "Displaying ${recentExpenses.size} recent expenses for user: $currentUserId")
                        for (expense in recentExpenses) {
                            addExpenseItemToList(expense, iconMap)
                        }
                    } else {
                        for (category in categories) {
                            val placeholderExpense = Expense(
                                id = 0,
                                userId = "",
                                date = "",
                                startTime = "",
                                endTime = "",
                                description = "",
                                amount = 0.0,
                                category = category,
                                photoUri = null
                            )
                            addExpenseItemToList(placeholderExpense, iconMap)
                        }
                    }

                    // Add View All button
                    val viewAllButton = Button(this@DashboardActivity).apply {
                        id = R.id.viewAllButton
                        text = getString(R.string.view_all_button)
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER
                            topMargin = 8
                        }
                        backgroundTintList = resources.getColorStateList(R.color.green, null)
                        setTextColor(resources.getColor(android.R.color.white, null))
                        setOnClickListener {
                            val intent = Intent(this@DashboardActivity, ExpenseListActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    expenseItemsContainer.addView(viewAllButton)

                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error loading user recent expenses: ${e.message}")
                    e.printStackTrace()
                    Toast.makeText(this@DashboardActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in loadUserRecentExpenses: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun addExpenseItemToList(expense: Expense, iconMap: Map<String, Int>) {
        try {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 8, 8, 8)
            }

            val categoryLowercase = expense.category?.lowercase() ?: "other"
            val iconResourceId = iconMap[categoryLowercase] ?: R.drawable.extra

            val categoryIcon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(24, 24)
                setImageResource(iconResourceId)
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(8, 0, 0, 0)
            }

            val categoryText = TextView(this).apply {
                text = expense.category ?: "Uncategorized"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.black, null))
                setTypeface(null, Typeface.BOLD)
            }

            val amountText = TextView(this).apply {
                text = "R ${String.format("%.2f", expense.amount)}"
                textSize = 12f
                setTextColor(Color.GRAY)
            }

            val dateText = TextView(this).apply {
                text = if (expense.date.isNullOrEmpty()) "" else expense.date
                textSize = 12f
                setTextColor(Color.GRAY)
            }

            infoLayout.addView(categoryText)
            infoLayout.addView(amountText)

            itemLayout.addView(categoryIcon)
            itemLayout.addView(infoLayout)
            itemLayout.addView(dateText)

            expenseItemsContainer.addView(itemLayout)
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in addExpenseItemToList: ${e.message}")
            e.printStackTrace()
        }
    }

    // Check budget status for current user
    private fun checkUserBudgetStatus(totalExpenses: Double) {
        try {
            val currentUserId = getCurrentUserId()
            val sharedPreferences = getSharedPreferences("BudgetVaultPrefs_$currentUserId", MODE_PRIVATE)
            val minSpending = sharedPreferences.getFloat("minSpending", 0.0f)
            val maxSpending = sharedPreferences.getFloat("maxSpending", 0.0f)

            if (minSpending > 0 && maxSpending > 0) {
                when {
                    totalExpenses < minSpending -> {
                        Toast.makeText(this, "You're below your minimum spending target!", Toast.LENGTH_SHORT).show()
                    }
                    totalExpenses > maxSpending -> {
                        Toast.makeText(this, "Warning: You've exceeded your maximum spending limit!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in checkUserBudgetStatus: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkForLargeExpenseAlert() {
        lifecycleScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                val recentExpenses = expenseRepository.getRecentExpensesLocal(currentUserId, 1)

                if (recentExpenses.isNotEmpty()) {
                    val latestExpense = recentExpenses[0]
                    val budgetPrefs = getSharedPreferences("BudgetVaultPrefs_$currentUserId", MODE_PRIVATE)
                    val maxSpending = budgetPrefs.getFloat("maxSpending", 0.0f).toDouble()

                    val largeExpenseThreshold = if (maxSpending > 0) {
                        maxOf(500.0, maxSpending * 0.2)
                    } else {
                        500.0
                    }

                    if (latestExpense.amount >= largeExpenseThreshold) {
                        NotificationsManager.showLargeExpenseAlert(this@DashboardActivity, latestExpense.amount)
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error checking for large expense: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun checkWeeklyActivity() {
        val currentUserId = getCurrentUserId()
        val prefs = getSharedPreferences("ActivityTracking_$currentUserId", MODE_PRIVATE)
        val lastWeeklySummary = prefs.getLong("lastWeeklySummary", 0)
        val currentTime = System.currentTimeMillis()
        val oneWeek = 7 * 24 * 60 * 60 * 1000L

        if (currentTime - lastWeeklySummary > oneWeek) {
            NotificationsManager.showWeeklySummary(this)
            prefs.edit().putLong("lastWeeklySummary", currentTime).apply()
        }
    }

    private fun checkForInactivity() {
        val currentUserId = getCurrentUserId()
        val prefs = getSharedPreferences("ActivityTracking_$currentUserId", MODE_PRIVATE)
        val lastActivity = prefs.getLong("lastExpenseEntry", System.currentTimeMillis())
        val currentTime = System.currentTimeMillis()
        val daysSinceLastEntry = ((currentTime - lastActivity) / (24 * 60 * 60 * 1000)).toInt()

        if (daysSinceLastEntry >= 3) {
            NotificationsManager.showInactivityReminder(this, daysSinceLastEntry)
        }

        prefs.edit().putLong("lastExpenseEntry", currentTime).apply()
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_dashboard -> true
                    R.id.navigation_budget -> {
                        val intent = Intent(this, BudgetActivity::class.java)
                        startActivityForResult(intent, REQUEST_CODE_SET_BUDGET)
                        true
                    }
                    R.id.navigation_add -> {
                        showAddOptionsDialog()
                        true
                    }
                    R.id.navigation_advices -> {
                        val intent = Intent(this, AdvicesActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.navigation_more -> {
                        val intent = Intent(this, MoreActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in setupBottomNavigation: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showTrialExpiredDialog() {
        try {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Trial Expired")
            builder.setMessage("Your 7-day guest trial has ended. Please create an account to continue using Budget Vault.")
            builder.setCancelable(false)
            builder.setPositiveButton("Create Account") { dialog, _ ->
                val intent = Intent(this, CreateAccountActivity::class.java)
                startActivity(intent)
                finish()
            }
            builder.show()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in showTrialExpiredDialog: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showAddOptionsDialog() {
        try {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_add_options)

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.gravity = Gravity.BOTTOM
            dialog.window?.attributes = layoutParams

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setDimAmount(0.7f)

            dialog.findViewById<Button>(R.id.addIncomeButton).setOnClickListener {
                val intent = Intent(this, AddIncomeActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_ADD_INCOME)
                dialog.dismiss()
            }

            dialog.findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
                val intent = Intent(this, AddExpenseActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_ADD_EXPENSE)
                dialog.dismiss()
            }

            dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in showAddOptionsDialog: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showXPEarnedSnackbar(xpEarned: Int) {
        try {
            val rootView = findViewById<View>(android.R.id.content)
            Snackbar.make(rootView, "+$xpEarned XP earned!", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in showXPEarnedSnackbar: ${e.message}")
            e.printStackTrace()
        }
    }

    // Get current user ID for user-specific operations
    private fun getCurrentUserId(): String {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("userId", "default_user") ?: "default_user"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)

            if (resultCode == RESULT_OK) {
                when (requestCode) {
                    REQUEST_CODE_ADD_EXPENSE -> {
                        val unlockedBadges = gamificationManager.trackAction(
                            GamificationManager.ACTION_ADD_EXPENSE,
                            this,
                            progressBar,
                            levelTextView
                        )

                        val xpEarned = GamificationManager.XP_EXPENSE_ADDED
                        showXPEarnedSnackbar(xpEarned)

                        updateUserGamificationUI()
                        checkForLargeExpenseAlert()
                        NotificationsManager.checkBudgetStatus(this)
                        loadUserExpensesData()
                        loadUserRecentExpenses()
                    }
                    REQUEST_CODE_ADD_INCOME -> {
                        val unlockedBadges = gamificationManager.trackAction(
                            GamificationManager.ACTION_ADD_INCOME,
                            this,
                            progressBar,
                            levelTextView
                        )

                        val xpEarned = GamificationManager.XP_INCOME_ADDED
                        showXPEarnedSnackbar(xpEarned)

                        updateUserGamificationUI()
                        loadUserExpensesData()
                        loadUserRecentExpenses()
                    }
                    REQUEST_CODE_SET_BUDGET -> {
                        val unlockedBadges = gamificationManager.trackAction(
                            GamificationManager.ACTION_SET_BUDGET,
                            this,
                            progressBar,
                            levelTextView
                        )

                        val xpEarned = GamificationManager.XP_SET_BUDGET
                        showXPEarnedSnackbar(xpEarned)

                        updateUserGamificationUI()
                        Toast.makeText(this, "Budget set! You'll receive alerts when limits are reached.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in onActivityResult: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        try {
            super.onResume()

            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNavigation.selectedItemId = R.id.navigation_dashboard

            // Refresh user-specific data (local data only)
            displayUserBudgetLimits()
            loadUserExpensesData()
            loadUserRecentExpenses()
            updateUserGamificationUI()

            // Check if sync is needed (with cooldown)
            checkAndSyncFromFirebase()

            checkWeeklyActivity()
            checkForInactivity()

        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in onResume: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        const val REQUEST_CODE_ADD_EXPENSE = 101
        const val REQUEST_CODE_ADD_INCOME = 102
        const val REQUEST_CODE_SET_BUDGET = 103
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 104
    }
}