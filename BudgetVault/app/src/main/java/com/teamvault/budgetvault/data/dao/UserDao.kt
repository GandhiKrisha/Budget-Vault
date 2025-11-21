package com.teamvault.budgetvault.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teamvault.budgetvault.data.model.User

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser (user: User)

    @Query ("SELECT * FROM user WHERE email = :email AND password = :password LIMIT 1")
    suspend fun getUser (email: String, password: String): User?

    @Query("SELECT * FROM user WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

}