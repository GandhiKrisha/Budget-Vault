package com.teamvault.budgetvault

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.teamvault.budgetvault.data.database.AppDatabase
import com.teamvault.budgetvault.data.model.Expense
import com.teamvault.budgetvault.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : BaseActivity() {

    private lateinit var dateInput: EditText
    private lateinit var startTimeInput: EditText
    private lateinit var endTimeInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var selectedCategoryText: TextView
    private lateinit var categoriesContainer: LinearLayout

    // Gamification manager
    private lateinit var gamificationManager: GamificationManager

    // Firebase repository
    private lateinit var expenseRepository: ExpenseRepository

    private var selectedCategory: String? = null
    private val calendar = Calendar.getInstance()

    // Photo attachment properties
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_PICK_IMAGE = 2
        private const val PERMISSION_CAMERA_REQUEST_CODE = 100
        private const val PERMISSION_STORAGE_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize gamification manager
        gamificationManager = GamificationManager.getInstance(this)

        // Initialize expense repository
        val database = AppDatabase.getDatabase(this)
        expenseRepository = ExpenseRepository(database.expenseDao())

        initializeViews()
        setupDateTimePickers()
        setupCategoryButtons()
        setupActionButtons()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        dateInput = findViewById(R.id.dateInput)
        startTimeInput = findViewById(R.id.startTimeInput)
        endTimeInput = findViewById(R.id.endTimeInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        amountInput = findViewById(R.id.amountInput)
        selectedCategoryText = findViewById(R.id.selectedCategoryText)
        categoriesContainer = findViewById(R.id.categoriesContainer)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupDateTimePickers() {
        dateInput.setOnClickListener { showDatePicker() }
        startTimeInput.setOnClickListener { showTimePicker(startTimeInput) }
        endTimeInput.setOnClickListener { showTimePicker(endTimeInput) }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            updateDateInView()
        }, year, month, day).show()
    }

    private fun updateDateInView() {
        val format = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        dateInput.setText(sdf.format(calendar.time))
    }

    private fun showTimePicker(timeField: EditText) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val time = String.format("%02d:%02d", selectedHour, selectedMinute)
            timeField.setText(time)
        }, hour, minute, true).show()
    }

    private fun setupCategoryButtons() {
        val rentButton = findViewById<Button>(R.id.rentCategoryButton)
        val transportButton = findViewById<Button>(R.id.transportCategoryButton)
        val entertainmentButton = findViewById<Button>(R.id.entertainmentCategoryButton)
        val clothingButton = findViewById<Button>(R.id.clothingCategoryButton)
        val addCategoryButton = findViewById<Button>(R.id.addCategoryButton)

        rentButton.setOnClickListener { selectCategory("Rent", rentButton) }
        transportButton.setOnClickListener { selectCategory("Transport", transportButton) }
        entertainmentButton.setOnClickListener { selectCategory("Entertainment", entertainmentButton) }
        clothingButton.setOnClickListener { selectCategory("Clothing", clothingButton) }
        addCategoryButton.setOnClickListener { showAddCategoryDialog() }
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
        val newCategoryButton = Button(this).apply {
            text = categoryName
            setPadding(24, 12, 24, 12)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 32
            }
            setOnClickListener { selectCategory(categoryName, this) }
        }

        categoriesContainer.addView(newCategoryButton, 1)
        categoriesContainer.requestLayout()
        newCategoryButton.post { selectCategory(categoryName, newCategoryButton) }

        Toast.makeText(this, "Category '$categoryName' added", Toast.LENGTH_SHORT).show()
    }

    private fun selectCategory(category: String, button: Button) {
        resetCategoryButtons()
        button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        button.setTextColor(ContextCompat.getColor(this, android.R.color.white))

        selectedCategory = category
        selectedCategoryText.text = category
        selectedCategoryText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
    }

    private fun resetCategoryButtons() {
        for (i in 0 until categoriesContainer.childCount) {
            val view = categoriesContainer.getChildAt(i)
            if (view is Button && view.id != R.id.addCategoryButton) {
                view.setBackgroundColor(Color.WHITE)
                view.setTextColor(Color.BLACK)
            }
        }
    }

    private fun setupActionButtons() {
        val attachPhotoButton = findViewById<Button>(R.id.attachPhotoButton)
        val saveExpenseButton = findViewById<Button>(R.id.saveExpenseButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        attachPhotoButton.setOnClickListener { showAttachPhotoDialog() }
        saveExpenseButton.setOnClickListener { saveExpense() }
        cancelButton.setOnClickListener { finish() }
    }

    private fun showAttachPhotoDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_attach_photo)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        dialog.findViewById<Button>(R.id.browseButton).setOnClickListener {
            if (checkStoragePermission()) openGallery()
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.takePhotoButton).setOnClickListener {
            if (checkCameraPermission()) takePicture()
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            if (selectedImageUri != null) {
                Toast.makeText(this, "Photo attached successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.cancelAttachButton).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_CAMERA_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun checkStoragePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_STORAGE_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    private fun takePicture() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try { createImageFile() } catch (ex: IOException) { null }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(this, "com.teamvault.budgetvault.fileprovider", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    currentPhotoPath?.let {
                        selectedImageUri = Uri.fromFile(File(it))
                        Toast.makeText(this, "Photo taken successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_PICK_IMAGE -> {
                    data?.data?.let { uri ->
                        selectedImageUri = uri
                        Toast.makeText(this, "Photo selected successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture()
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveExpense() {
        if (dateInput.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCategory == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        if (amountInput.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Get proper user ID from SharedPreferences
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "default_user") ?: "default_user"

        val expense = Expense(
            userId = userId, // Use proper user ID
            date = dateInput.text.toString(),
            startTime = startTimeInput.text.toString(),
            endTime = endTimeInput.text.toString(),
            description = descriptionInput.text.toString(),
            amount = amountInput.text.toString().toDouble(),
            category = selectedCategory ?: "uncategorized",
            photoUri = selectedImageUri?.toString() // Save the URI as String
        )

        // Use the new repository with Firebase sync
        lifecycleScope.launch {
            val result = expenseRepository.addExpense(expense)

            if (result.isSuccess) {
                Toast.makeText(this@AddExpenseActivity, "Expense saved successfully! +10 XP", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                showSuccessDialog()
            } else {
                Toast.makeText(this@AddExpenseActivity, "Error saving expense: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_success)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()

            // Return to dashboard with RESULT_OK to track the action
            setResult(Activity.RESULT_OK)
            finish()
        }, 1500)

        dialog.show()
    }

    private fun resetForm() {
        dateInput.text.clear()
        startTimeInput.text.clear()
        endTimeInput.text.clear()
        descriptionInput.text.clear()
        amountInput.text.clear()

        selectedCategory = null
        selectedCategoryText.text = getString(R.string.select_expense_category)
        selectedCategoryText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        resetCategoryButtons()
        selectedImageUri = null
        currentPhotoPath = null
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_add

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_budget -> {
                    val intent = Intent(this, BudgetActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_add -> {
                    showAddOptionsDialog()
                    true
                }
                R.id.navigation_advices -> {
                    val intent = Intent(this, AdvicesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_more -> {
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
            // Handle add income action with result
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivityForResult(intent, DashboardActivity.REQUEST_CODE_ADD_INCOME)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
            // Handle add expense action with result
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