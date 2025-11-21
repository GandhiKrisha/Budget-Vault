package com.teamvault.budgetvault.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teamvault.budgetvault.data.dao.ExpenseDao
import com.teamvault.budgetvault.data.model.Expense
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // Sync cooldown to prevent excessive syncing
    private var lastSyncTime = 0L
    private val SYNC_COOLDOWN = 5 * 60 * 1000L // 5 minutes

    // Local Room operations (filtered by user)
    suspend fun insertExpenseLocal(expense: Expense) = expenseDao.insertExpense(expense)

    suspend fun getAllExpensesLocal(): List<Expense> = expenseDao.getAllExpenses()

    // Get total expenses for current user only
    suspend fun getTotalExpensesLocal(): Double? {
        val userId = getCurrentUserIdForLocal()
        return expenseDao.getTotalExpensesByUser(userId) ?: 0.0
    }

    suspend fun getRecentExpensesLocal(userId: String, limit: Int): List<Expense> {
        return expenseDao.getRecentExpenses(userId, limit)
    }

    suspend fun getExpensesByUserAndDateRange(userId: String, startDate: String, endDate: String): List<Expense> {
        return expenseDao.getAllExpenses().filter { expense ->
            expense.userId == userId &&
                    expense.date != null &&
                    isDateInRange(expense.date!!, startDate, endDate)
        }
    }

    suspend fun getTotalExpensesByDateRange(userId: String, startDate: String, endDate: String): Double {
        return getExpensesByUserAndDateRange(userId, startDate, endDate).sumOf { it.amount }
    }

    private fun isDateInRange(expenseDate: String, startDate: String, endDate: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val expense = dateFormat.parse(expenseDate)
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)

            expense != null && start != null && end != null &&
                    (expense.after(start) || expense == start) &&
                    (expense.before(end) || expense == end)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error parsing dates: ${e.message}")
            false
        }
    }

    suspend fun deleteExpenseLocal(expenseId: Int, userId: String) {
        expenseDao.deleteUserExpense(userId, expenseId)
    }

    private fun getCurrentUserIdForLocal(): String {
        return currentUserId ?: context?.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
            ?.getString("userId", "default_user") ?: "default_user"
    }

    // Firebase operations with duplicate prevention
    suspend fun addExpenseToFirebase(expense: Expense): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d("ExpenseRepo", "Adding expense to Firebase for user: $userId")

            // Check if expense already exists in Firebase
            val existingQuery = firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .whereEqualTo("amount", expense.amount)
                .whereEqualTo("date", expense.date)
                .whereEqualTo("category", expense.category)
                .whereEqualTo("description", expense.description)
                .limit(1)

            val existingSnapshot = existingQuery.get().await()
            if (!existingSnapshot.isEmpty) {
                Log.d("ExpenseRepo", "Expense already exists in Firebase, skipping")
                return Result.success("exists")
            }

            val expenseData = mapOf(
                "userId" to userId,
                "date" to expense.date,
                "startTime" to expense.startTime,
                "endTime" to expense.endTime,
                "description" to expense.description,
                "amount" to expense.amount,
                "category" to expense.category,
                "photoUri" to expense.photoUri,
                "localId" to expense.id,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            val docRef = firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .add(expenseData)
                .await()

            Log.d("ExpenseRepo", "Expense added to Firebase with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("ExpenseRepo", "Failed to add expense to Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncExpensesFromFirebase(): Result<List<Expense>> {
        return try {
            val currentTime = System.currentTimeMillis()

            // Check cooldown
            if (currentTime - lastSyncTime < SYNC_COOLDOWN) {
                Log.d("ExpenseRepo", "Sync skipped - cooldown active")
                return Result.success(emptyList())
            }

            val userId = currentUserId
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d("ExpenseRepo", "Syncing expenses from Firebase for user: $userId")

            // Get last sync timestamp
            val sharedPrefs = context?.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val lastSyncTimestamp = sharedPrefs?.getLong("last_expense_sync_$userId", 0L) ?: 0L

            val query = if (lastSyncTimestamp > 0) {
                firestore.collection("users")
                    .document(userId)
                    .collection("expenses")
                    .whereGreaterThan("createdAt", com.google.firebase.Timestamp(lastSyncTimestamp / 1000, 0))
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            } else {
                firestore.collection("users")
                    .document(userId)
                    .collection("expenses")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            }

            val snapshot = query.get().await()
            val newExpenses = mutableListOf<Expense>()

            for (doc in snapshot.documents) {
                val data = doc.data
                if (data != null) {
                    val expense = Expense(
                        id = 0, // Room will auto-generate
                        userId = userId,
                        date = data["date"] as? String ?: "",
                        startTime = data["startTime"] as? String ?: "",
                        endTime = data["endTime"] as? String,
                        description = data["description"] as? String,
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        category = data["category"] as? String ?: "",
                        photoUri = data["photoUri"] as? String
                    )

                    // Check if expense already exists locally
                    val existsCount = expenseDao.checkExpenseExists(
                        userId, expense.amount, expense.date ?: "", expense.category ?: ""
                    )

                    if (existsCount == 0) {
                        expenseDao.insertExpense(expense)
                        newExpenses.add(expense)
                        Log.d("ExpenseRepo", "Inserted new expense: ${expense.description}")
                    } else {
                        Log.d("ExpenseRepo", "Expense already exists locally, skipping: ${expense.description}")
                    }
                }
            }

            // Update last sync time
            sharedPrefs?.edit()?.putLong("last_expense_sync_$userId", currentTime)?.apply()
            lastSyncTime = currentTime

            Log.d("ExpenseRepo", "Synced ${newExpenses.size} new expenses from Firebase")
            Result.success(newExpenses)

        } catch (e: Exception) {
            Log.e("ExpenseRepo", "Failed to sync expenses from Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    // Hybrid method: Add expense both locally and to Firebase
    suspend fun addExpense(expense: Expense): Result<Unit> {
        return try {
            Log.d("ExpenseRepo", "Adding expense: ${expense.description} - ${expense.amount}")

            // Always save locally first (offline-first approach)
            expenseDao.insertExpense(expense)
            Log.d("ExpenseRepo", "Expense saved locally")

            // Try to sync to Firebase if user is authenticated
            if (auth.currentUser != null) {
                val firebaseResult = addExpenseToFirebase(expense)
                if (firebaseResult.isSuccess) {
                    Log.d("ExpenseRepo", "Expense synced to Firebase successfully")
                } else {
                    Log.w("ExpenseRepo", "Failed to sync to Firebase, but saved locally")
                }
            } else {
                Log.d("ExpenseRepo", "User not authenticated, saved locally only")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ExpenseRepo", "Failed to add expense: ${e.message}")
            Result.failure(e)
        }
    }

    // Clear duplicate expenses (for testing/cleanup)
    suspend fun clearDuplicateExpenses(userId: String): Int {
        return try {
            val allExpenses = expenseDao.getExpensesByUser(userId)

            // Group by unique identifier and keep only one of each
            val uniqueExpenses = allExpenses.distinctBy {
                "${it.date}_${it.amount}_${it.description}_${it.category}"
            }

            val duplicatesCount = allExpenses.size - uniqueExpenses.size

            if (duplicatesCount > 0) {
                // Clear all expenses for user and re-insert unique ones
                expenseDao.deleteAllUserExpenses(userId)

                for (expense in uniqueExpenses) {
                    expenseDao.insertExpense(expense)
                }

                Log.d("ExpenseRepo", "Removed $duplicatesCount duplicate expenses for user: $userId")
            }

            duplicatesCount
        } catch (e: Exception) {
            Log.e("ExpenseRepo", "Error clearing duplicates: ${e.message}")
            0
        }
    }

    // Reset sync data (for testing)
    suspend fun resetSyncData() {
        context?.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            ?.edit()?.clear()?.apply()
        lastSyncTime = 0L
    }
}