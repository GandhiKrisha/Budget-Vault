package com.teamvault.budgetvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Expense")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // Foreign key to user
    val date: String,
    val startTime: String,
    val endTime: String?,
    val description: String?,
    val amount: Double,
    val category: String,
    val photoUri: String? = null
)

