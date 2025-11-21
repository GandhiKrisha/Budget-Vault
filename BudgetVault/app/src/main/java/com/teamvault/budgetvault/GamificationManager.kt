package com.teamvault.budgetvault

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

/**
 * GamificationManager handles all gamification features in the app:
 * - XP tracking and progress
 * - Badge unlocking and management
 * - Celebration animations
 */
class GamificationManager private constructor(private val context: Context) {

    companion object {
        // Singleton instance
        @Volatile
        private var instance: GamificationManager? = null

        fun getInstance(context: Context): GamificationManager {
            return instance ?: synchronized(this) {
                instance ?: GamificationManager(context.applicationContext).also { instance = it }
            }
        }

        // Constants for badge IDs
        const val BADGE_FIRST_EXPENSE = "badge_first_expense"
        const val BADGE_FIRST_INCOME = "badge_first_income"
        const val BADGE_SET_BUDGET = "badge_set_budget"
        const val BADGE_COMPLETE_QUIZ = "badge_complete_quiz"
        const val BADGE_WATCH_VIDEO = "badge_watch_video"
        const val BADGE_INVITE_FRIEND = "badge_invite_friend"
        const val BADGE_STREAK_7_DAYS = "badge_streak_7_days"
        const val BADGE_BUDGET_MASTER = "badge_budget_master"

        // XP Constants
        const val XP_EXPENSE_ADDED = 10
        const val XP_INCOME_ADDED = 15
        const val XP_SET_BUDGET = 25
        const val XP_QUIZ_COMPLETED = 50
        const val XP_QUIZ_PER_CORRECT = 5
        const val XP_WATCH_VIDEO = 20
        const val XP_INVITE_FRIEND = 30

        // Level thresholds (XP needed to reach each level)
        private val LEVEL_THRESHOLDS = listOf(
            0,     // Level 1
            100,   // Level 2
            250,   // Level 3
            500,   // Level 4
            1000,  // Level 5
            2000   // Level 6
        )

        // Action constants for tracking
        const val ACTION_ADD_EXPENSE = "add_expense"
        const val ACTION_ADD_INCOME = "add_income"
        const val ACTION_SET_BUDGET = "set_budget"
        const val ACTION_COMPLETE_QUIZ = "complete_quiz"
        const val ACTION_WATCH_VIDEO = "watch_video"
        const val ACTION_INVITE_FRIEND = "invite_friend"
    }

    // SharedPreferences for storing gamification data
    private val prefs: SharedPreferences = context.getSharedPreferences("GamificationPrefs", Context.MODE_PRIVATE)

    // Badge information - ID, name, description, resource ID, and XP requirement
    private val badges = mapOf(
        BADGE_FIRST_EXPENSE to Badge(
            BADGE_FIRST_EXPENSE,
            "First Expense",
            "Added your first expense",
            R.drawable.firstsaving,
            0
        ),
        BADGE_FIRST_INCOME to Badge(
            BADGE_FIRST_INCOME,
            "Income Tracker",
            "Added your first income",
            R.drawable.moneystar,
            0
        ),
        BADGE_SET_BUDGET to Badge(
            BADGE_SET_BUDGET,
            "Budget Planner",
            "Set your first budget limits",
            R.drawable.savingsbroker,
            0
        ),
        BADGE_COMPLETE_QUIZ to Badge(
            BADGE_COMPLETE_QUIZ,
            "Financial Learner",
            "Completed your first financial quiz",
            R.drawable.streakbadge,
            0
        ),
        BADGE_WATCH_VIDEO to Badge(
            BADGE_WATCH_VIDEO,
            "Financial Student",
            "Watched a financial advice video",
            R.drawable.moneystar,
            100
        ),
        BADGE_INVITE_FRIEND to Badge(
            BADGE_INVITE_FRIEND,
            "Community Builder",
            "Invited a friend to BudgetVault",
            R.drawable.streakbadge,
            250
        ),
        BADGE_STREAK_7_DAYS to Badge(
            BADGE_STREAK_7_DAYS,
            "Consistency King",
            "Used the app for 7 days in a row",
            R.drawable.savingsbroker,
            500
        ),
        BADGE_BUDGET_MASTER to Badge(
            BADGE_BUDGET_MASTER,
            "Budget Master",
            "Stayed within budget for 30 days",
            R.drawable.moneystar,
            1000
        )
    )

