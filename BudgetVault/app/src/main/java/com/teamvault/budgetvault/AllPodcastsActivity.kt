package com.teamvault.budgetvault

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AllPodcastsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_podcasts)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, AdvicesActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set up all podcast buttons
        setupAllPodcastButtons()
    }

    private fun setupAllPodcastButtons() {

        val listenPodcast1 = findViewById<Button>(R.id.listenPodcast1)
        val listenPodcast2 = findViewById<Button>(R.id.listenPodcast2)
        val listenPodcast3 = findViewById<Button>(R.id.listenPodcast3)
        val listenPodcast4 = findViewById<Button>(R.id.listenPodcast4)
        val listenPodcast5 = findViewById<Button>(R.id.listenPodcast5)
        val listenPodcast6 = findViewById<Button>(R.id.listenPodcast6)
        val listenPodcast7 = findViewById<Button>(R.id.listenPodcast7)

        // Set click listeners for original podcasts
        listenPodcast1.setOnClickListener {
            openSpotifyOrBrowser("https://open.spotify.com/show/0784MlYJkguddHefDmCyxK")
        }

        listenPodcast2.setOnClickListener {
            openSpotifyOrBrowser("https://open.spotify.com/show/0H1u8pqYrvz6zj6noHbkB3")
        }

        // Set click listeners for additional podcasts
        listenPodcast3.setOnClickListener {
            openSpotifyOrBrowser("https://open.spotify.com/show/7yJ2UBhpMhLaI0ASdbKO91")
        }

        listenPodcast4.setOnClickListener {
            openSpotifyOrBrowser("https://open.spotify.com/show/0onjxuheqNMfUHem407NcQ")
        }

        listenPodcast5.setOnClickListener {
            openSpotifyOrBrowser("https://open.spotify.com/show/24w9rYGFzEWdUxQSNhCAan")
        }

        listenPodcast6.setOnClickListener {
            openSpotifyOrBrowser("https://open.spotify.com/show/4ZDUPYjz7R8weAtUqH8nHz")
        }

        listenPodcast7.setOnClickListener {
            openSpotifyOrBrowser("https://open.spotify.com/show/5exfRPDNCBHmntEkJrlLmX")
        }
    }

    private fun openSpotifyOrBrowser(spotifyUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(spotifyUrl)
        intent.setPackage("com.spotify.music")

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Spotify not installed — fallback to browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl))
            startActivity(browserIntent)
        }
    }
}