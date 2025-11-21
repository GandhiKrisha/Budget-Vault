package com.teamvault.budgetvault

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * FeedbackActivity handles user feedback collection and submission
 * This activity allows users to submit feedback via email
 */
class FeedbackActivity : AppCompatActivity() {
    // Email address to receive feedback
    private val FEEDBACK_EMAIL = "budgetvault14@gmail.com"

    // UI Components
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var subjectInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var submitBtn: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback)

        // Apply window insets for proper edge-to-edge display
        setupWindowInsets()

        // Initialize UI components
        initializeViews()

        // Set up click listeners
        setupClickListeners()
    }

    /**
     * Set up window insets for edge-to-edge display
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        subjectInput = findViewById(R.id.subjectInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        submitBtn = findViewById(R.id.SubmitBtn)
    }

    /**
     * Set up click listeners for buttons
     */
    private fun setupClickListeners() {
        // Set up back button click listener
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            navigateToMoreActivity()
        }

        // Set up submit button click listener
        submitBtn.setOnClickListener {
            if (validateInputs()) {
                sendFeedbackEmail()
                showSuccessDialog()
            }
        }
    }

    /**
     * Navigate back to the MoreActivity
     */
    private fun navigateToMoreActivity() {
        val intent = Intent(this, MoreActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Validate all input fields before submission
     * @return Boolean indicating if all inputs are valid
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            nameInput.error = "Name cannot be empty"
            isValid = false
        }

        // Validate email
        val email = emailInput.text.toString().trim()
        if (email.isEmpty()) {
            emailInput.error = "Email cannot be empty"
            isValid = false
        } else if (!isValidEmail(email)) {
            emailInput.error = "Invalid email format"
            isValid = false
        }

        // Validate subject
        val subject = subjectInput.text.toString().trim()
        if (subject.isEmpty()) {
            subjectInput.error = "Subject cannot be empty"
            isValid = false
        }

        // Validate message (optional validation)
        val message = descriptionInput.text.toString().trim()
        if (message.isEmpty()) {
            descriptionInput.error = "Please provide feedback details"
            isValid = false
        }

        return isValid
    }

    /**
     * Check if email format is valid
     * @param email Email string to validate
     * @return Boolean indicating if email format is valid
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Send feedback via email
     */
    private fun sendFeedbackEmail() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val subject = subjectInput.text.toString().trim()
        val message = descriptionInput.text.toString().trim()

        // Create email intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback: $subject")
            putExtra(Intent.EXTRA_TEXT, createEmailBody(name, email, message))
        }

        try {
            startActivity(Intent.createChooser(intent, "Send feedback via..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Create formatted email body
     * @param name User's name
     * @param email User's email
     * @param message Feedback message
     * @return Formatted email body
     */
    private fun createEmailBody(name: String, email: String, message: String): String {
        return """
            Name: $name
            Email: $email
            
            Message:
            $message
            """.trimIndent()
    }

    /**
     * Reset all form fields
     */
    private fun resetForm() {
        nameInput.text?.clear()
        emailInput.text?.clear()
        subjectInput.text?.clear()
        descriptionInput.text?.clear()

        // Clear any error states
        nameInput.error = null
        emailInput.error = null
        subjectInput.error = null
        descriptionInput.error = null
    }

    /**
     * Show success dialog after feedback submission
     */
    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_feedback_success)

        // Set dialog properties for better appearance
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        // Show dialog for a short time, then dismiss and reset form
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            resetForm()
        }, 2000) // 2 seconds delay for better user experience

        dialog.show()
    }
}