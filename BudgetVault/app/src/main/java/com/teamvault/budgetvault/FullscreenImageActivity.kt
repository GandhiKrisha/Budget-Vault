package com.teamvault.budgetvault

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullscreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        val imageView: ImageView = findViewById(R.id.fullscreenImageView)
        val photoUri = intent.getStringExtra("photoUri")

        if (photoUri != null) {
            Glide.with(this).load(photoUri).into(imageView)
        } else {
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
