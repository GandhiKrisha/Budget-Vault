package com.teamvault.budgetvault

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import at.favre.lib.crypto.bcrypt.BCrypt
import com.teamvault.budgetvault.data.database.AppDatabase
import com.teamvault.budgetvault.data.firebase.AuthService
import com.teamvault.budgetvault.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var signUpButton: Button

    private lateinit var db: AppDatabase
    private lateinit var authService: AuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth Service
        authService = AuthService()

        // buttons listener
        val signInClick: TextView = findViewById(R.id.signin)
        signInClick.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        val backBtn: ImageView = findViewById(R.id.backBtn)
        backBtn.isClickable = true
        backBtn.setOnClickListener {
            finish()
        }

        // Initialize form fields
        fullNameEditText = findViewById(R.id.fullName)
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        termsCheckbox = findViewById(R.id.termsCheckbox)
        signUpButton = findViewById(R.id.signUpButton)

        // Initialize database
        db = AppDatabase.getDatabase(this)

        // Handle sign up button click
        signUpButton.setOnClickListener {
            signUpUser()
        }

        // "Sign in as Guest" listener
        val signInGuest: TextView = findViewById(R.id.signInGuestButton)
        signInGuest.setOnClickListener {
            val sharedPref = getSharedPreferences("BudgetVaultPrefs", MODE_PRIVATE)
            val editor = sharedPref.edit()
            val currentTime = System.currentTimeMillis()

            // Save guest start time only if it doesn't exist yet
            if (sharedPref.getLong("guestStartDate", 0L) == 0L) {
                editor.putLong("guestStartDate", currentTime)
                editor.apply()
            }

            // Go to Dashboard with Guest Flag
            val intent = Intent(this, DashboardActivity::class.java)
            intent.putExtra("isGuest", true)
            startActivity(intent)
            finish()
        }
    }

    private fun signUpUser() {
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        // Basic field checks
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Email validation
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            return
        }

        // Password strength validation
        val passwordPattern = Regex("^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$")
        if (!passwordPattern.matches(password)) {
            Toast.makeText(
                this,
                "Password must be at least 8 characters long, contain a digit, an uppercase letter, and a special character.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Password confirmation check
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        // Terms agreement
        if (!termsCheckbox.isChecked) {
            Toast.makeText(this, "Please accept the Terms and Conditions.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Try Firebase first
            val firebaseResult = authService.signUp(email, password)

            if (firebaseResult.isSuccess) {
                // Firebase signup successful
                val firebaseUser = firebaseResult.getOrNull()
                Log.d("SignUp", "Firebase signup successful for: ${firebaseUser?.email}")

                // Also create local user record for offline access
                val bcryptHashString = BCrypt.withDefaults().hashToString(12, password.toCharArray())
                val newUser = User(
                    fullName = fullName,
                    email = email,
                    password = bcryptHashString
                    // Note: We can add firebaseUid field to User model later if needed
                )

                withContext(Dispatchers.IO) {
                    db.userDao().insertUser(newUser)
                }

                Toast.makeText(this@CreateAccountActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@CreateAccountActivity, SignInActivity::class.java))
                finish()
            } else {
                // Firebase failed, create local account only
                Log.d("SignUp", "Firebase signup failed, creating local account: ${firebaseResult.exceptionOrNull()?.message}")

                // Check if user already exists locally
                val existingUser = withContext(Dispatchers.IO) {
                    db.userDao().getUserByEmail(email)
                }

                if (existingUser != null) {
                    Toast.makeText(this@CreateAccountActivity, "An account with this email already exists.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val bcryptHashString = BCrypt.withDefaults().hashToString(12, password.toCharArray())
                val newUser = User(
                    fullName = fullName,
                    email = email,
                    password = bcryptHashString
                )

                withContext(Dispatchers.IO) {
                    db.userDao().insertUser(newUser)
                }

                Toast.makeText(this@CreateAccountActivity, "Account created locally!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@CreateAccountActivity, SignInActivity::class.java))
                finish()
            }
        }
    }
}