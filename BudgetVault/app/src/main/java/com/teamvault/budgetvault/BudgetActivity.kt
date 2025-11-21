package com.teamvault.budgetvault

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class BudgetActivity : BaseActivity() {

    private lateinit var categoriesContainer: LinearLayout
    private lateinit var selectedCategoryText: TextView
    private lateinit var minSpendingInput: EditText
    private lateinit var maxSpendingInput: EditText
    private lateinit var descriptionInput: EditText

    // Gamification manager
    private lateinit var gamificationManager: GamificationManager

    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize gamification manager
        gamificationManager = GamificationManager.getInstance(this)

        // Initialize UI elements
        initializeViews()

        // Set up category buttons
        setupCategoryButtons()

        // Set up action buttons
        setupActionButtons()

        // Set up bottom navigation
        setupBottomNavigation()

        // Load existing budget settings if available
        loadExistingBudgetSettings()
    }

    private fun loadExistingBudgetSettings() {
        val sharedPreferences = getSharedPreferences("BudgetVaultPrefs", MODE_PRIVATE)
        val savedCategory = sharedPreferences.getString("selectedCategory", null)
        val minSpending = sharedPreferences.getFloat("minSpending", 0.0f)
        val maxSpending = sharedPreferences.getFloat("maxSpending", 0.0f)

        if (savedCategory != null) {
            // Find the button for this category and select it
            when (savedCategory) {
                "Food" -> findViewById<Button>(R.id.foodCategoryButton)?.let { selectCategory("Food", it) }
                "Transport" -> findViewById<Button>(R.id.transportCategoryButton)?.let { selectCategory("Transport", it) }
                "Entertainment" -> findViewById<Button>(R.id.entertainmentCategoryButton)?.let { selectCategory("Entertainment", it) }
                "Shopping" -> findViewById<Button>(R.id.shoppingCategoryButton)?.let { selectCategory("Shopping", it) }
                else -> {
                    // For custom categories, try to find the button or don't select anything
                    selectedCategory = savedCategory
                    selectedCategoryText.text = savedCategory
                    selectedCategoryText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                }
            }

            // Set the input values
            minSpendingInput.setText(String.format("%.2f", minSpending))
            maxSpendingInput.setText(String.format("%.2f", maxSpending))
        }
    }

    private fun initializeViews() {
        categoriesContainer = findViewById(R.id.categoriesContainer)
        selectedCategoryText = findViewById(R.id.selectedCategoryText)
        minSpendingInput = findViewById(R.id.minSpendingInput)
        maxSpendingInput = findViewById(R.id.maxSpendingInput)
        descriptionInput = findViewById(R.id.descriptionInput)

        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.backBtn)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupCategoryButtons() {
        val foodButton = findViewById<Button>(R.id.foodCategoryButton)
        val transportButton = findViewById<Button>(R.id.transportCategoryButton)
        val entertainmentButton = findViewById<Button>(R.id.entertainmentCategoryButton)
        val shoppingButton = findViewById<Button>(R.id.shoppingCategoryButton)
        val addCategoryButton = findViewById<Button>(R.id.addCategoryButton)

        // Set up category selection
        foodButton.setOnClickListener { selectCategory("Food", foodButton) }
        transportButton.setOnClickListener { selectCategory("Transport", transportButton) }
        entertainmentButton.setOnClickListener { selectCategory("Entertainment", entertainmentButton) }
        shoppingButton.setOnClickListener { selectCategory("Shopping", shoppingButton) }

        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val categoryNameInput = dialogView.findViewById<EditText>(R.id.categoryNameInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val categoryName = categoryNameInput.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    addNewCategory(categoryName)
                } else {
                    Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addNewCategory(categoryName: String) {
        // Create a new category button
        val newCategoryButton = Button(this).apply {
            text = categoryName

            // Set explicit padding and styling
            setPadding(16, 0, 16, 0)

            // Use black text color
            setTextColor(Color.BLACK)

            // Create a rounded shape for the button
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.cornerRadius = dpToPx(20).toFloat() // Rounded corners
            shape.setColor(Color.parseColor("#F0F0F0")) // Your original color from XML
            background = shape

            // Set layout parameters directly
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(40) // 40dp height to match other buttons
            ).apply {
                marginEnd = dpToPx(8) // 8dp right margin
            }

            // Set click listener for the new button
            setOnClickListener {
                selectCategory(categoryName, this)
            }
        }

        // Add the new button to the container after the + button
        categoriesContainer.addView(newCategoryButton, 1)

        // Force layout refresh
        categoriesContainer.requestLayout()

        // Wait for button to be properly added before selecting it
        newCategoryButton.post {
            // Automatically select the new category
            selectCategory(categoryName, newCategoryButton)
        }

        // Show confirmation
        Toast.makeText(this, "Category '$categoryName' added", Toast.LENGTH_SHORT).show()
    }

    // Helper method to convert dp to pixels
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun selectCategory(category: String, button: Button) {
        // Reset all buttons to default style
        resetCategoryButtons()

        // Set selected button style
        if (button.background is GradientDrawable) {
            // For dynamically created buttons with GradientDrawable
            (button.background as GradientDrawable).setColor(Color.parseColor("#CDCDCD")) // Slightly darker to show selection
        } else {
            // For regular buttons
            button.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            button.alpha = 0.7f // Make it slightly darker to show selection
        }

        button.setTextColor(Color.BLACK)

        // Update selected category
        selectedCategory = category
        selectedCategoryText.text = category
        selectedCategoryText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
    }

    private fun resetCategoryButtons() {
        // Reset all buttons in the categories container
        for (i in 0 until categoriesContainer.childCount) {
            val view = categoriesContainer.getChildAt(i)
            if (view is Button && view.id != R.id.addCategoryButton) {
                // Check if the button is using a GradientDrawable (dynamically added button)
                if (view.background is GradientDrawable) {
                    (view.background as GradientDrawable).setColor(Color.parseColor("#F0F0F0")) // Reset to original color
                } else {
                    // For regular buttons
                    view.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
                    view.alpha = 1.0f // Normal brightness
                }
                view.setTextColor(Color.BLACK)
            }
        }
    }

    private fun setupActionButtons() {
        val setLimitButton = findViewById<Button>(R.id.setLimitButton)
        val clearButton = findViewById<Button>(R.id.clearButton)
        val viewRewardsButton = findViewById<Button>(R.id.viewRewardsButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        setLimitButton.setOnClickListener {
            setLimit()
        }

        clearButton.setOnClickListener {
            // Clear inputs
            minSpendingInput.text.clear()
            maxSpendingInput.text.clear()
            descriptionInput.text.clear()

            // Reset category selection
            selectedCategory = null
            selectedCategoryText.text = "Select a category"
            selectedCategoryText.setTextColor(Color.parseColor("#A0A0A0"))

            // Reset category buttons
            resetCategoryButtons()

            Toast.makeText(this, "Form cleared", Toast.LENGTH_SHORT).show()
        }

        viewRewardsButton.setOnClickListener {
            // Navigate to rewards screen
            val intent = Intent(this, RewardsActivity::class.java)
            startActivity(intent)
        }

        cancelButton.setOnClickListener {
            // Return to previous screen
            finish()
        }
    }

    private fun setLimit() {
        // Validate inputs
        if (selectedCategory == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (minSpendingInput.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter minimum spending", Toast.LENGTH_SHORT).show()
            return
        }

        if (maxSpendingInput.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter maximum spending", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate min is less than max
        val minAmount = minSpendingInput.text.toString().toDoubleOrNull() ?: 0.0
        val maxAmount = maxSpendingInput.text.toString().toDoubleOrNull() ?: 0.0

        if (minAmount >= maxAmount) {
            Toast.makeText(this, "Minimum spending must be less than maximum spending", Toast.LENGTH_SHORT).show()
            return
        }

        // Save the budget limits to SharedPreferences
        val sharedPreferences = getSharedPreferences("BudgetVaultPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selectedCategory", selectedCategory)
        editor.putFloat("minSpending", minAmount.toFloat())
        editor.putFloat("maxSpending", maxAmount.toFloat())
        editor.putString("budgetDescription", descriptionInput.text.toString())
        editor.apply()

        // Show success message with XP info for gamification
        Toast.makeText(this, "Budget limits saved successfully! +25 XP", Toast.LENGTH_SHORT).show()

        // Set the result to inform DashboardActivity that budget was set
        setResult(Activity.RESULT_OK)

        // Show success dialog
        showSuccessDialog()
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_limit_success)

        // Set dialog properties
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        // Automatic dismiss after delay
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()

            // Return to Dashboard with RESULT_OK to track the action
            setResult(Activity.RESULT_OK)
            finish()

        }, 1500) // 1.5 seconds delay

        dialog.show()
    }

    private fun clearForm() {
        // Clear inputs
        minSpendingInput.text.clear()
        maxSpendingInput.text.clear()
        descriptionInput.text.clear()

        // Reset category selection
        selectedCategory = null
        selectedCategoryText.text = "Select a category"
        selectedCategoryText.setTextColor(Color.parseColor("#A0A0A0"))

        // Reset category buttons
        resetCategoryButtons()
    }

    private fun setupBottomNavigation() {
        // Set up bottom navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_budget  // Highlight budget icon

        // Handle bottom navigation item selection
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_budget -> {
                    // Already on budget screen
                    true
                }
                R.id.navigation_add -> {
                    // Show the Add options dialog
                    showAddOptionsDialog()
                    true
                }
                R.id.navigation_advices -> {
                    // Navigate to advices screen
                    val intent = Intent(this, AdvicesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_more -> {
                    // Handle more options
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

        // Set up button click listeners
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