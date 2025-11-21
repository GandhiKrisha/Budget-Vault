package com.teamvault.budgetvault

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Scheduler for recurring budget notifications
 */
object NotificationScheduler {

    private const val DAILY_REMINDER_WORK = "daily_reminder_work"
    private const val WEEKLY_SUMMARY_WORK = "weekly_summary_work"
    private const val MONTHLY_REMINDER_WORK = "monthly_reminder_work"

    /**
     * Schedule all recurring notifications
     */
    fun scheduleAllNotifications(context: Context) {
        scheduleDailyReminder(context)
        scheduleWeeklySummary(context)
        scheduleMonthlyReminder(context)
    }

    /**
     * Schedule daily expense reminder
     */
    private fun scheduleDailyReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val dailyReminderRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(8, TimeUnit.HOURS) // Start 8 hours from now
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyReminderRequest
        )
    }

    /**
     * Schedule weekly summary
     */
    private fun scheduleWeeklySummary(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val weeklySummaryRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(7, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEEKLY_SUMMARY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            weeklySummaryRequest
        )
    }

    /**
     * Schedule monthly reminder
     */
    private fun scheduleMonthlyReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val monthlyReminderRequest = PeriodicWorkRequestBuilder<MonthlyReminderWorker>(30, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MONTHLY_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            monthlyReminderRequest
        )
    }

    /**
     * Cancel all scheduled notifications
     */
    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(DAILY_REMINDER_WORK)
            cancelUniqueWork(WEEKLY_SUMMARY_WORK)
            cancelUniqueWork(MONTHLY_REMINDER_WORK)
        }
    }

    /**
     * Cancel specific notification type
     */
    fun cancelNotification(context: Context, workName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
}

/**
 * Worker for daily expense reminders
 */
class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // Check if user has added expenses today
            val prefs = applicationContext.getSharedPreferences("ActivityTracking", Context.MODE_PRIVATE)
            val lastActivity = prefs.getLong("lastExpenseEntry", 0)
            val currentTime = System.currentTimeMillis()
            val oneDayAgo = currentTime - (24 * 60 * 60 * 1000)

            // If no activity in the last 24 hours, send reminder
            if (lastActivity < oneDayAgo) {
                NotificationsManager.showDailyExpenseReminder(applicationContext)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * Worker for weekly summaries
 */
class WeeklySummaryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            NotificationsManager.showWeeklySummary(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * Worker for monthly budget reminders
 */
class MonthlyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            NotificationsManager.showMonthlyBudgetReminder(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}