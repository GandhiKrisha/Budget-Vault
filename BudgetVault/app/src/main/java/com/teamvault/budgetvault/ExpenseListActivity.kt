package com.teamvault.budgetvault

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.teamvault.budgetvault.data.database.AppDatabase
import com.teamvault.budgetvault.data.model.Expense
import com.teamvault.budgetvault.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.graphics.Color
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.EditText

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var expenseRecyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var db: AppDatabase
    private lateinit var categoryChart: PieChart

    // Firebase repository
    private lateinit var expenseRepository: ExpenseRepository

    // UI components for filtering
    private lateinit var fromDateEditText: EditText
    private lateinit var toDateEditText: EditText
    private lateinit var categorySpinner: android.widget.Spinner
    private var selectedFromDate: String? = null
    private var selectedToDate: String? = null
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        expenseRecyclerView = findViewById(R.id.expensesRecyclerView)
        expenseRecyclerView.layoutManager = LinearLayoutManager(this)

        categoryChart = findViewById(R.id.categoryChart)

        db = AppDatabase.getDatabase(this)

        // Initialize Firebase repository
        expenseRepository = ExpenseRepository(db.expenseDao())

        // Initialize filters
        fromDateEditText = findViewById(R.id.fromDateEditText)
        toDateEditText = findViewById(R.id.toDateEditText)
        categorySpinner = findViewById(R.id.categorySpinner)

        // From date picker click listener
        fromDateEditText.setOnClickListener {
            showDatePicker(true)
        }

        // To date picker click listener
        toDateEditText.setOnClickListener {
            showDatePicker(false)
        }

        // Set up category spinner
        val categories = listOf("All", "Food", "Entertainment", "Transport", "Rent", "Clothing", "Others")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        categorySpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = when (position) {
                    0 -> null // "All"
                    categories.size - 1 -> "Others" // "Others"
                    else -> categories[position] // other categories
                }
                loadExpensesForUser()  // Reload expenses with selected filters
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        })

        // Back button listener
        val backbtn = findViewById<ImageButton>(R.id.backButton)
        backbtn.setOnClickListener {
            finish()
        }

        // Sync data from Firebase and load expenses
        syncAndLoadExpenses()
    }

    // Function to show the date picker dialog
    private fun showDatePicker(isFromDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.set(year, month, dayOfMonth)
                val selectedDate = sdf.format(calendar.time)

                if (isFromDate) {
                    selectedFromDate = selectedDate
                    fromDateEditText.setText(selectedFromDate)
                } else {
                    selectedToDate = selectedDate
                    toDateEditText.setText(selectedToDate)
                }

                loadExpensesForUser()  // Reload expenses with selected date filter
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // Sync data from Firebase and then load expenses
    private fun syncAndLoadExpenses() {
        lifecycleScope.launch {
            try {
                // First sync expenses from Firebase
                val syncResult = expenseRepository.syncExpensesFromFirebase()
                if (syncResult.isSuccess) {
                    Log.d("ExpenseListActivity", "Expenses synced from Firebase successfully")
                } else {
                    Log.w("ExpenseListActivity", "Failed to sync from Firebase: ${syncResult.exceptionOrNull()?.message}")
                }

                // Then load expenses (local data which now includes synced data)
                loadExpensesForUser()
            } catch (e: Exception) {
                Log.e("ExpenseListActivity", "Error syncing and loading expenses: ${e.message}")
                // Still try to load local expenses even if sync fails
                loadExpensesForUser()
            }
        }
    }

    // Function to load expenses from the repository and apply filters
    private fun loadExpensesForUser() {
        lifecycleScope.launch {
            try {
                // Get current user ID
                val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
                val currentUserId = sharedPreferences.getString("userId", "default_user") ?: "default_user"

                // Get all expenses from repository (local + synced Firebase data)
                val allExpenses = expenseRepository.getAllExpensesLocal()

                // Filter expenses for current user
                val userExpenses = allExpenses.filter { expense ->
                    expense.userId == currentUserId ||
                            (currentUserId.startsWith("firebase_") && expense.userId == currentUserId.removePrefix("firebase_")) ||
                            (expense.userId.isEmpty() && currentUserId == "default_user") // For legacy data
                }

                // Filter expenses based on date and category
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                val filteredExpenses = userExpenses.filter { expense ->
                    // Since expense.date is a String, we need to parse it first
                    val expenseDateStr = expense.date
                    val expenseDate = try {
                        if (!expenseDateStr.isNullOrEmpty()) {
                            // Convert from stored format (likely dd/MM/yyyy) to our format (yyyy-MM-dd)
                            val storedDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val date = storedDateFormat.parse(expenseDateStr)
                            date?.let { dateFormat.format(it) }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }

                    val fromDateMatches = if (selectedFromDate != null && expenseDate != null) {
                        try {
                            // Using string comparison since we've formatted both to yyyy-MM-dd
                            expenseDate >= selectedFromDate!!
                        } catch (e: Exception) {
                            true // If comparison fails, don't filter
                        }
                    } else {
                        true // No from date filter or no expense date
                    }

                    val toDateMatches = if (selectedToDate != null && expenseDate != null) {
                        try {
                            // Using string comparison since we've formatted both to yyyy-MM-dd
                            expenseDate <= selectedToDate!!
                        } catch (e: Exception) {
                            true // If comparison fails, don't filter
                        }
                    } else {
                        true // No to date filter or no expense date
                    }

                    val matchesCategory = when (selectedCategory) {
                        "Others" -> !listOf("Food", "Grocery", "Equipments", "Entertainment", "Transport", "Rent", "Clothing").contains(expense.category)
                        null -> true // "All"
                        else -> expense.category == selectedCategory
                    }

                    fromDateMatches && toDateMatches && matchesCategory
                }

                // Update RecyclerView and Pie Chart
                if (filteredExpenses.isNotEmpty()) {
                    setupRecyclerView(filteredExpenses)
                    Log.d("ExpenseListActivity", "Loaded ${filteredExpenses.size} filtered expenses")
                } else {
                    Toast.makeText(this@ExpenseListActivity, "No matching expenses found.", Toast.LENGTH_SHORT).show()
                    expenseRecyclerView.adapter = null

                    // Clear the pie chart when no data
                    categoryChart.clear()
                    categoryChart.invalidate()
                }
            } catch (e: Exception) {
                Log.e("ExpenseListActivity", "Error loading expenses: ${e.message}")
                Toast.makeText(this@ExpenseListActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to set up the RecyclerView with filtered expenses
    private fun setupRecyclerView(expenses: List<Expense>) {
        expenseAdapter = ExpenseAdapter(expenses)
        expenseRecyclerView.adapter = expenseAdapter
        showPieChart(expenses)
    }

    // Function to show Pie Chart of expenses by category
    private fun showPieChart(expenses: List<Expense>) {
        try {
            // Summarize expenses by category
            val categoryTotals = mutableMapOf<String, Float>()
            for (expense in expenses) {
                val amount = expense.amount.toFloat()
                val category = expense.category ?: "Others"
                categoryTotals[category] = categoryTotals.getOrDefault(category, 0f) + amount
            }

            val entries = ArrayList<PieEntry>()
            for ((category, total) in categoryTotals) {
                entries.add(PieEntry(total, category))
            }

            if (entries.isEmpty()) {
                // No data to show
                categoryChart.clear()
                categoryChart.invalidate()
                return
            }

            val colors = listOf(
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#8BC34A"), // Green
                Color.parseColor("#FF5722"), // Red
                Color.parseColor("#FFC107"), // Amber
                Color.parseColor("#9C27B0"), // Purple
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#607D8B"), // Blue Grey
                Color.parseColor("#E91E63")  // Pink
            )

            val dataSet = PieDataSet(entries, "Expenses by Category")
            dataSet.colors = colors
            dataSet.sliceSpace = 2f
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.WHITE

            val data = PieData(dataSet)

            categoryChart.data = data
            categoryChart.description = Description().apply { text = "" }
            categoryChart.setUsePercentValues(true)
            categoryChart.setDrawHoleEnabled(true)
            categoryChart.setHoleColor(Color.TRANSPARENT)
            categoryChart.setTransparentCircleAlpha(110)
            categoryChart.setEntryLabelColor(Color.BLACK)
            categoryChart.animateY(1000)

            categoryChart.invalidate()
        } catch (e: Exception) {
            Log.e("ExpenseListActivity", "Error creating pie chart: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        syncAndLoadExpenses()
    }
}