package com.teamvault.budgetvault

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView

/**
 * Dialog shown when a user levels up
 */
class LevelUpDialog(
    context: Context,
    private val newLevel: Int
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up dialog appearance
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_level_up)
        setCancelable(false)

        // Set transparent background to use custom rounded corners
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog width to match parent
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = layoutParams

        // Set up the views
        val levelUpAnimation = findViewById<LottieAnimationView>(R.id.levelUpAnimation)
        val levelText = findViewById<TextView>(R.id.levelText)
        val levelMessageText = findViewById<TextView>(R.id.levelMessageText)
        val closeButton = findViewById<Button>(R.id.closeButton)

        // Set the level text
        levelText.text = "Level $newLevel"

        // Set the level message based on the new level
        val message = when (newLevel) {
            2 -> "Great start! You're on your way to financial mastery!"
            3 -> "Impressive! You're making great progress!"
            4 -> "Amazing! You're becoming a budget expert!"
            5 -> "Fantastic! You're nearly a budget master!"
            6 -> "Incredible! You've reached the highest level!"
            else -> "Congratulations on reaching the next level!"
        }
        levelMessageText.text = message

        // Start the level up animation
        levelUpAnimation.playAnimation()

        // Close button
        closeButton.setOnClickListener {
            dismiss()
        }
    }
}