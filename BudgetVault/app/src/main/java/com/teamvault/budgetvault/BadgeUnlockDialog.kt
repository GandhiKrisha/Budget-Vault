package com.teamvault.budgetvault

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

/**
 * Dialog shown when a user unlocks a new badge
 */
class BadgeUnlockDialog(
    context: Context,
    private val badge: GamificationManager.Badge
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up dialog appearance
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_badge_unlock)
        setCancelable(false)

        // Set transparent background to use custom rounded corners
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog width to match parent
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = layoutParams

        // Set up the views
        val badgeIconView = findViewById<ImageView>(R.id.badgeIcon)
        val badgeTitleView = findViewById<TextView>(R.id.badgeTitle)
        val badgeDescriptionView = findViewById<TextView>(R.id.badgeDescription)
        val confettiAnimation = findViewById<LottieAnimationView>(R.id.confettiAnimation)
        val closeButton = findViewById<Button>(R.id.closeButton)

        // Set the badge data
        badgeIconView.setImageResource(badge.iconResourceId)
        badgeTitleView.text = badge.name
        badgeDescriptionView.text = badge.description

        // Start the confetti animation
        confettiAnimation.playAnimation()

        // Close button
        closeButton.setOnClickListener {
            dismiss()
        }
    }
}