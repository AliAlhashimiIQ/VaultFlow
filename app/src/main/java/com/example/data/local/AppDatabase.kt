package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    exportSchema = true
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
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from schema v1 to v2:
        // - Adds savingsGoalId and savingsGoalTitle columns to transactions table
        // - Creates savings_goals table
        // - Creates user_settings table
        // IMPORTANT: For any future schema changes, add a new Migration object here.
        // NEVER use fallbackToDestructiveMigration() — it wipes all user financial data.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add savings-related columns to transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN savingsGoalId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN savingsGoalTitle TEXT DEFAULT NULL")

                // Create savings_goals table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        targetAmount REAL NOT NULL,
                        currentAmount REAL NOT NULL DEFAULT 0.0,
                        iconName TEXT NOT NULL DEFAULT 'savings',
                        colorHex TEXT NOT NULL DEFAULT '#34D399',
                        targetDate INTEGER,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create user_settings table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        currencyCode TEXT NOT NULL DEFAULT 'IQD',
                        currencySymbol TEXT NOT NULL DEFAULT 'IQD',
                        monthlyBudget REAL NOT NULL DEFAULT 0.0,
                        isSetupCompleted INTEGER NOT NULL DEFAULT 1,
                        themeMode TEXT NOT NULL DEFAULT 'SYSTEM',
                        accentTheme TEXT NOT NULL DEFAULT 'INDIGO'
                    )
                """.trimIndent())
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
                    accentTheme = "EMERALD"
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

            // Fresh install starts with 0 mock transactions and 0 mock savings goals
            // Users start with a clean ledger and clean savings vaults
        }
    }
}
