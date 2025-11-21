package com.teamvault.budgetvault.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.teamvault.budgetvault.data.model.Expense

@Dao
interface ExpenseDao {

    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpenseIfNotExists(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("SELECT * FROM Expense")
    suspend fun getAllExpenses(): List<Expense>

    @Query("SELECT * FROM Expense WHERE userId = :userId")
    suspend fun getExpensesByUser(userId: String): List<Expense>

    @Query("SELECT * FROM Expense WHERE userId = :userId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentExpenses(userId: String, limit: Int): List<Expense>

    // Total calculations
    @Query("SELECT SUM(amount) FROM Expense WHERE userId = :userId")
    suspend fun getTotalExpensesByUser(userId: String): Double?

    @Query("SELECT SUM(amount) FROM Expense")
    suspend fun getTotalExpenses(): Double?

    // Date-based queries
    @Query("SELECT * FROM Expense WHERE userId = :userId AND date = :date")
    suspend fun getExpensesByUserAndDate(userId: String, date: String): List<Expense>

    @Query("SELECT SUM(amount) FROM Expense WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    suspend fun getExpensesBetweenDatesByUser(userId: String, startDate: String, endDate: String): Double?

    @Query("SELECT * FROM Expense WHERE userId = :userId AND date = :date ORDER BY id DESC LIMIT 1")
    suspend fun getTodaysLastExpenseByUser(userId: String, date: String): Expense?

    // Category-based queries
    @Query("SELECT * FROM Expense WHERE userId = :userId AND category = :category")
    suspend fun getExpensesByUserAndCategory(userId: String, category: String): List<Expense>

    @Query("SELECT SUM(amount) FROM Expense WHERE userId = :userId AND category = :category AND date >= :sinceDate")
    suspend fun getCategoryExpensesSinceByUser(userId: String, category: String, sinceDate: String): Double?

    // Notification-related queries (user-specific)
    @Query("SELECT SUM(amount) FROM Expense WHERE userId = :userId AND date >= :sinceDate")
    suspend fun getExpensesSinceByUser(userId: String, sinceDate: String): Double?

    @Query("SELECT COUNT(*) FROM Expense WHERE userId = :userId AND date >= :sinceDate")
    suspend fun getExpenseCountSinceByUser(userId: String, sinceDate: String): Int?

    // Notification-related queries (global - for backward compatibility)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM Expense WHERE date >= :sinceDate")
    suspend fun getExpensesSince(sinceDate: String): Double?

    @Query("SELECT COUNT(*) FROM Expense WHERE date >= :sinceDate")
    suspend fun getExpenseCountSince(sinceDate: String): Int?

    // Delete operations
    @Query("DELETE FROM Expense WHERE userId = :userId AND id = :expenseId")
    suspend fun deleteUserExpense(userId: String, expenseId: Int)

    @Query("DELETE FROM Expense WHERE userId = :userId")
    suspend fun deleteAllUserExpenses(userId: String)

    @Query("DELETE FROM Expense")
    suspend fun deleteAllExpenses()

    // Duplicate checking
    @Query("SELECT COUNT(*) FROM Expense WHERE userId = :userId AND amount = :amount AND date = :date AND category = :category")
    suspend fun checkExpenseExists(userId: String, amount: Double, date: String, category: String): Int

    @Query("SELECT COUNT(*) FROM Expense WHERE id = :expenseId AND userId = :userId")
    suspend fun expenseExists(expenseId: String, userId: String): Int

    // Advanced queries for analytics
    @Query("""
        SELECT category, SUM(amount) as total 
        FROM Expense 
        WHERE userId = :userId 
        GROUP BY category 
        ORDER BY total DESC
    """)
    suspend fun getCategoryTotals(userId: String): List<CategoryTotal>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM Expense 
        WHERE userId = :userId AND date BETWEEN :startDate AND :endDate 
        GROUP BY category 
        ORDER BY total DESC
    """)
    suspend fun getCategoryTotalsByDateRange(userId: String, startDate: String, endDate: String): List<CategoryTotal>

    // Monthly/Weekly summaries
    @Query("""
        SELECT * FROM Expense 
        WHERE userId = :userId AND date >= :sinceDate 
        ORDER BY date DESC, id DESC
    """)
    suspend fun getExpensesSinceDate(userId: String, sinceDate: String): List<Expense>

    @Query("""
        SELECT AVG(amount) FROM Expense 
        WHERE userId = :userId AND date >= :sinceDate
    """)
    suspend fun getAverageExpenseAmount(userId: String, sinceDate: String): Double?

    // Search functionality
    @Query("""
        SELECT * FROM Expense 
        WHERE userId = :userId AND 
        (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%')
        ORDER BY date DESC, id DESC
    """)
    suspend fun searchExpenses(userId: String, searchQuery: String): List<Expense>

    // Pagination support
    @Query("""
        SELECT * FROM Expense 
        WHERE userId = :userId 
        ORDER BY date DESC, id DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getExpensesPaginated(userId: String, limit: Int, offset: Int): List<Expense>

    // Count queries
    @Query("SELECT COUNT(*) FROM Expense WHERE userId = :userId")
    suspend fun getExpenseCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM Expense WHERE userId = :userId AND date = :date")
    suspend fun getExpenseCountByDate(userId: String, date: String): Int

    @Query("SELECT COUNT(*) FROM Expense WHERE userId = :userId AND category = :category")
    suspend fun getExpenseCountByCategory(userId: String, category: String): Int
}

// Data class for category totals 
data class CategoryTotal(
    val category: String,
    val total: Double
)