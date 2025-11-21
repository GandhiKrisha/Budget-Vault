package com.teamvault.budgetvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Incomes")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val amount: Double,
    val sourceOfIncome: String,
    val description: String? = null
)