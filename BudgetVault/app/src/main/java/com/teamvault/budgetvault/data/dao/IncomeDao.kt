package com.teamvault.budgetvault.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teamvault.budgetvault.data.model.Income

@Dao
interface IncomeDao {
    @Insert
    suspend fun insertIncome(income: Income)

    @Query("SELECT SUM(amount) FROM Incomes")
    suspend fun getTotalIncome(): Double?

    @Query("SELECT * FROM INCOMES ORDER BY date DESC")
    suspend fun getAllIncome(): List<Income>
}