package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    SAVINGS_DEPOSIT
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String, // EXPENSE, INCOME, SAVINGS_DEPOSIT
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val note: String = "",
    val savingsGoalId: Long? = null,
    val savingsGoalTitle: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
