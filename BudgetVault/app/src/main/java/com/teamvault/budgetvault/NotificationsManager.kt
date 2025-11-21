package com.teamvault.budgetvault

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.teamvault.budgetvault.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager class for handling notification preferences and displaying smart budget notifications
 */
object NotificationsManager {
    private const val CHANNEL_ID = "budgetvault_channel"
    private const val PREF_NAME = "app_preferences"
    private const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val PREF_NOTIFICATION_COOLDOWN = "notification_cooldown"

    // Notification IDs for different types
    private const val NOTIFICATION_ID_BUDGET_EXCEEDED = 1001
    private const val NOTIFICATION_ID_BUDGET_WARNING = 1002
    private const val NOTIFICATION_ID_LOW_SPENDING = 1003
    private const val NOTIFICATION_ID_WEEKLY_SUMMARY = 1004
    private const val NOTIFICATION_ID_MONTHLY_REMINDER = 1005
    private const val NOTIFICATION_ID_SAVINGS_GOAL = 1006
    private const val NOTIFICATION_ID_DAILY_REMINDER = 1007
    private const val NOTIFICATION_ID_BADGE_EARNED = 1008

    // Cooldown periods (in milliseconds)
    private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, true) // Default to true
    }

    /**
     * Update notification preferences
     */
    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled).apply()

        // Create notification channel when enabling notifications
        if (enabled) {
            createNotificationChannel(context)
        }
    }

    /**
     * Check if enough time has passed since last notification of this type
     */
    private fun canShowNotification(context: Context, notificationType: String, cooldownPeriod: Long = ONE_DAY_MILLIS): Boolean {
        val sharedPrefs = context.getSharedPreferences(PREF_NOTIFICATION_COOLDOWN, Context.MODE_PRIVATE)
        val lastShownTime = sharedPrefs.getLong("last_$notificationType", 0L)
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastShownTime) >= cooldownPeriod
    }

    /**
     * Mark that a notification of this type was shown
     */
    private fun markNotificationShown(context: Context, notificationType: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NOTIFICATION_COOLDOWN, Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("last_$notificationType", System.currentTimeMillis()).apply()
    }

    /**
     * Create the notification channel for Android 8.0+
     */
    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ (Android 8.0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Vault"
            val descriptionText = "Budget Vault notifications for spending alerts and reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }

            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show a notification
     */
    private fun showNotification(context: Context, title: String, message: String, notificationId: Int, priority: Int = NotificationCompat.PRIORITY_DEFAULT) {
        try {
            // Check if notifications are enabled in app preferences
            if (!areNotificationsEnabled(context)) {
                return
            }

            // Ensure channel is created
            createNotificationChannel(context)

            // Create intent to open dashboard when notification is tapped
            val intent = Intent(context, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            // Show the notification
            with(NotificationManagerCompat.from(context)) {
                try {
                    notify(notificationId, builder.build())
                } catch (e: SecurityException) {
                    Log.e("NotificationsManager", "Permission denied for showing notification: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationsManager", "Error showing notification: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Check budget status and show appropriate notifications
     */
    fun checkBudgetStatus(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)

                // Get current user ID
                val sharedPreferences = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
                val currentUserId = sharedPreferences.getString("userId", "default_user") ?: "default_user"

                // Get total expenses for current user
                val totalExpenses = database.expenseDao().getTotalExpensesByUser(currentUserId) ?: 0.0

                val budgetPrefs = context.getSharedPreferences("BudgetVaultPrefs_$currentUserId", Context.MODE_PRIVATE)
                val minSpending = budgetPrefs.getFloat("minSpending", 0.0f).toDouble()
                val maxSpending = budgetPrefs.getFloat("maxSpending", 0.0f).toDouble()

                // Only check if budget limits are set
                if (minSpending > 0 && maxSpending > 0) {
                    when {
                        totalExpenses >= maxSpending -> {
                            // Budget exceeded - high priority notification (once per day)
                            if (canShowNotification(context, "budget_exceeded", ONE_DAY_MILLIS)) {
                                showBudgetExceededNotification(context, totalExpenses, maxSpending)
                                markNotificationShown(context, "budget_exceeded")
                            }
                        }
                        totalExpenses >= (maxSpending * 0.8) -> {
                            // 80% of budget reached - warning notification (once per day)
                            if (canShowNotification(context, "budget_warning", ONE_DAY_MILLIS)) {
                                showBudgetWarningNotification(context, totalExpenses, maxSpending)
                                markNotificationShown(context, "budget_warning")
                            }
                        }
                        totalExpenses < (minSpending * 0.5) -> {
                            // Very low spending - might be good or bad (once per day)
                            if (canShowNotification(context, "low_spending", ONE_DAY_MILLIS)) {
                                showLowSpendingNotification(context, totalExpenses, minSpending)
                                markNotificationShown(context, "low_spending")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationsManager", "Error checking budget status: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Show budget exceeded notification
     */
    private fun showBudgetExceededNotification(context: Context, currentSpending: Double, budgetLimit: Double) {
        val overspent = currentSpending - budgetLimit
        val title = "⚠️ Budget Exceeded!"
        val message = "You've overspent by R${String.format("%.2f", overspent)}. Current spending: R${String.format("%.2f", currentSpending)} (Limit: R${String.format("%.2f", budgetLimit)})"

        showNotification(context, title, message, NOTIFICATION_ID_BUDGET_EXCEEDED, NotificationCompat.PRIORITY_HIGH)
    }

    /**
     * Show budget warning notification (80% reached)
     */
    private fun showBudgetWarningNotification(context: Context, currentSpending: Double, budgetLimit: Double) {
        val remaining = budgetLimit - currentSpending
        val percentage = ((currentSpending / budgetLimit) * 100).toInt()
        val title = "📊 Budget Alert"
        val message = "You've used $percentage% of your budget. R${String.format("%.2f", remaining)} remaining this month."

        showNotification(context, title, message, NOTIFICATION_ID_BUDGET_WARNING)
    }

    /**
     * Show low spending notification
     */
    private fun showLowSpendingNotification(context: Context, currentSpending: Double, minSpending: Double) {
        val title = "💡 Low Spending Notice"
        val message = "Your spending is quite low this month (R${String.format("%.2f", currentSpending)}). Consider if you're meeting your financial goals."

        showNotification(context, title, message, NOTIFICATION_ID_LOW_SPENDING)
    }

    /**
     * Show weekly spending summary
     */
    fun showWeeklySummary(context: Context) {
        // Only show once per week
        if (!canShowNotification(context, "weekly_summary", 7 * ONE_DAY_MILLIS)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)

                // Get current user ID
                val sharedPreferences = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
                val currentUserId = sharedPreferences.getString("userId", "default_user") ?: "default_user"

                // Get expenses from the last 7 days
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

                val weeklyExpenses = database.expenseDao().getExpensesSinceByUser(currentUserId, weekAgo) ?: 0.0
                val expenseCount = database.expenseDao().getExpenseCountSinceByUser(currentUserId, weekAgo) ?: 0

                val title = "📈 Weekly Summary"
                val message = "This week you spent R${String.format("%.2f", weeklyExpenses)} across $expenseCount transactions. Keep tracking!"

                showNotification(context, title, message, NOTIFICATION_ID_WEEKLY_SUMMARY)
                markNotificationShown(context, "weekly_summary")
            } catch (e: Exception) {
                Log.e("NotificationsManager", "Error showing weekly summary: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Show monthly budget reminder
     */
    fun showMonthlyBudgetReminder(context: Context) {
        // Only show once per month
        if (!canShowNotification(context, "monthly_reminder", 30 * ONE_DAY_MILLIS)) {
            return
        }

        val title = "📅 Monthly Check-in"
        val message = "New month, new budget! Time to review your spending goals and set limits for this month."

        showNotification(context, title, message, NOTIFICATION_ID_MONTHLY_REMINDER)
        markNotificationShown(context, "monthly_reminder")
    }

    /**
     * Show savings goal notification
     */
    fun showSavingsGoalNotification(context: Context, savedAmount: Double, goalAmount: Double) {
        val percentage = ((savedAmount / goalAmount) * 100).toInt()
        val title = "💰 Savings Progress"
        val message = "Great job! You're $percentage% towards your savings goal. Keep it up!"

        showNotification(context, title, message, NOTIFICATION_ID_SAVINGS_GOAL)
    }

    /**
     * Show daily expense reminder
     */
    fun showDailyExpenseReminder(context: Context) {
        // Only show once per day
        if (!canShowNotification(context, "daily_reminder", ONE_DAY_MILLIS)) {
            return
        }

        val title = "📝 Daily Reminder"
        val message = "Don't forget to log your expenses today to stay on track with your budget!"

        showNotification(context, title, message, NOTIFICATION_ID_DAILY_REMINDER)
        markNotificationShown(context, "daily_reminder")
    }

    /**
     * Show badge earned notification (no cooldown - always show when badge is earned)
     */
    fun showBadgeEarnedNotification(context: Context, badgeName: String) {
        val title = "🏆 Badge Unlocked!"
        val message = "Congratulations! You've earned the '$badgeName' badge. Keep up the great work!"

        showNotification(context, title, message, NOTIFICATION_ID_BADGE_EARNED, NotificationCompat.PRIORITY_HIGH)
    }

    /**
     * Show inactivity reminder
     */
    fun showInactivityReminder(context: Context, daysSinceLastEntry: Int) {
        // Only show once per day
        if (!canShowNotification(context, "inactivity_reminder", ONE_DAY_MILLIS)) {
            return
        }

        val title = "🔔 We Miss You!"
        val message = "It's been $daysSinceLastEntry days since your last expense entry. Stay on track with your budget!"

        showNotification(context, title, message, NOTIFICATION_ID_DAILY_REMINDER)
        markNotificationShown(context, "inactivity_reminder")
    }

    /**
     * Show expense category limit warning
     */
    fun showCategoryLimitWarning(context: Context, category: String, spent: Double, limit: Double) {
        // Only show once per day per category
        if (!canShowNotification(context, "category_warning_$category", ONE_DAY_MILLIS)) {
            return
        }

        val title = "⚠️ Category Alert"
        val message = "You've spent R${String.format("%.2f", spent)} on $category this month (Limit: R${String.format("%.2f", limit)})"

        showNotification(context, title, message, NOTIFICATION_ID_BUDGET_WARNING)
        markNotificationShown(context, "category_warning_$category")
    }

    /**
     * Show large expense alert (show once per hour to avoid spam)
     */
    fun showLargeExpenseAlert(context: Context, amount: Double) {
        // Only show once per hour to avoid spam when adding multiple large expenses
        if (!canShowNotification(context, "large_expense", ONE_HOUR_MILLIS)) {
            return
        }

        val title = "💸 Large Expense Alert"
        val message = "You just recorded a large expense of R${String.format("%.2f", amount)}. Make sure this aligns with your budget!"

        showNotification(context, title, message, NOTIFICATION_ID_BUDGET_WARNING, NotificationCompat.PRIORITY_HIGH)
        markNotificationShown(context, "large_expense")
    }

    /**
     * Clear all notifications
     */
    fun clearAllNotifications(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
    }

    /**
     * Clear specific notification
     */
    fun clearNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    /**
     * Reset notification cooldowns (useful for testing)
     */
    fun resetNotificationCooldowns(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREF_NOTIFICATION_COOLDOWN, Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }

    /**
     * Get time until next notification can be shown (for debugging)
     */
    fun getTimeUntilNextNotification(context: Context, notificationType: String): Long {
        val sharedPrefs = context.getSharedPreferences(PREF_NOTIFICATION_COOLDOWN, Context.MODE_PRIVATE)
        val lastShownTime = sharedPrefs.getLong("last_$notificationType", 0L)
        val currentTime = System.currentTimeMillis()
        val timeSinceLastShown = currentTime - lastShownTime
        val timeRemaining = ONE_DAY_MILLIS - timeSinceLastShown

        return maxOf(0L, timeRemaining)
    }
}