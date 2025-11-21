package com.teamvault.budgetvault

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

class AllVideosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_videos)
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

        // Set up all video buttons
        setupAllVideoButtons()
    }

    private fun setupAllVideoButtons() {
        // Original 3 videos
        val watchVideo1 = findViewById<Button>(R.id.watchVideo1)
        val watchVideo2 = findViewById<Button>(R.id.watchVideo2)
        val watchVideo3 = findViewById<Button>(R.id.watchVideo3)

        // Additional 5 videos
        val watchVideo4 = findViewById<Button>(R.id.watchVideo4)
        val watchVideo5 = findViewById<Button>(R.id.watchVideo5)
        val watchVideo6 = findViewById<Button>(R.id.watchVideo6)
        val watchVideo7 = findViewById<Button>(R.id.watchVideo7)
        val watchVideo8 = findViewById<Button>(R.id.watchVideo8)

        // Set click listeners for original videos
        watchVideo1.setOnClickListener {
            openYoutubeLink("https://www.youtube.com/watch?v=sVKQn2I4HDM")
        }

        watchVideo2.setOnClickListener {
            openYoutubeLink("https://youtu.be/T_776Cwvejs?si=F2RCxcjPqOIDGgwG")
        }

        watchVideo3.setOnClickListener {
            openYoutubeLink("https://youtu.be/iOsEo3u85cA?si=ddCR8KTn1gWK0Qns")
        }

        // Set click listeners for additional videos
        watchVideo4.setOnClickListener {
            openYoutubeLink("https://www.youtube.com/watch?v=xfPbT7HPkKA")
        }

        watchVideo5.setOnClickListener {
            openYoutubeLink("https://www.youtube.com/watch?v=2IUBveJXaa0")
        }

        watchVideo6.setOnClickListener {
            openYoutubeLink("https://www.youtube.com/watch?v=8LsD73abBYo")
        }

        watchVideo7.setOnClickListener {
            openYoutubeLink("https://youtu.be/CbhjhWleKGE?si=LmAHkUhvNd-6DGmo")
        }

        watchVideo8.setOnClickListener {
            openYoutubeLink("https://www.youtube.com/watch?v=NEzqHbtGa9U")
        }
    }

    private fun openYoutubeLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open video link", Toast.LENGTH_SHORT).show()
        }
    }
}