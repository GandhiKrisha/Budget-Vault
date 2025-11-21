package com.teamvault.budgetvault

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.teamvault.budgetvault.data.database.AppDatabase
import com.teamvault.budgetvault.data.model.Expense
import com.teamvault.budgetvault.data.repository.ExpenseRepository
import com.teamvault.budgetvault.data.repository.IncomeRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ExpenseReportActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var lineChart: LineChart
    private lateinit var expenseRecyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseReportAdapter

    // Firebase repositories
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var incomeRepository: IncomeRepository

    // Date range filters
    private var startDate: String = ""
    private var endDate: String = ""
    private var currentExpenses: List<Expense> = emptyList()

    // Date format
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_report)

        // Initialize repositories
        database = AppDatabase.getDatabase(this)
        expenseRepository = ExpenseRepository(database.expenseDao(), this)
        incomeRepository = IncomeRepository(database.incomeDao())

        // Initialize views
        initializeViews()

        // Set default date range to current month
        setDefaultDateRange()

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBackButton()
        setupBottomNavigation()
        setupDateRangeButtons()

        // Sync data and display
        syncDataAndDisplay()
    }

    private fun initializeViews() {
        lineChart = findViewById(R.id.expenseIncomeChart)
        expenseRecyclerView = findViewById(R.id.expenseRecyclerView)

        // Setup RecyclerView
        expenseAdapter = ExpenseReportAdapter(
            onDeleteClick = { expense -> showDeleteConfirmation(expense) }
        )
        expenseRecyclerView.layoutManager = LinearLayoutManager(this)
        expenseRecyclerView.adapter = expenseAdapter
    }

    private fun setDefaultDateRange() {
        val calendar = Calendar.getInstance()

        // End date is today
        endDate = dateFormat.format(calendar.time)

        // Start date is first day of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = dateFormat.format(calendar.time)

        updateDateRangeDisplay()
    }

    private fun setupDateRangeButtons() {
        val startDateButton = findViewById<Button>(R.id.startDateButton)
        val endDateButton = findViewById<Button>(R.id.endDateButton)
        val filterButton = findViewById<Button>(R.id.filterButton)

        startDateButton.setOnClickListener { showDatePicker(true) }
        endDateButton.setOnClickListener { showDatePicker(false) }
        filterButton.setOnClickListener { filterByDateRange() }

        updateDateRangeDisplay()
    }

    private fun updateDateRangeDisplay() {
        val startDateButton = findViewById<Button>(R.id.startDateButton)
        val endDateButton = findViewById<Button>(R.id.endDateButton)

        startDateButton.text = if (startDate.isNotEmpty()) startDate else "Start Date"
        endDateButton.text = if (endDate.isNotEmpty()) endDate else "End Date"
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()

        // Set current date or existing selected date
        if (isStartDate && startDate.isNotEmpty()) {
            try {
                calendar.time = dateFormat.parse(startDate) ?: Date()
            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error parsing start date: ${e.message}")
            }
        } else if (!isStartDate && endDate.isNotEmpty()) {
            try {
                calendar.time = dateFormat.parse(endDate) ?: Date()
            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error parsing end date: ${e.message}")
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)

                if (isStartDate) {
                    startDate = selectedDate
                } else {
                    endDate = selectedDate
                }

                updateDateRangeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun filterByDateRange() {
        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate date range
        try {
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)

            if (start != null && end != null && start.after(end)) {
                Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            return
        }

        // Apply filter and refresh data
        loadFilteredData()
    }

    private fun loadFilteredData() {
        lifecycleScope.launch {
            try {
                val currentUserId = getCurrentUserId()

                // Get expenses for the selected date range
                currentExpenses = expenseRepository.getExpensesByUserAndDateRange(
                    currentUserId, startDate, endDate
                )

                // Update displays
                displayTotalSpending()
                displayTotalIncome()
                displayGraph()

                // Update expense list
                expenseAdapter.updateExpenses(currentExpenses)

                Log.d("ExpenseReportActivity", "Filtered ${currentExpenses.size} expenses for range: $startDate to $endDate")

            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error loading filtered data: ${e.message}")
                Toast.makeText(this@ExpenseReportActivity, "Error loading filtered data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBackButton() {
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

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

    private fun showAddOptionsDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_options)

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.gravity = Gravity.BOTTOM
        dialog.window?.attributes = layoutParams

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f)

        dialog.findViewById<Button>(R.id.addIncomeButton).setOnClickListener {
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Sync data from Firebase and then display
    private fun syncDataAndDisplay() {
        lifecycleScope.launch {
            try {
                // Sync expenses and income from Firebase
                expenseRepository.syncExpensesFromFirebase()
                incomeRepository.syncIncomeFromFirebase()

                Log.d("ExpenseReportActivity", "Data synced successfully")

                // Load filtered data for the current date range
                loadFilteredData()
            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error syncing data: ${e.message}")
                // Still display local data even if sync fails
                loadFilteredData()
            }
        }
    }

    // Method to display total spending with filtered data
    private fun displayTotalSpending() {
        val totalSpendingTextView = findViewById<TextView>(R.id.totalSpendingText)

        lifecycleScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                val total = expenseRepository.getTotalExpensesByDateRange(currentUserId, startDate, endDate)

                totalSpendingTextView.text = "R ${String.format("%.2f", total)}"

                Log.d("ExpenseReportActivity", "Total expenses for user $currentUserId ($startDate to $endDate): R$total")
            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error calculating total spending: ${e.message}")
                totalSpendingTextView.text = "R 0.00"
            }
        }
    }

    // Method to display total income (for entire period, not filtered)
    private fun displayTotalIncome() {
        val totalIncomeTextView = findViewById<TextView>(R.id.totalIncomeText)

        lifecycleScope.launch {
            try {
                val totalIncome = incomeRepository.getTotalIncomeLocal() ?: 0.0
                totalIncomeTextView.text = "R ${String.format("%.2f", totalIncome)}"

                Log.d("ExpenseReportActivity", "Total income: R$totalIncome")
            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error calculating total income: ${e.message}")
                totalIncomeTextView.text = "R 0.00"
            }
        }
    }

    // Method to display expenses vs income chart with filtered data
    private fun displayGraph() {
        lifecycleScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                val totalExpense = expenseRepository.getTotalExpensesByDateRange(currentUserId, startDate, endDate)
                val totalIncome = incomeRepository.getTotalIncomeLocal() ?: 0.0

                // Create two lists of entries: one for expense and one for income
                val expenseEntries = mutableListOf<Entry>()
                val incomeEntries = mutableListOf<Entry>()

                // Add data points for expenses and income
                expenseEntries.add(Entry(0f, 0f))  // Start at origin for Expense
                expenseEntries.add(Entry(1f, totalExpense.toFloat()))  // X: 1 (Expense), Y: total expense

                incomeEntries.add(Entry(0f, 0f))  // Start at origin for Income
                incomeEntries.add(Entry(1f, totalIncome.toFloat()))  // X: 1 (Income), Y: total income

                // Create a LineDataSet object for expenses (smooth curve)
                val expenseDataSet = LineDataSet(expenseEntries, "Expenses ($startDate to $endDate)")
                expenseDataSet.setColor(Color.RED)
                expenseDataSet.setCircleColor(Color.RED)
                expenseDataSet.valueTextColor = Color.WHITE
                expenseDataSet.setDrawCircles(true)
                expenseDataSet.lineWidth = 2f
                expenseDataSet.setDrawFilled(true)
                expenseDataSet.fillColor = Color.RED
                expenseDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

                // Create a LineDataSet object for income (smooth curve)
                val incomeDataSet = LineDataSet(incomeEntries, "Total Income")
                incomeDataSet.setColor(Color.GREEN)
                incomeDataSet.setCircleColor(Color.GREEN)
                incomeDataSet.valueTextColor = Color.WHITE
                incomeDataSet.setDrawCircles(true)
                incomeDataSet.lineWidth = 2f
                incomeDataSet.setDrawFilled(true)
                incomeDataSet.fillColor = Color.GREEN
                incomeDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

                // Create a LineData object with both datasets
                val lineData = LineData(expenseDataSet, incomeDataSet)

                // Set the data to the LineChart
                lineChart.data = lineData
                lineChart.invalidate()  // Refresh chart
                lineChart.animateXY(1500, 1500, Easing.EaseInOutQuad) // Smooth animation

                Log.d("ExpenseReportActivity", "Chart updated - Expenses: R$totalExpense, Income: R$totalIncome")
            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error updating chart: ${e.message}")
            }
        }
    }

    private fun showDeleteConfirmation(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?\n\n${expense.description}\nR ${String.format("%.2f", expense.amount)}")
            .setPositiveButton("Delete") { _, _ ->
                deleteExpense(expense)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense(expense: Expense) {
        lifecycleScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                expenseRepository.deleteExpenseLocal(expense.id, currentUserId)

                Toast.makeText(this@ExpenseReportActivity, "Expense deleted", Toast.LENGTH_SHORT).show()

                // Refresh the data
                loadFilteredData()

                Log.d("ExpenseReportActivity", "Deleted expense: ${expense.description}")
            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error deleting expense: ${e.message}")
                Toast.makeText(this@ExpenseReportActivity, "Error deleting expense", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Generate and download PDF report with filtered data
    private fun generateAndDownloadReport() {
        lifecycleScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                val totalIncome = incomeRepository.getTotalIncomeLocal() ?: 0.0

                generatePDFReportWithList(currentExpenses, totalIncome)

                Log.d("ExpenseReportActivity", "PDF report generated for ${currentExpenses.size} expenses")
            } catch (e: Exception) {
                Log.e("ExpenseReportActivity", "Error generating PDF report: ${e.message}")
                Toast.makeText(this@ExpenseReportActivity, "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generatePDFReportWithList(expenses: List<Expense>, income: Double) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var yPosition = 50
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("BudgetVault Expense Report", 200f, yPosition.toFloat(), paint)

            yPosition += 30
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText("Date Range: $startDate to $endDate", 20f, yPosition.toFloat(), paint)

            yPosition += 40
            paint.textSize = 14f

            canvas.drawText("Total Income: R %.2f".format(income), 20f, yPosition.toFloat(), paint)
            yPosition += 20

            val totalExpenses = expenses.sumOf { it.amount }
            canvas.drawText("Total Expenses (Filtered): R %.2f".format(totalExpenses), 20f, yPosition.toFloat(), paint)
            yPosition += 20

            val balance = income - totalExpenses
            canvas.drawText("Balance: R %.2f".format(balance), 20f, yPosition.toFloat(), paint)
            yPosition += 30

            canvas.drawText("Expense Details:", 20f, yPosition.toFloat(), paint)
            yPosition += 20

            paint.textSize = 12f
            expenses.forEachIndexed { index, expense ->
                val expenseLine = "${index + 1}. ${expense.category} - R %.2f (${expense.date})".format(expense.amount)
                canvas.drawText(expenseLine, 20f, yPosition.toFloat(), paint)
                yPosition += 15

                if (expense.description?.isNotEmpty() == true) {
                    canvas.drawText("   Description: ${expense.description}", 30f, yPosition.toFloat(), paint)
                    yPosition += 15
                }

                if (yPosition > 800) {
                    pdfDocument.finishPage(page)
                    // Create new page if needed
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 2).create()
                    val newPage = pdfDocument.startPage(newPageInfo)
                    yPosition = 50
                    paint.textSize = 14f
                    canvas.drawText("... Report continues", 20f, yPosition.toFloat(), paint)
                    yPosition += 30
                    paint.textSize = 12f
                }
            }

            pdfDocument.finishPage(page)

            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "BudgetVault_Report_${startDate.replace("/", "_")}_to_${endDate.replace("/", "_")}.pdf"
            )

            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "Report saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()

            pdfDocument.close()
        } catch (e: IOException) {
            Log.e("ExpenseReportActivity", "Error generating PDF: ${e.message}")
            Toast.makeText(this, "Error saving report: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentUserId(): String {
        val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("userId", "default_user") ?: "default_user"
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        syncDataAndDisplay()
    }
}

// RecyclerView Adapter for expenses
class ExpenseReportAdapter(
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseReportAdapter.ExpenseViewHolder>() {

    private var expenses: List<Expense> = emptyList()

    fun updateExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryText: TextView = itemView.findViewById(R.id.categoryText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val amountText: TextView = itemView.findViewById(R.id.amountText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val imagePhoto: ImageView = itemView.findViewById(R.id.imagePhoto)
        private val viewImageButton: Button = itemView.findViewById(R.id.viewImageButton)
        private val imageLayout: LinearLayout = itemView.findViewById(R.id.imageLayout)

        fun bind(expense: Expense) {
            categoryText.text = expense.category ?: "Uncategorized"
            descriptionText.text = expense.description ?: "No description"
            amountText.text = "R ${String.format("%.2f", expense.amount)}"
            dateText.text = expense.date ?: ""

            deleteButton.setOnClickListener {
                onDeleteClick(expense)
            }

            // Handle image display if photo exists
            if (!expense.photoUri.isNullOrEmpty()) {
                imageLayout.visibility = View.VISIBLE
                imagePhoto.visibility = View.VISIBLE
                viewImageButton.visibility = View.VISIBLE

                // Load image using a simple approach (you can use Glide if available)
                try {
                    val uri = android.net.Uri.parse(expense.photoUri)
                    imagePhoto.setImageURI(uri)
                } catch (e: Exception) {
                    imagePhoto.setImageResource(android.R.drawable.gallery_thumb)
                }

                viewImageButton.setOnClickListener {
                    showImageDialog(expense.photoUri!!)
                }
            } else {
                imageLayout.visibility = View.GONE
                imagePhoto.visibility = View.GONE
                viewImageButton.visibility = View.GONE
            }

            // Add visual feedback
            itemView.setOnClickListener {
                // Optional: Show expense details
            }
        }

        private fun showImageDialog(photoUri: String) {
            val dialog = android.app.AlertDialog.Builder(itemView.context).create()
            val imageView = ImageView(itemView.context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                adjustViewBounds = true
            }

            try {
                val uri = android.net.Uri.parse(photoUri)
                imageView.setImageURI(uri)
            } catch (e: Exception) {
                imageView.setImageResource(android.R.drawable.gallery_thumb)
            }

            dialog.setView(imageView)
            dialog.setButton(android.app.AlertDialog.BUTTON_NEGATIVE, "Close") { d, _ -> d.dismiss() }
            dialog.show()
        }
    }
}