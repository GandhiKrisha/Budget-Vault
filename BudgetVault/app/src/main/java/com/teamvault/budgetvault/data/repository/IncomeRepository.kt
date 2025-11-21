package com.teamvault.budgetvault.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teamvault.budgetvault.data.dao.IncomeDao
import com.teamvault.budgetvault.data.model.Income
import kotlinx.coroutines.tasks.await

class IncomeRepository(
    private val incomeDao: IncomeDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // Local Room operations
    suspend fun insertIncomeLocal(income: Income) = incomeDao.insertIncome(income)

    suspend fun getAllIncomeLocal(): List<Income> = incomeDao.getAllIncome()

    // Get total income for current user only
    suspend fun getTotalIncomeLocal(): Double? {
        // Since Income doesn't have userId field, we'll need to add it
        // For now, return total income (will fix this with model update)
        return incomeDao.getTotalIncome()
    }

    // Firebase operations
    suspend fun addIncomeToFirebase(income: Income): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d("IncomeRepo", "Adding income to Firebase for user: $userId")

            val incomeData = mapOf(
                "userId" to userId,
                "date" to income.date,
                "amount" to income.amount,
                "sourceOfIncome" to income.sourceOfIncome,
                "description" to income.description,
                "localId" to income.id, // Add local ID to prevent duplicates
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            val docRef = firestore.collection("users")
                .document(userId)
                .collection("income")
                .add(incomeData)
                .await()

            Log.d("IncomeRepo", "Income added to Firebase with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("IncomeRepo", "Failed to add income to Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncIncomeFromFirebase(): Result<List<Income>> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d("IncomeRepo", "Syncing income from Firebase for user: $userId")

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("income")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val incomeList = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    Income(
                        id = 0, // Room will auto-generate
                        date = data["date"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        sourceOfIncome = data["sourceOfIncome"] as? String ?: "",
                        description = data["description"] as? String
                    )
                } else null
            }

            Log.d("IncomeRepo", "Synced ${incomeList.size} income records from Firebase")

            // Clear existing income and add synced data to avoid duplicates
            // Note: This is a simple approach, ideally we'd check for duplicates
            incomeList.forEach { income ->
                incomeDao.insertIncome(income)
            }

            Result.success(incomeList)
        } catch (e: Exception) {
            Log.e("IncomeRepo", "Failed to sync income from Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    // Hybrid method: Add income both locally and to Firebase
    suspend fun addIncome(income: Income): Result<Unit> {
        return try {
            Log.d("IncomeRepo", "Adding income: ${income.sourceOfIncome} - ${income.amount}")

            // Always save locally first (offline-first approach)
            incomeDao.insertIncome(income)
            Log.d("IncomeRepo", "Income saved locally")

            // Try to sync to Firebase if user is authenticated
            if (auth.currentUser != null) {
                val firebaseResult = addIncomeToFirebase(income)
                if (firebaseResult.isSuccess) {
                    Log.d("IncomeRepo", "Income synced to Firebase successfully")
                } else {
                    Log.w("IncomeRepo", "Failed to sync to Firebase, but saved locally")
                }
            } else {
                Log.d("IncomeRepo", "User not authenticated, saved locally only")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("IncomeRepo", "Failed to add income: ${e.message}")
            Result.failure(e)
        }
    }
}