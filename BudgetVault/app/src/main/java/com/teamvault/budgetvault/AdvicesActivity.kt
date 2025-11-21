package com.teamvault.budgetvault

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdvicesActivity : BaseActivity() {

    // Gamification manager
    private lateinit var gamificationManager: GamificationManager

    // Track watched videos and listened podcasts to prevent duplicate XP
    private val watchedVideos = HashSet<String>()
    private val listenedPodcasts = HashSet<String>()

    // Video and podcast URLs for tracking
    private val VIDEO_URL_1 = "https://www.youtube.com/watch?v=sVKQn2I4HDM"
    private val VIDEO_URL_2 = "https://youtu.be/T_776Cwvejs?si=F2RCxcjPqOIDGgwG"
    private val VIDEO_URL_3 = "https://youtu.be/iOsEo3u85cA?si=ddCR8KTn1gWK0Qns"
    private val PODCAST_URL_1 = "https://open.spotify.com/show/0784MlYJkguddHefDmCyxK"
    private val PODCAST_URL_2 = "https://open.spotify.com/show/0H1u8pqYrvz6zj6noHbkB3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_advices)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize gamification manager
        gamificationManager = GamificationManager.getInstance(this)

        // Load watched/listened states from preferences
        loadWatchedStates()

        // Initialize back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set up video buttons
        setupVideoButtons()

        // Set up podcast buttons
        setupPodcastButtons()

        // Set up view all links
        setupViewAllLinks()

        // Set up bottom navigation
        setupBottomNavigation()
    }

    // Load watched states from SharedPreferences
    private fun loadWatchedStates() {
        val prefs = getSharedPreferences("AdvicesActivityPrefs", MODE_PRIVATE)

        if (prefs.getBoolean("watched_$VIDEO_URL_1", false)) watchedVideos.add(VIDEO_URL_1)
        if (prefs.getBoolean("watched_$VIDEO_URL_2", false)) watchedVideos.add(VIDEO_URL_2)
        if (prefs.getBoolean("watched_$VIDEO_URL_3", false)) watchedVideos.add(VIDEO_URL_3)

        if (prefs.getBoolean("listened_$PODCAST_URL_1", false)) listenedPodcasts.add(PODCAST_URL_1)
        if (prefs.getBoolean("listened_$PODCAST_URL_2", false)) listenedPodcasts.add(PODCAST_URL_2)
    }

    // Save watched states to SharedPreferences
    private fun saveWatchedState(type: String, url: String) {
        val prefs = getSharedPreferences("AdvicesActivityPrefs", MODE_PRIVATE)
        val editor = prefs.edit()

        when (type) {
            "video" -> editor.putBoolean("watched_$url", true)
            "podcast" -> editor.putBoolean("listened_$url", true)
        }

        editor.apply()
    }


    //It sets up the video Button.
    private fun setupVideoButtons() {
        val watchVideo1 = findViewById<Button>(R.id.watchVideo1)
        val watchVideo2 = findViewById<Button>(R.id.watchVideo2)
        val watchVideo3 = findViewById<Button>(R.id.watchVideo3)

        watchVideo1.setOnClickListener {
            openYoutubeLink(VIDEO_URL_1)
        }

        watchVideo2.setOnClickListener {
            openYoutubeLink(VIDEO_URL_2)
        }

        watchVideo3.setOnClickListener {
            openYoutubeLink(VIDEO_URL_3)
        }
    }

    // method to open youtube link
    private fun openYoutubeLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)

            // For gamification, we'll show a dialog when they return to the app
            // This is handled in onResume to track when they come back from watching
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open video link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPodcastButtons() {
        val listenPodcast1 = findViewById<Button>(R.id.listenPodcast1)
        val listenPodcast2 = findViewById<Button>(R.id.listenPodcast2)

        listenPodcast1.setOnClickListener {
            openSpotifyOrBrowser(PODCAST_URL_1)
        }

        listenPodcast2.setOnClickListener {
            openSpotifyOrBrowser(PODCAST_URL_2)
        }
    }

    private fun openSpotifyOrBrowser(spotifyUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(spotifyUrl)
        intent.setPackage("com.spotify.music")

        try {
            startActivity(intent)
            // For gamification, we'll show a dialog when they return to the app
            // This is handled in onResume to track when they come back from listening
        } catch (e: ActivityNotFoundException) {
            // Spotify not installed — fallback to browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl))
            startActivity(browserIntent)
        }
    }

    private fun setupViewAllLinks() {
        val viewAllVideos = findViewById<TextView>(R.id.viewAllVideos)
        val viewAllPodcasts = findViewById<TextView>(R.id.viewAllPodcasts)

        viewAllVideos.setOnClickListener {
            // Navigate to all videos screen
            val intent = Intent(this, AllVideosActivity::class.java)
            startActivity(intent)
        }

        viewAllPodcasts.setOnClickListener {
            // Navigate to all podcasts screen
            val intent = Intent(this, AllPodcastsActivity::class.java)
            startActivity(intent)
        }
    }

    // Track when a video is watched
    private fun trackVideoWatched(url: String) {
        // Only give XP if this video hasn't been watched before
        if (!watchedVideos.contains(url)) {
            // Add to watched set
            watchedVideos.add(url)

            // Save to preferences
            saveWatchedState("video", url)

            // Track the action
            val unlockedBadges = gamificationManager.trackAction(
                GamificationManager.ACTION_WATCH_VIDEO,
                this,
                null,
                null
            )

            // Show completion dialog
            showContentCompletionDialog("video", unlockedBadges.isNotEmpty())
        }
    }

    // Track when a podcast is listened to
    private fun trackPodcastListened(url: String) {
        // Only give XP if this podcast hasn't been listened to before
        if (!listenedPodcasts.contains(url)) {
            // Add to listened set
            listenedPodcasts.add(url)

            // Save to preferences
            saveWatchedState("podcast", url)

            // Track the action
            val unlockedBadges = gamificationManager.trackAction(
                GamificationManager.ACTION_WATCH_VIDEO, // Using same action as videos - both are educational content
                this,
                null,
                null
            )

            // Show completion dialog
            showContentCompletionDialog("podcast", unlockedBadges.isNotEmpty())
        }
    }

    // Show a dialog after completion
    private fun showContentCompletionDialog(type: String, badgeUnlocked: Boolean) {
        val contentType = if (type == "video") "video" else "podcast episode"

        val message = if (badgeUnlocked) {
            "Great job finishing that $contentType! You earned 20 XP and unlocked a new badge!"
        } else {
            "Great job finishing that $contentType! You earned 20 XP!"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Financial Education Reward")
            .setMessage(message)
            .setPositiveButton("Great!") { _, _ -> }
            .create()

        dialog.show()
    }

    // Override onResume to check for video/podcast completion
    override fun onResume() {
        super.onResume()

        // Show dialog asking if they completed the content
        // This is a simplification since we can't directly track YouTube/Spotify completion
        val prefs = getSharedPreferences("AdvicesActivityWatchTracking", MODE_PRIVATE)
        val lastOpenedUrl = prefs.getString("last_opened_url", null)

        if (lastOpenedUrl != null) {
            // Clear the last opened URL first to prevent repeated dialogs
            prefs.edit().remove("last_opened_url").apply()

            val contentType = if (lastOpenedUrl.contains("youtube")) "video" else "podcast"

            // Ask if they finished watching/listening
            AlertDialog.Builder(this)
                .setTitle("Did you finish?")
                .setMessage("Did you finish the $contentType? You'll earn XP for completing it!")
                .setPositiveButton("Yes, I finished it") { _, _ ->
                    if (contentType == "video") {
                        trackVideoWatched(lastOpenedUrl)
                    } else {
                        trackPodcastListened(lastOpenedUrl)
                    }
                }
                .setNegativeButton("Not yet", null)
                .create()
                .show()
        }
    }

    // Override onPause to save the last opened URL
    override fun onPause() {
        super.onPause()

        // This is called right before the activity is no longer visible
        // We'll save any URL that was just opened
        val lastIntent = intent
        if (lastIntent?.action == Intent.ACTION_VIEW) {
            val urlData = lastIntent.data?.toString()
            if (urlData != null) {
                val prefs = getSharedPreferences("AdvicesActivityWatchTracking", MODE_PRIVATE)
                prefs.edit().putString("last_opened_url", urlData).apply()
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_advices  // Highlight advices icon

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    // Navigate to dashboard
                    val intent = Intent(this,DashboardActivity::class.java)
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
                    // Already on advices screen
                    true
                }
                R.id.navigation_more -> {
                    // Navigate to more screen
                    val intent = Intent(this, MoreActivity::class.java)
                    startActivity(intent)
                    finish()
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

        // Set up button click listeners with result codes for gamification
        dialog.findViewById<Button>(R.id.addIncomeButton).setOnClickListener {
            // Handle add income action with result code
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivityForResult(intent, DashboardActivity.REQUEST_CODE_ADD_INCOME)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
            // Handle add expense action with result code
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivityForResult(intent, DashboardActivity.REQUEST_CODE_ADD_EXPENSE)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            // Just dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }
}