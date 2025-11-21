package com.teamvault.budgetvault.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.teamvault.budgetvault.data.dao.ExpenseDao
import com.teamvault.budgetvault.data.dao.IncomeDao
import com.teamvault.budgetvault.data.dao.UserDao
import com.teamvault.budgetvault.data.model.User
import com.teamvault.budgetvault.data.model.Expense
import com.teamvault.budgetvault.data.model.Income

@Database(entities = [User::class, Expense::class, Income::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "app_database"
                ).fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
