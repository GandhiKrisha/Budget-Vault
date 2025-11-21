package com.teamvault.budgetvault

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the saved language before setting up the view
        LanguageManager.applyLanguage(this)

        // Also apply the saved theme
        ThemeManager.applyTheme(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Initialize buttons
        val notificationsButton = findViewById<Button>(R.id.notificationsButton)
        val inviteFriendButton = findViewById<Button>(R.id.inviteFriendButton)
        val changeCurrencyButton = findViewById<Button>(R.id.changeCurrencyButton)
        val changeLanguageButton = findViewById<Button>(R.id.changeLanguageButton)
        val changeThemeButton = findViewById<Button>(R.id.changeThemeButton)

        // Set up button click listeners
        notificationsButton.setOnClickListener {
            // Show notifications toggle dialog
            showNotificationsDialog()
        }

        inviteFriendButton.setOnClickListener {
            // Show invite friend dialog
            showInviteFriendOptions()
        }

        changeCurrencyButton.setOnClickListener {
            // Show currency selection dialog
            showCurrencySelectionDialog()
        }

        changeLanguageButton.setOnClickListener {
            // Show language selection dialog
            showLanguageSelectionDialog()
        }

        changeThemeButton.setOnClickListener {
            // Show theme toggle dialog
            showThemeToggleDialog()
        }
    }

    private fun showLanguageSelectionDialog() {
        // Create the dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_language_selection)

        // Set dialog width to match parent
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = layoutParams

        // Set background to transparent for rounded corners and dim amount for blur effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f) // This creates the dim/blur effect

        // Set up language button click listeners
        dialog.findViewById<Button>(R.id.englishButton).setOnClickListener {
            changeAppLanguage("en")
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.spanishButton).setOnClickListener {
            changeAppLanguage("es")
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.frenchButton).setOnClickListener {
            changeAppLanguage("fr")
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.zuluButton).setOnClickListener {
            changeAppLanguage("zu")
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.afrikaansButton).setOnClickListener {
            changeAppLanguage("af")
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.cancelLanguageButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Method to change app language
    private fun changeAppLanguage(languageCode: String) {
        // Use LanguageManager to update language
        LanguageManager.updateLanguage(this, languageCode)

        // Show confirmation toast
        val languageName = when(languageCode) {
            "en" -> "English"
            "es" -> "Spanish"
            "fr" -> "French"
            "zu" -> "Zulu"
            "af" -> "Afrikaans"
            else -> "English"
        }
        Toast.makeText(this, "Language changed to $languageName", Toast.LENGTH_SHORT).show()

        // Restart the app
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    // Method for theme toggle dialog
    private fun showThemeToggleDialog() {
        // Create the dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_theme_toggle)

        // Set dialog width to match parent
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = layoutParams

        // Set background to transparent for rounded corners and dim amount for blur effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f)

        // Get the theme toggle switch
        val themeToggle = dialog.findViewById<Switch>(R.id.themeToggleSwitch)
        val themeTitle = dialog.findViewById<TextView>(R.id.themeTitleText)

        // Set initial state based on current theme
        val isDarkTheme = ThemeManager.isDarkTheme(this)
        themeToggle.isChecked = isDarkTheme
        updateThemeTitleText(themeTitle, isDarkTheme)

        // Set up theme switch listener
        themeToggle.setOnCheckedChangeListener { _, isChecked ->
            updateThemeTitleText(themeTitle, isChecked)
            ThemeManager.setTheme(this, isChecked)
        }

        dialog.show()

        // Auto-dismiss after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 2000) // 2 seconds
    }

    private fun updateThemeTitleText(textView: TextView, isDarkMode: Boolean) {
        textView.text = if (isDarkMode) "Dark mode turned on" else "Light mode turned on"
    }

    // Method for notifications dialog
    private fun showNotificationsDialog() {
        // Create the dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_notifications_toggle)

        // Set dialog width to match parent
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = layoutParams

        // Set background to transparent for rounded corners and dim amount for blur effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f)

        // Get the notifications toggle switch
        val notificationsToggle = dialog.findViewById<Switch>(R.id.notificationsToggleSwitch)

        // Set initial state based on current notifications setting
        val notificationsEnabled = NotificationsManager.areNotificationsEnabled(this)
        notificationsToggle.isChecked = notificationsEnabled

        // Set up notifications switch listener
        notificationsToggle.setOnCheckedChangeListener { _, isChecked ->
            // Update notifications state using NotificationsManager
            NotificationsManager.setNotificationsEnabled(this, isChecked)

            // Display a toast message
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            // Request notification permissions if enabled (for Android 13+)
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // In a real implementation, you would request notification permission here
                // ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            }
        }

        dialog.show()

        // Auto-dismiss after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 2000) // 2 seconds
    }

    // Method for currency selection dialog
    private fun showCurrencySelectionDialog() {
        // Create the dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_currency_selection)

        // Set dialog width to match parent
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = layoutParams

        // Set background to transparent for rounded corners and dim amount for blur effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f)

        // Get the current selected currency
        val currentCurrency = CurrencyManager.getSelectedCurrency(this)

        // Find currency text views
        val currencyINR = dialog.findViewById<TextView>(R.id.currencyINR)
        val currencyZAR = dialog.findViewById<TextView>(R.id.currencyZAR)
        val currencyGBP = dialog.findViewById<TextView>(R.id.currencyGBP)
        val currencyEUR = dialog.findViewById<TextView>(R.id.currencyEUR)
        val currencyCNY = dialog.findViewById<TextView>(R.id.currencyCNY)

        // Update initial UI state based on current selection
        updateCurrencySelection(
            currentCurrency,
            currencyINR, currencyZAR, currencyGBP, currencyEUR, currencyCNY
        )

        // Set click listeners for each currency option
        currencyINR.setOnClickListener {
            selectCurrency(CurrencyManager.CURRENCY_INR, dialog)
        }

        currencyZAR.setOnClickListener {
            selectCurrency(CurrencyManager.CURRENCY_ZAR, dialog)
        }

        currencyGBP.setOnClickListener {
            selectCurrency(CurrencyManager.CURRENCY_GBP, dialog)
        }

        currencyEUR.setOnClickListener {
            selectCurrency(CurrencyManager.CURRENCY_EUR, dialog)
        }

        currencyCNY.setOnClickListener {
            selectCurrency(CurrencyManager.CURRENCY_CNY, dialog)
        }

        dialog.show()

        // Auto-dismiss after selection (handled in selectCurrency method)
    }

    private fun updateCurrencySelection(
        selectedCurrency: String,
        currencyINR: TextView,
        currencyZAR: TextView,
        currencyGBP: TextView,
        currencyEUR: TextView,
        currencyCNY: TextView
    ) {
        // Reset all to unselected state
        currencyINR.setBackgroundResource(R.drawable.currency_button_background)
        currencyINR.setTextColor(resources.getColor(R.color.dark_gray, theme))

        currencyZAR.setBackgroundResource(R.drawable.currency_button_background)
        currencyZAR.setTextColor(resources.getColor(R.color.dark_gray, theme))

        currencyGBP.setBackgroundResource(R.drawable.currency_button_background)
        currencyGBP.setTextColor(resources.getColor(R.color.dark_gray, theme))

        currencyEUR.setBackgroundResource(R.drawable.currency_button_background)
        currencyEUR.setTextColor(resources.getColor(R.color.dark_gray, theme))

        currencyCNY.setBackgroundResource(R.drawable.currency_button_background)
        currencyCNY.setTextColor(resources.getColor(R.color.dark_gray, theme))

        // Set selected state for current currency
        when (selectedCurrency) {
            CurrencyManager.CURRENCY_INR -> {
                currencyINR.setBackgroundResource(R.drawable.currency_button_selected_background)
                currencyINR.setTextColor(resources.getColor(R.color.green, theme))
            }
            CurrencyManager.CURRENCY_ZAR -> {
                currencyZAR.setBackgroundResource(R.drawable.currency_button_selected_background)
                currencyZAR.setTextColor(resources.getColor(R.color.green, theme))
            }
            CurrencyManager.CURRENCY_GBP -> {
                currencyGBP.setBackgroundResource(R.drawable.currency_button_selected_background)
                currencyGBP.setTextColor(resources.getColor(R.color.green, theme))
            }
            CurrencyManager.CURRENCY_EUR -> {
                currencyEUR.setBackgroundResource(R.drawable.currency_button_selected_background)
                currencyEUR.setTextColor(resources.getColor(R.color.green, theme))
            }
            CurrencyManager.CURRENCY_CNY -> {
                currencyCNY.setBackgroundResource(R.drawable.currency_button_selected_background)
                currencyCNY.setTextColor(resources.getColor(R.color.green, theme))
            }
        }
    }

    private fun selectCurrency(currencyCode: String, dialog: Dialog) {
        // Update the currency preference
        CurrencyManager.setSelectedCurrency(this, currencyCode)

        // Show confirmation toast
        val currencyName = when (currencyCode) {
            CurrencyManager.CURRENCY_INR -> "Indian Rupee (₹)"
            CurrencyManager.CURRENCY_ZAR -> "South African Rand (R)"
            CurrencyManager.CURRENCY_GBP -> "British Pound (£)"
            CurrencyManager.CURRENCY_EUR -> "Euro (€)"
            CurrencyManager.CURRENCY_CNY -> "Chinese Yuan (¥)"
            else -> "South African Rand (R)"
        }
        Toast.makeText(this, "Currency changed to $currencyName", Toast.LENGTH_SHORT).show()

        // Dismiss the dialog after selection
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 500) // Dismiss after 0.5 seconds
    }

    // Method to show invite options
    private fun showInviteFriendOptions() {
        // Here you would typically show a share intent to let the user choose
        // how they want to invite their friend (email, messaging app, etc.)
        // app  not yet be ready for the full implementation of inviting friends.

        // For now, we'll just show the success dialog directly
        showInvitationSentDialog()
    }

    // Method to show the success dialog
    private fun showInvitationSentDialog() {
        // Create the dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_invite_success)

        // Set dialog width to match parent
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = layoutParams

        // Set background to transparent for rounded corners and dim amount for blur effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f)

        dialog.show()

        // Auto-dismiss after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 2000) // 2 seconds
    }
}