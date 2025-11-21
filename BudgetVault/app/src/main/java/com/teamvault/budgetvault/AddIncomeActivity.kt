package com.teamvault.budgetvault

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.teamvault.budgetvault.data.database.AppDatabase
import com.teamvault.budgetvault.data.model.Income
import com.teamvault.budgetvault.data.repository.IncomeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncomeActivity : BaseActivity() {

    private lateinit var dateInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var selectedSourceText: TextView
    private lateinit var incomeCategoriesContainer: LinearLayout

    // Firebase repository
    private lateinit var incomeRepository: IncomeRepository

    private var selectedSource: String? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_income)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize income repository
        val database = AppDatabase.getDatabase(this)
        incomeRepository = IncomeRepository(database.incomeDao())

        // Initialize views
        initializeViews()

        // Set up date picker
        setupDatePicker()

        // Set up source buttons
        setupSourceButtons()

        // Set up action buttons
        setupActionButtons()
    }

    private fun initializeViews() {
        // Find all input fields
        dateInput = findViewById(R.id.dateInput)
        amountInput = findViewById(R.id.amountInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        selectedSourceText = findViewById(R.id.selectedSourceText)
        incomeCategoriesContainer = findViewById(R.id.incomeCategoriesContainer)

        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupDatePicker() {
        // Date picker
        dateInput.setOnClickListener {
            showDatePicker()
        }
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

    private fun setupSourceButtons() {
        val salaryButton = findViewById<Button>(R.id.salaryButton)
        val savingsButton = findViewById<Button>(R.id.savingsButton)
        val investmentButton = findViewById<Button>(R.id.investmentButton)
        val addSourceButton = findViewById<Button>(R.id.addSourceButton)

        // Set up source selection
        salaryButton.setOnClickListener { selectSource("Salary", salaryButton) }
        savingsButton.setOnClickListener { selectSource("Savings", savingsButton) }
        investmentButton.setOnClickListener { selectSource("Investment", investmentButton) }

        addSourceButton.setOnClickListener {
            showAddSourceDialog()
        }
    }

    private fun showAddSourceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_source, null)
        val sourceNameInput = dialogView.findViewById<EditText>(R.id.sourceNameInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Income Source")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val sourceName = sourceNameInput.text.toString().trim()
                if (sourceName.isNotEmpty()) {
                    addNewSource(sourceName)
                } else {
                    Toast.makeText(this, "Source name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addNewSource(sourceName: String) {
        // Create a new source button
        val newSourceButton = Button(this).apply {
            text = sourceName
            setPadding(24, 12, 24, 12)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 32
            }
            setOnClickListener {
                selectSource(sourceName, this)
            }
        }

        incomeCategoriesContainer.addView(newSourceButton, 1)
        incomeCategoriesContainer.requestLayout()
        newSourceButton.post {
            selectSource(sourceName, newSourceButton)
        }

        Toast.makeText(this, "Source '$sourceName' added", Toast.LENGTH_SHORT).show()
    }

    private fun selectSource(source: String, button: Button) {
        resetSourceButtons()
        button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        selectedSource = source
        selectedSourceText.text = source
        selectedSourceText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
    }

    private fun resetSourceButtons() {
        for (i in 0 until incomeCategoriesContainer.childCount) {
            val view = incomeCategoriesContainer.getChildAt(i)
            if (view is Button && view.id != R.id.addSourceButton) {
                view.setBackgroundColor(Color.parseColor("#F0F0F0"))
                view.setTextColor(Color.BLACK)
            }
        }
    }

    private fun setupActionButtons() {
        val addIncomeButton = findViewById<Button>(R.id.addIncomeButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        addIncomeButton.setOnClickListener {
            saveIncome()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun saveIncome() {
        try {
            // Validate inputs
            if (dateInput.text.isNullOrEmpty()) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedSource == null) {
                Toast.makeText(this, "Please select a source", Toast.LENGTH_SHORT).show()
                return
            }

            if (amountInput.text.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return
            }

            val amount = amountInput.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return
            }

            // Create an Income object
            val income = Income(
                date = dateInput.text.toString(),
                amount = amount,
                sourceOfIncome = selectedSource ?: "",
                description = descriptionInput.text.toString()
            )

            // Use the new repository with Firebase sync
            lifecycleScope.launch {
                val result = incomeRepository.addIncome(income)

                if (result.isSuccess) {
                    Log.d("AddIncomeActivity", "Income saved successfully")
                    Toast.makeText(this@AddIncomeActivity, "Income saved successfully! +10 XP", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    showSuccessDialog()
                } else {
                    Log.e("AddIncomeActivity", "Error saving income: ${result.exceptionOrNull()?.message}")
                    Toast.makeText(this@AddIncomeActivity, "Error saving income: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("AddIncomeActivity", "Unexpected error: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "An unexpected error occurred", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessDialog() {
        try {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_success_income)

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
                finish()
            }, 1500) // 1.5 seconds delay

            dialog.show()
        } catch (e: Exception) {
            Log.e("AddIncomeActivity", "Error showing success dialog: ${e.message}")
            e.printStackTrace()
            finish()
        }
    }
}