    /**
     * Get the current user XP
     */
    fun getCurrentXP(): Int {
        return prefs.getInt("user_xp", 0)
    }

    /**
     * Add XP to the user and check for level ups and badge unlocks
     */
    fun addXP(amount: Int, progressBar: ProgressBar? = null, levelText: TextView? = null): Boolean {
        val currentXP = getCurrentXP()
        val newXP = currentXP + amount
        val currentLevel = getCurrentLevel(currentXP)
        val newLevel = getCurrentLevel(newXP)

        // Save the new XP
        prefs.edit().putInt("user_xp", newXP).apply()

        // Update UI if provided
        updateProgressBar(progressBar, newXP)
        updateLevelText(levelText, newLevel)

        // Check if level up occurred
        return newLevel > currentLevel
    }

    /**
     * Get the current user level based on XP
     */
    fun getCurrentLevel(xp: Int = getCurrentXP()): Int {
        var level = 1
        for (threshold in LEVEL_THRESHOLDS) {
            if (xp >= threshold) {
                level++
            } else {
                break
            }
        }
        return level - 1 // Adjust because we started counting from 1
    }

    /**
     * Get XP needed for next level
     */
    fun getXPForNextLevel(xp: Int = getCurrentXP()): Int {
        val currentLevel = getCurrentLevel(xp)
        if (currentLevel >= LEVEL_THRESHOLDS.size - 1) {
            return 0 // Max level
        }
        return LEVEL_THRESHOLDS[currentLevel]
    }

    /**
     * Calculate progress percentage toward next level (0-100)
     */
    fun getProgressToNextLevel(xp: Int = getCurrentXP()): Int {
        val currentLevel = getCurrentLevel(xp)

        if (currentLevel >= LEVEL_THRESHOLDS.size - 1) {
            return 100 // Max level
        }

        val currentLevelThreshold = if (currentLevel == 0) 0 else LEVEL_THRESHOLDS[currentLevel - 1]
        val nextLevelThreshold = LEVEL_THRESHOLDS[currentLevel]

        val xpInCurrentLevel = xp - currentLevelThreshold
        val xpNeededForNextLevel = nextLevelThreshold - currentLevelThreshold

        return ((xpInCurrentLevel.toFloat() / xpNeededForNextLevel) * 100).toInt()
    }

    /**
     * Update the progress bar with current progress
     */
    fun updateProgressBar(progressBar: ProgressBar?, xp: Int = getCurrentXP()) {
        progressBar?.let {
            val progress = getProgressToNextLevel(xp)
            it.progress = progress
        }
    }

    /**
     * Update the level text with current level
     */
    fun updateLevelText(levelText: TextView?, level: Int = getCurrentLevel()) {
        levelText?.let {
            it.text = "Level $level"
        }
    }

    /**
     * Check if a badge is unlocked
     */
    fun isBadgeUnlocked(badgeId: String): Boolean {
        return prefs.getBoolean("badge_$badgeId", false)
    }

    /**
     * Get all unlocked badges
     */
    fun getUnlockedBadges(): List<Badge> {
        return badges.values.filter { isBadgeUnlocked(it.id) }
    }

    /**
     * Unlock a badge and return it
     */
    fun unlockBadge(badgeId: String): Badge? {
        if (badges.containsKey(badgeId) && !isBadgeUnlocked(badgeId)) {
            val badge = badges[badgeId]

            // Check if user has enough XP for this badge
            if (badge != null && getCurrentXP() >= badge.xpRequired) {
                // Set badge as unlocked
                prefs.edit().putBoolean("badge_$badgeId", true).apply()
                return badge
            }
        }
        return null
    }

