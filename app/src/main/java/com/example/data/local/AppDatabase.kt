package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.dao.SettingsDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        SavingsGoalEntity::class,
        UserSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_tracker_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insert initial default values in background thread
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultData(database)
                    }
                }
            }
        }

        suspend fun populateDefaultData(database: AppDatabase) {
            val settingsDao = database.settingsDao()
            val categoryDao = database.categoryDao()
            val savingsGoalDao = database.savingsGoalDao()
            val transactionDao = database.transactionDao()

            // Default User Settings (IQD Iraqi Dinar default matching user screenshot, configurable anytime)
            settingsDao.saveUserSettings(
                UserSettingsEntity(
                    id = 1,
                    currencyCode = "IQD",
                    currencySymbol = "IQD",
                    monthlyBudget = 150000.0,
                    isSetupCompleted = true,
                    themeMode = "SYSTEM",
                    accentTheme = "INDIGO"
                )
            )

            // Default Categories
            val defaultCategories = listOf(
                CategoryEntity(name = "Subscription", iconName = "subscriptions", colorHex = "#FACC15", isIncome = false, isDefault = true, sortOrder = 1),
                CategoryEntity(name = "Other", iconName = "category", colorHex = "#F43F5E", isIncome = false, isDefault = true, sortOrder = 2),
                CategoryEntity(name = "Personal", iconName = "person", colorHex = "#38BDF8", isIncome = false, isDefault = true, sortOrder = 3),
                CategoryEntity(name = "Market", iconName = "shopping_cart", colorHex = "#EC4899", isIncome = false, isDefault = true, sortOrder = 4),
                CategoryEntity(name = "Food", iconName = "fastfood", colorHex = "#FB923C", isIncome = false, isDefault = true, sortOrder = 5),
                CategoryEntity(name = "Transport", iconName = "directions_car", colorHex = "#2DD4BF", isIncome = false, isDefault = true, sortOrder = 6),
                CategoryEntity(name = "Bills", iconName = "receipt_long", colorHex = "#A855F7", isIncome = false, isDefault = true, sortOrder = 7),
                CategoryEntity(name = "Entertainment", iconName = "movie", colorHex = "#F472B6", isIncome = false, isDefault = true, sortOrder = 8),
                CategoryEntity(name = "Health", iconName = "medical_services", colorHex = "#EF4444", isIncome = false, isDefault = true, sortOrder = 9),
                CategoryEntity(name = "Shopping", iconName = "shopping_bag", colorHex = "#60A5FA", isIncome = false, isDefault = true, sortOrder = 10),
                CategoryEntity(name = "Home", iconName = "home", colorHex = "#34D399", isIncome = false, isDefault = true, sortOrder = 11),
                CategoryEntity(name = "Education", iconName = "school", colorHex = "#818CF8", isIncome = false, isDefault = true, sortOrder = 12),

                // Income Categories
                CategoryEntity(name = "Salary", iconName = "payments", colorHex = "#10B981", isIncome = true, isDefault = true, sortOrder = 1),
                CategoryEntity(name = "Freelance", iconName = "laptop", colorHex = "#06B6D4", isIncome = true, isDefault = true, sortOrder = 2),
                CategoryEntity(name = "Investments", iconName = "trending_up", colorHex = "#3B82F6", isIncome = true, isDefault = true, sortOrder = 3),
                CategoryEntity(name = "Bonus / Gift", iconName = "redeem", colorHex = "#F59E0B", isIncome = true, isDefault = true, sortOrder = 4),
                CategoryEntity(name = "Other Income", iconName = "account_balance_wallet", colorHex = "#8B5CF6", isIncome = true, isDefault = true, sortOrder = 5)
            )
            categoryDao.insertAllCategories(defaultCategories)

            // Default Savings Goals (Emergency Fund, Vacation, New Laptop)
            val defaultGoals = listOf(
                SavingsGoalEntity(
                    title = "Emergency Fund",
                    targetAmount = 1000000.0,
                    currentAmount = 350000.0,
                    iconName = "shield",
                    colorHex = "#38BDF8",
                    targetDate = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)
                ),
                SavingsGoalEntity(
                    title = "New Laptop",
                    targetAmount = 800000.0,
                    currentAmount = 520000.0,
                    iconName = "laptop",
                    colorHex = "#34D399",
                    targetDate = System.currentTimeMillis() + (45L * 24 * 60 * 60 * 1000)
                ),
                SavingsGoalEntity(
                    title = "Vacation Trip",
                    targetAmount = 600000.0,
                    currentAmount = 180000.0,
                    iconName = "flight",
                    colorHex = "#FBBF24",
                    targetDate = System.currentTimeMillis() + (120L * 24 * 60 * 60 * 1000)
                )
            )
            savingsGoalDao.insertAllSavingsGoals(defaultGoals)

            // Seed a few initial transactions matching the screenshot aesthetic so the app is instantly rich and functional on first launch
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L
            val initialTransactions = listOf(
                TransactionEntity(
                    amount = 750.0,
                    type = "EXPENSE",
                    categoryId = 4,
                    categoryName = "Market",
                    categoryIcon = "shopping_cart",
                    categoryColor = "#EC4899",
                    note = "Groceries & Milk",
                    timestamp = now - (2 * 3600 * 1000L)
                ),
                TransactionEntity(
                    amount = 10000.0,
                    type = "EXPENSE",
                    categoryId = 3,
                    categoryName = "Personal",
                    categoryIcon = "person",
                    categoryColor = "#38BDF8",
                    note = "Barber & Grooming",
                    timestamp = now - (6 * 3600 * 1000L)
                ),
                TransactionEntity(
                    amount = 2000.0,
                    type = "EXPENSE",
                    categoryId = 5,
                    categoryName = "Food",
                    categoryIcon = "fastfood",
                    categoryColor = "#FB923C",
                    note = "Dinner burger",
                    timestamp = now - dayMillis - (3 * 3600 * 1000L)
                ),
                TransactionEntity(
                    amount = 1000.0,
                    type = "EXPENSE",
                    categoryId = 4,
                    categoryName = "Market",
                    categoryIcon = "shopping_cart",
                    categoryColor = "#EC4899",
                    note = "Snacks & Drinks",
                    timestamp = now - dayMillis - (5 * 3600 * 1000L)
                ),
                TransactionEntity(
                    amount = 5500.0,
                    type = "EXPENSE",
                    categoryId = 5,
                    categoryName = "Food",
                    categoryIcon = "fastfood",
                    categoryColor = "#FB923C",
                    note = "Lunch meal",
                    timestamp = now - dayMillis - (7 * 3600 * 1000L)
                ),
                TransactionEntity(
                    amount = 1250.0,
                    type = "EXPENSE",
                    categoryId = 6,
                    categoryName = "Transport",
                    categoryIcon = "directions_car",
                    categoryColor = "#2DD4BF",
                    note = "Taxi ride",
                    timestamp = now - dayMillis - (9 * 3600 * 1000L)
                ),
                TransactionEntity(
                    amount = 19400.0,
                    type = "EXPENSE",
                    categoryId = 1,
                    categoryName = "Subscription",
                    categoryIcon = "subscriptions",
                    categoryColor = "#FACC15",
                    note = "Cloud & Media Subs",
                    timestamp = now - (2 * dayMillis)
                ),
                TransactionEntity(
                    amount = 16000.0,
                    type = "EXPENSE",
                    categoryId = 2,
                    categoryName = "Other",
                    categoryIcon = "category",
                    categoryColor = "#F43F5E",
                    note = "Hardware tools",
                    timestamp = now - (3 * dayMillis)
                ),
                TransactionEntity(
                    amount = 45900.0,
                    type = "EXPENSE",
                    categoryId = 6,
                    categoryName = "Transport",
                    categoryIcon = "directions_car",
                    categoryColor = "#2DD4BF",
                    note = "Car service & fuel",
                    timestamp = now - (5 * dayMillis)
                ),
                TransactionEntity(
                    amount = 250000.0,
                    type = "INCOME",
                    categoryId = 13,
                    categoryName = "Salary",
                    categoryIcon = "payments",
                    categoryColor = "#10B981",
                    note = "Monthly payment",
                    timestamp = now - (6 * dayMillis)
                )
            )
            transactionDao.insertAllTransactions(initialTransactions)
        }
    }
}
