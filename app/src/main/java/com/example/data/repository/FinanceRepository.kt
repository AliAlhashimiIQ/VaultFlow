package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val database: AppDatabase) {
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val settingsDao = database.settingsDao()

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsInRange(startTime, endTime)

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        // If this is a savings deposit linked to a goal, update the goal balance
        if (transaction.type == "SAVINGS_DEPOSIT" && transaction.savingsGoalId != null) {
            savingsGoalDao.addDepositToGoal(transaction.savingsGoalId, transaction.amount)
        }
        return id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun clearAllTransactions() =
        transactionDao.deleteAllTransactions()

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(isIncome: Boolean): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(isIncome)

    suspend fun insertCategory(category: CategoryEntity) =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) {
        val existing = categoryDao.getCategoryById(category.id)
        val oldName = existing?.name ?: category.name
        categoryDao.updateCategory(category)
        transactionDao.updateCategoryInfoForTransactions(
            categoryId = category.id,
            oldName = oldName,
            newName = category.name,
            newIcon = category.iconName,
            newColor = category.colorHex
        )
    }

    suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    // Savings Goals
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity) =
        savingsGoalDao.insertSavingsGoal(goal)

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) =
        savingsGoalDao.updateSavingsGoal(goal)

    suspend fun depositToSavingsGoal(goalId: Long, amount: Double, goalTitle: String, currencySymbol: String) {
        savingsGoalDao.addDepositToGoal(goalId, amount)
        // Also record as a savings transaction
        transactionDao.insertTransaction(
            TransactionEntity(
                amount = amount,
                type = "SAVINGS_DEPOSIT",
                categoryId = -1,
                categoryName = "Savings Goal",
                categoryIcon = "savings",
                categoryColor = "#34D399",
                note = "Deposit to $goalTitle",
                savingsGoalId = goalId,
                savingsGoalTitle = goalTitle,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) =
        savingsGoalDao.deleteSavingsGoal(goal)

    suspend fun deleteSavingsGoalById(id: Long) =
        savingsGoalDao.deleteSavingsGoalById(id)

    // Settings
    val userSettings: Flow<UserSettingsEntity?> = settingsDao.getUserSettings()

    suspend fun getCurrentSettings(): UserSettingsEntity {
        return settingsDao.getUserSettingsDirect() ?: UserSettingsEntity(
            id = 1,
            currencyCode = "IQD",
            currencySymbol = "IQD",
            monthlyBudget = 150000.0,
            isSetupCompleted = true,
            themeMode = "SYSTEM",
            accentTheme = "INDIGO"
        )
    }

    suspend fun updateCurrency(code: String, symbol: String) {
        val current = getCurrentSettings()
        settingsDao.saveUserSettings(current.copy(currencyCode = code, currencySymbol = symbol))
    }

    suspend fun updateMonthlyBudget(budget: Double) {
        val current = getCurrentSettings()
        settingsDao.saveUserSettings(current.copy(monthlyBudget = budget))
    }

    suspend fun updateThemeMode(mode: String) {
        val current = getCurrentSettings()
        settingsDao.saveUserSettings(current.copy(themeMode = mode))
    }

    suspend fun updateAccentTheme(theme: String) {
        val current = getCurrentSettings()
        settingsDao.saveUserSettings(current.copy(accentTheme = theme))
    }

    suspend fun saveUserSettings(settings: UserSettingsEntity) =
        settingsDao.saveUserSettings(settings)

    suspend fun insertAllTransactions(transactions: List<TransactionEntity>) =
        transactionDao.insertAllTransactions(transactions)

    suspend fun insertAllCategories(categories: List<CategoryEntity>) =
        categoryDao.insertAllCategories(categories)

    suspend fun insertAllSavingsGoals(goals: List<SavingsGoalEntity>) =
        savingsGoalDao.insertAllSavingsGoals(goals)

    suspend fun wipeAndRestore(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>? = null,
        goals: List<SavingsGoalEntity>? = null,
        settings: UserSettingsEntity? = null
    ) {
        transactionDao.deleteAllTransactions()
        if (categories != null && categories.isNotEmpty()) {
            categoryDao.deleteAllCategories()
            categoryDao.insertAllCategories(categories)
        }
        if (goals != null && goals.isNotEmpty()) {
            savingsGoalDao.deleteAllSavingsGoals()
            savingsGoalDao.insertAllSavingsGoals(goals)
        }
        if (settings != null) {
            settingsDao.saveUserSettings(settings)
        }
        if (transactions.isNotEmpty()) {
            transactionDao.insertAllTransactions(transactions)
        }
    }

    // Reset / Seed
    suspend fun resetDatabase() {
        transactionDao.deleteAllTransactions()
        savingsGoalDao.deleteAllSavingsGoals()
        categoryDao.deleteAllCategories()
        AppDatabase.populateDefaultData(database)
    }
}
