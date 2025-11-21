package com.teamvault.budgetvault

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.firebase.FirebaseApp
import com.teamvault.budgetvault.data.database.AppDatabase
import com.teamvault.budgetvault.data.firebase.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignInActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var db: AppDatabase
    private lateinit var authService: AuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth Service
        authService = AuthService()

        // Temporary test
        Log.d("Firebase", "Firebase initialized: ${FirebaseApp.getInstance() != null}")

        // Initialize fields
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        signInButton = findViewById(R.id.signUpButton)

        // Initialize database
        db = AppDatabase.getDatabase(this)

        // Handle sign in button click
        signInButton.setOnClickListener {
            signInUser()
        }

        // Navigation logic
        val forgotPassword: TextView = findViewById(R.id.forgotPassword)
        forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        val signupBtn: TextView = findViewById(R.id.signup)
        signupBtn.setOnClickListener {
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
        }

        val backBtn: ImageView = findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }

        // Makes the user to have 7 days trial if they sign in as guest
        val signInGuestBtn: Button = findViewById(R.id.signInGuestButton)
        signInGuestBtn.setOnClickListener {
            val sharedPref = getSharedPreferences("BudgetVaultPrefs", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putLong("guestStartDate", System.currentTimeMillis())
            editor.apply()

            val intent = Intent(this, DashboardActivity::class.java)
            intent.putExtra("isGuest", true)
            startActivity(intent)
            finish()
        }
    }

    private fun signInUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Try Firebase authentication first
            val firebaseResult = authService.signIn(email, password)

            if (firebaseResult.isSuccess) {
                // Firebase auth successful
                val firebaseUser = firebaseResult.getOrNull()
                Log.d("SignIn", "Firebase auth successful for: ${firebaseUser?.email}")

                // Save user ID for expense tracking
                val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
                sharedPreferences.edit()
                    .putString("userId", firebaseUser?.uid)
                    .apply()

                Toast.makeText(this@SignInActivity, "Signed in successfully!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@SignInActivity, DashboardActivity::class.java)
                intent.putExtra("user_full_name", firebaseUser?.displayName ?: email)
                startActivity(intent)
                finish()
            } else {
                // Firebase failed, try local Room authentication
                Log.d("SignIn", "Firebase auth failed, trying local auth: ${firebaseResult.exceptionOrNull()?.message}")

                val user = withContext(Dispatchers.IO) {
                    db.userDao().getUserByEmail(email)
                }

                if (user != null && BCrypt.verifyer().verify(password.toCharArray(), user.password).verified) {
                    // Local auth successful
                    val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
                    sharedPreferences.edit()
                        .putString("userId", user.id.toString()) // Use local ID
                        .apply()

                    Toast.makeText(this@SignInActivity, "Signed in successfully (offline)!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@SignInActivity, DashboardActivity::class.java)
                    intent.putExtra("user_full_name", user.fullName)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@SignInActivity, "Invalid email or password.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}