    /**
     * Helper function to trigger appropriate rewards for an action
     */
    fun trackAction(action: String, activity: AppCompatActivity? = null, progressBar: ProgressBar? = null, levelText: TextView? = null): List<Badge> {
        val unlockedBadges = mutableListOf<Badge>()

        when (action) {
            ACTION_ADD_EXPENSE -> {
                // Add XP
                val levelUp = addXP(XP_EXPENSE_ADDED, progressBar, levelText)

                // Try to unlock the first expense badge
                unlockBadge(BADGE_FIRST_EXPENSE)?.let { badge ->
                    unlockedBadges.add(badge)
                    // Show notification for badge unlock
                    activity?.let {
                        NotificationsManager.showBadgeEarnedNotification(it, badge.name)
                    }
                }

                // If leveled up, show celebration
                if (levelUp) {
                    activity?.let { showLevelUpCelebration(it) }
                }
            }
            ACTION_ADD_INCOME -> {
                val levelUp = addXP(XP_INCOME_ADDED, progressBar, levelText)
                unlockBadge(BADGE_FIRST_INCOME)?.let { badge ->
                    unlockedBadges.add(badge)
                    // Show notification for badge unlock
                    activity?.let {
                        NotificationsManager.showBadgeEarnedNotification(it, badge.name)
                    }
                }
                if (levelUp) {
                    activity?.let { showLevelUpCelebration(it) }
                }
            }
            ACTION_SET_BUDGET -> {
                val levelUp = addXP(XP_SET_BUDGET, progressBar, levelText)
                unlockBadge(BADGE_SET_BUDGET)?.let { badge ->
                    unlockedBadges.add(badge)
                    // Show notification for badge unlock
                    activity?.let {
                        NotificationsManager.showBadgeEarnedNotification(it, badge.name)
                    }
                }
                if (levelUp) {
                    activity?.let { showLevelUpCelebration(it) }
                }
            }
            ACTION_COMPLETE_QUIZ -> {
                val levelUp = addXP(XP_QUIZ_COMPLETED, progressBar, levelText)
                unlockBadge(BADGE_COMPLETE_QUIZ)?.let { badge ->
                    unlockedBadges.add(badge)
                    // Show notification for badge unlock
                    activity?.let {
                        NotificationsManager.showBadgeEarnedNotification(it, badge.name)
                    }
                }
                if (levelUp) {
                    activity?.let { showLevelUpCelebration(it) }
                }
            }
            ACTION_WATCH_VIDEO -> {
                val levelUp = addXP(XP_WATCH_VIDEO, progressBar, levelText)
                unlockBadge(BADGE_WATCH_VIDEO)?.let { badge ->
                    unlockedBadges.add(badge)
                    // Show notification for badge unlock
                    activity?.let {
                        NotificationsManager.showBadgeEarnedNotification(it, badge.name)
                    }
                }
                if (levelUp) {
                    activity?.let { showLevelUpCelebration(it) }
                }
            }
            ACTION_INVITE_FRIEND -> {
                val levelUp = addXP(XP_INVITE_FRIEND, progressBar, levelText)
                unlockBadge(BADGE_INVITE_FRIEND)?.let { badge ->
                    unlockedBadges.add(badge)
                    // Show notification for badge unlock
                    activity?.let {
                        NotificationsManager.showBadgeEarnedNotification(it, badge.name)
                    }
                }
                if (levelUp) {
                    activity?.let { showLevelUpCelebration(it) }
                }
            }
        }

        // If badges were unlocked, show badge unlock animation
        if (unlockedBadges.isNotEmpty() && activity != null) {
            showBadgeUnlockCelebration(activity, unlockedBadges.first())
        }

        return unlockedBadges
    }

    /**
     * Show level up celebration animation
     */
    private fun showLevelUpCelebration(activity: AppCompatActivity) {
        val levelUpDialog = LevelUpDialog(activity, getCurrentLevel())
        levelUpDialog.show()
    }

    /**
     * Show badge unlock celebration animation
     */
    private fun showBadgeUnlockCelebration(activity: AppCompatActivity, badge: Badge) {
        val badgeUnlockDialog = BadgeUnlockDialog(activity, badge)
        badgeUnlockDialog.show()
    }

    /**
     * Reset all gamification data (for testing)
     */
    fun resetGamificationData() {
        prefs.edit().clear().apply()
    }

    // Data class for badges
    data class Badge(
        val id: String,
        val name: String,
        val description: String,
        val iconResourceId: Int,
        val xpRequired: Int
    )
}