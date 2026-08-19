package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.entity.UserSettingsEntity
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    ALL("All")
}

enum class TransactionTypeFilter(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    INCOME("Income"),
    SAVINGS("Vault")
}

data class CategorySpendSummary(
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val totalAmount: Double,
    val percentage: Float, // 0.0 to 100.0
    val transactionCount: Int
)

data class DailySpendPoint(
    val dayLabel: String,
    val amount: Double,
    val isToday: Boolean
)

data class AnalyticsState(
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalSavedInVaults: Double = 0.0,
    val netBalance: Double = 0.0,
    val savingsRate: Float = 0.0f,
    val budget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val budgetProgress: Float = 0.0f,
    val dailyAverage: Double = 0.0,
    val projectedMonthTotal: Double = 0.0,
    val categoryBreakdowns: List<CategorySpendSummary> = emptyList(),
    val incomeCategoryBreakdowns: List<CategorySpendSummary> = emptyList(),
    val dailyTrends: List<DailySpendPoint> = emptyList(),
    val incomeTrends: List<DailySpendPoint> = emptyList()
)

data class ImportPreviewData(
    val fileName: String,
    val format: String,
    val transactionCount: Int,
    val categoryCount: Int,
    val goalCount: Int,
    val parsedTransactions: List<TransactionEntity>,
    val parsedCategories: List<CategoryEntity> = emptyList(),
    val parsedGoals: List<SavingsGoalEntity> = emptyList(),
    val parsedSettings: UserSettingsEntity? = null
)

data class ImportResult(
    val success: Boolean,
    val importedCount: Int,
    val message: String
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = FinanceRepository(database)

    // Data streams from Room
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettingsEntity> = repository.userSettings
        .combine(MutableStateFlow(UserSettingsEntity())) { settings, default ->
            settings ?: default
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserSettingsEntity(id = 1, currencyCode = "IQD", currencySymbol = "IQD", monthlyBudget = 150000.0)
        )

    // History filter states
    private val _selectedPeriod = MutableStateFlow(PeriodFilter.THIS_MONTH)
    val selectedPeriod: StateFlow<PeriodFilter> = _selectedPeriod.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    val selectedTypeFilter: StateFlow<TransactionTypeFilter> = _selectedTypeFilter.asStateFlow()

    // Tap Screen State
    private val _tapAmountString = MutableStateFlow("0")
    val tapAmountString: StateFlow<String> = _tapAmountString.asStateFlow()

    private val _tapTransactionType = MutableStateFlow(TransactionType.EXPENSE)
    val tapTransactionType: StateFlow<TransactionType> = _tapTransactionType.asStateFlow()

    private val _tapSelectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val tapSelectedCategory: StateFlow<CategoryEntity?> = _tapSelectedCategory.asStateFlow()

    private val _tapSelectedGoal = MutableStateFlow<SavingsGoalEntity?>(null)
    val tapSelectedGoal: StateFlow<SavingsGoalEntity?> = _tapSelectedGoal.asStateFlow()

    private val _tapNote = MutableStateFlow("")
    val tapNote: StateFlow<String> = _tapNote.asStateFlow()

    private val _tapTimestamp = MutableStateFlow(System.currentTimeMillis())
    val tapTimestamp: StateFlow<Long> = _tapTimestamp.asStateFlow()

    private val _transactionSavedEvent = MutableStateFlow(false)
    val transactionSavedEvent: StateFlow<Boolean> = _transactionSavedEvent.asStateFlow()

    // Import / Export State
    private val _importPreview = MutableStateFlow<ImportPreviewData?>(null)
    val importPreview: StateFlow<ImportPreviewData?> = _importPreview.asStateFlow()

    private val _importResultEvent = MutableStateFlow<ImportResult?>(null)
    val importResultEvent: StateFlow<ImportResult?> = _importResultEvent.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedPeriod,
        _historySearchQuery,
        _selectedCategoryFilter,
        _selectedTypeFilter
    ) { transactions, period, query, categoryFilter, typeFilter ->
        val range = getPeriodRange(period)
        transactions.filter { tx ->
            val matchesRange = (period == PeriodFilter.ALL) || (tx.timestamp in range.first..range.second)
            val matchesCategory = categoryFilter == null || tx.categoryName.equals(categoryFilter, ignoreCase = true)
            val matchesType = when (typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.EXPENSES -> tx.type == "EXPENSE"
                TransactionTypeFilter.INCOME -> tx.type == "INCOME"
                TransactionTypeFilter.SAVINGS -> tx.type == "SAVINGS_DEPOSIT"
            }
            val matchesQuery = query.isBlank() ||
                    tx.categoryName.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)
            matchesRange && matchesCategory && matchesType && matchesQuery
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics Calculation
    val analyticsState: StateFlow<AnalyticsState> = combine(
        allTransactions,
        allCategories,
        userSettings,
        _selectedPeriod
    ) { transactions, categories, settings, period ->
        calculateAnalytics(transactions, categories, settings, period)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())

    init {
        // Initialize default category selection when categories load
        viewModelScope.launch {
            allCategories.collect { categories ->
                if (_tapSelectedCategory.value == null && categories.isNotEmpty()) {
                    val defaultExpense = categories.firstOrNull { !it.isIncome } ?: categories.first()
                    _tapSelectedCategory.value = defaultExpense
                }
            }
        }
    }

    // --- Tap Keypad Handlers ---
    fun onKeypadDigit(digit: String) {
        if (digit == ".") {
            onKeypadDecimal()
            return
        }
        val current = _tapAmountString.value
        if (current == "0") {
            _tapAmountString.value = digit
        } else if (current.length < 10) {
            _tapAmountString.value = current + digit
        }
    }

    fun onNumpadDigit(digit: String) = onKeypadDigit(digit)

    fun onNumpadAddPreset(preset: Double) {
        val current = _tapAmountString.value.toDoubleOrNull() ?: 0.0
        val newAmount = current + preset
        _tapAmountString.value = if (newAmount % 1.0 == 0.0) newAmount.toLong().toString() else newAmount.toString()
    }

    fun onKeypadDecimal() {
        val current = _tapAmountString.value
        if (!current.contains(".")) {
            _tapAmountString.value = "$current."
        }
    }

    fun onKeypadBackspace() {
        val current = _tapAmountString.value
        if (current.length > 1) {
            _tapAmountString.value = current.dropLast(1)
        } else {
            _tapAmountString.value = "0"
        }
    }

    fun onNumpadBackspace() = onKeypadBackspace()

    fun onKeypadClear() {
        _tapAmountString.value = "0"
    }

    fun onNumpadClear() = onKeypadClear()

    fun setTransactionType(type: TransactionType) {
        _tapTransactionType.value = type
        viewModelScope.launch {
            val categories = allCategories.value
            if (type == TransactionType.INCOME) {
                _tapSelectedCategory.value = categories.firstOrNull { it.isIncome } ?: categories.firstOrNull()
            } else if (type == TransactionType.EXPENSE) {
                _tapSelectedCategory.value = categories.firstOrNull { !it.isIncome } ?: categories.firstOrNull()
            } else if (type == TransactionType.SAVINGS_DEPOSIT) {
                val goals = allSavingsGoals.value
                _tapSelectedGoal.value = goals.firstOrNull()
            }
        }
    }

    fun setTapTransactionType(type: TransactionType) = setTransactionType(type)

    fun setSelectedCategory(category: CategoryEntity) {
        _tapSelectedCategory.value = category
    }

    fun setTapCategory(category: CategoryEntity) = setSelectedCategory(category)

    fun setSelectedGoal(goal: SavingsGoalEntity) {
        _tapSelectedGoal.value = goal
    }

    fun setTapGoal(goal: SavingsGoalEntity) = setSelectedGoal(goal)

    fun setNote(note: String) {
        _tapNote.value = note
    }

    fun setTapNote(note: String) = setNote(note)

    fun setTimestamp(timestamp: Long) {
        _tapTimestamp.value = timestamp
    }

    fun setTapTimestamp(timestamp: Long) = setTimestamp(timestamp)

    fun saveTransaction() = saveCurrentTransaction()
    fun consumeSavedEvent() = resetSavedEvent()

    fun saveCurrentTransaction() {
        val amount = _tapAmountString.value.toDoubleOrNull() ?: return
        if (amount <= 0.0) return

        val type = _tapTransactionType.value
        val note = _tapNote.value.trim()
        val timestamp = _tapTimestamp.value

        viewModelScope.launch {
            when (type) {
                TransactionType.EXPENSE, TransactionType.INCOME -> {
                    val cat = _tapSelectedCategory.value ?: return@launch
                    val tx = TransactionEntity(
                        amount = amount,
                        type = type.name,
                        categoryId = cat.id,
                        categoryName = cat.name,
                        categoryIcon = cat.iconName,
                        categoryColor = cat.colorHex,
                        note = note,
                        timestamp = timestamp
                    )
                    repository.insertTransaction(tx)
                }
                TransactionType.SAVINGS_DEPOSIT -> {
                    val goal = _tapSelectedGoal.value ?: return@launch
                    val tx = TransactionEntity(
                        amount = amount,
                        type = TransactionType.SAVINGS_DEPOSIT.name,
                        categoryId = 0,
                        categoryName = "Vault: ${goal.title}",
                        categoryIcon = goal.iconName,
                        categoryColor = goal.colorHex,
                        savingsGoalId = goal.id,
                        savingsGoalTitle = goal.title,
                        note = if (note.isNotBlank()) note else "Deposit into ${goal.title}",
                        timestamp = timestamp
                    )
                    repository.insertTransaction(tx)
                }
            }

            // Reset Tap inputs
            _tapAmountString.value = "0"
            _tapNote.value = ""
            _tapTimestamp.value = System.currentTimeMillis()
            _transactionSavedEvent.value = true
        }
    }

    fun resetSavedEvent() {
        _transactionSavedEvent.value = false
    }

    // --- History & Filtering Handlers ---
    fun setPeriodFilter(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun setSelectedCategoryFilter(categoryName: String?) {
        _selectedCategoryFilter.value = categoryName
    }

    fun setTypeFilter(filter: TransactionTypeFilter) {
        _selectedTypeFilter.value = filter
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // --- Savings Goals ---
    fun addSavingsGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        iconName: String = "savings",
        colorHex: String = "#4F46E5",
        targetDate: Long? = null
    ) {
        createSavingsGoal(title, targetAmount, initialAmount, iconName, colorHex, targetDate)
    }

    fun createSavingsGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        iconName: String = "savings",
        colorHex: String = "#4F46E5",
        targetDate: Long? = null
    ) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                title = title,
                targetAmount = targetAmount,
                currentAmount = initialAmount,
                iconName = iconName,
                colorHex = colorHex,
                targetDate = targetDate,
                isCompleted = initialAmount >= targetAmount
            )
            val id = repository.insertSavingsGoal(goal)
            if (initialAmount > 0.0) {
                repository.insertTransaction(
                    TransactionEntity(
                        amount = initialAmount,
                        type = TransactionType.SAVINGS_DEPOSIT.name,
                        categoryId = 0,
                        categoryName = "Vault: $title",
                        categoryIcon = iconName,
                        categoryColor = colorHex,
                        savingsGoalId = id,
                        savingsGoalTitle = title,
                        note = "Initial deposit",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun depositToSavingsGoal(goalId: Long, amount: Double, goalTitle: String) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val symbol = userSettings.value.currencySymbol
            repository.depositToSavingsGoal(goalId, amount, goalTitle, symbol)
        }
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // --- Settings Handlers ---
    fun updateCurrency(code: String, symbol: String) {
        viewModelScope.launch {
            repository.updateCurrency(code, symbol)
        }
    }

    fun updateMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            repository.updateMonthlyBudget(budget)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }

    fun updateAccentTheme(accent: String) {
        viewModelScope.launch {
            repository.updateAccentTheme(accent)
        }
    }

    fun addCategory(name: String, iconName: String, colorHex: String, isIncome: Boolean) {
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    isIncome = isIncome,
                    isDefault = false
                )
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetDatabase()
        }
    }

    // --- IMPORT & RESTORE SYSTEM ---
    fun loadAndPreviewImport(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            try {
                val fileName = getFileNameFromUri(context, uri) ?: "imported_data"
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: throw IllegalArgumentException("Could not read file content")

                val trimmed = content.trim()
                val isJson = trimmed.startsWith("{") || trimmed.startsWith("[")

                if (isJson) {
                    val preview = parseJsonBackup(trimmed, fileName)
                    _importPreview.value = preview
                } else {
                    val preview = parseCsvLedger(trimmed, fileName)
                    _importPreview.value = preview
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _importResultEvent.value = ImportResult(
                    success = false,
                    importedCount = 0,
                    message = "Import error: ${e.localizedMessage ?: "Invalid file format"}"
                )
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun cancelImportPreview() {
        _importPreview.value = null
    }

    fun confirmImport(mergeMode: Boolean) {
        val preview = _importPreview.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            try {
                val existingCategories = allCategories.value.toMutableList()
                val existingCategoriesByName = existingCategories.associateBy { it.name.lowercase() }.toMutableMap()

                // Create any categories that don't exist yet
                val newCategoriesToAdd = mutableListOf<CategoryEntity>()
                for (tx in preview.parsedTransactions) {
                    val catNameLower = tx.categoryName.lowercase()
                    if (catNameLower.isNotBlank() && !existingCategoriesByName.containsKey(catNameLower) && !catNameLower.startsWith("vault:")) {
                        val isIncome = tx.type == "INCOME"
                        val newCat = CategoryEntity(
                            name = tx.categoryName,
                            iconName = tx.categoryIcon.ifBlank { if (isIncome) "payments" else "category" },
                            colorHex = tx.categoryColor.ifBlank { if (isIncome) "#10B981" else "#4F46E5" },
                            isIncome = isIncome,
                            isDefault = false
                        )
                        newCategoriesToAdd.add(newCat)
                        existingCategoriesByName[catNameLower] = newCat
                    }
                }

                if (mergeMode) {
                    if (newCategoriesToAdd.isNotEmpty()) {
                        repository.insertAllCategories(newCategoriesToAdd)
                    }
                    if (preview.parsedGoals.isNotEmpty()) {
                        repository.insertAllSavingsGoals(preview.parsedGoals)
                    }
                    if (preview.parsedTransactions.isNotEmpty()) {
                        repository.insertAllTransactions(preview.parsedTransactions)
                    }
                } else {
                    // Replace mode (clean restore)
                    val categoriesToRestore = if (preview.parsedCategories.isNotEmpty()) {
                        preview.parsedCategories
                    } else {
                        existingCategories + newCategoriesToAdd
                    }
                    repository.wipeAndRestore(
                        transactions = preview.parsedTransactions,
                        categories = categoriesToRestore,
                        goals = preview.parsedGoals,
                        settings = preview.parsedSettings
                    )
                }

                _importPreview.value = null
                _importResultEvent.value = ImportResult(
                    success = true,
                    importedCount = preview.parsedTransactions.size,
                    message = "Successfully imported ${preview.parsedTransactions.size} transactions!"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _importResultEvent.value = ImportResult(
                    success = false,
                    importedCount = 0,
                    message = "Import failed: ${e.localizedMessage ?: "Unknown error"}"
                )
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun consumeImportResult() {
        _importResultEvent.value = null
    }

    private fun parseJsonBackup(jsonString: String, fileName: String): ImportPreviewData {
        val parsedTransactions = mutableListOf<TransactionEntity>()
        val parsedCategories = mutableListOf<CategoryEntity>()
        val parsedGoals = mutableListOf<SavingsGoalEntity>()
        var parsedSettings: UserSettingsEntity? = null

        if (jsonString.startsWith("[")) {
            // Array of transactions
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                parsedTransactions.add(parseTransactionJson(obj))
            }
        } else {
            val root = JSONObject(jsonString)
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    parsedTransactions.add(parseTransactionJson(txArray.getJSONObject(i)))
                }
            }
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    parsedCategories.add(
                        CategoryEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", "Other"),
                            iconName = obj.optString("iconName", "category"),
                            colorHex = obj.optString("colorHex", "#4F46E5"),
                            isIncome = obj.optBoolean("isIncome", false),
                            isDefault = obj.optBoolean("isDefault", false),
                            sortOrder = obj.optInt("sortOrder", 0)
                        )
                    )
                }
            }
            if (root.has("savingsGoals") || root.has("goals")) {
                val goalArray = root.optJSONArray("savingsGoals") ?: root.optJSONArray("goals") ?: JSONArray()
                for (i in 0 until goalArray.length()) {
                    val obj = goalArray.getJSONObject(i)
                    parsedGoals.add(
                        SavingsGoalEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", "Goal"),
                            targetAmount = obj.optDouble("targetAmount", 0.0),
                            currentAmount = obj.optDouble("currentAmount", 0.0),
                            iconName = obj.optString("iconName", "savings"),
                            colorHex = obj.optString("colorHex", "#4F46E5"),
                            targetDate = if (obj.has("targetDate") && !obj.isNull("targetDate")) obj.getLong("targetDate") else null,
                            isCompleted = obj.optBoolean("isCompleted", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                parsedSettings = UserSettingsEntity(
                    id = 1,
                    currencyCode = sObj.optString("currencyCode", "IQD"),
                    currencySymbol = sObj.optString("currencySymbol", "IQD"),
                    monthlyBudget = sObj.optDouble("monthlyBudget", 150000.0),
                    themeMode = sObj.optString("themeMode", "SYSTEM"),
                    accentTheme = sObj.optString("accentTheme", "INDIGO")
                )
            }
        }

        return ImportPreviewData(
            fileName = fileName,
            format = "JSON Backup",
            transactionCount = parsedTransactions.size,
            categoryCount = parsedCategories.size,
            goalCount = parsedGoals.size,
            parsedTransactions = parsedTransactions,
            parsedCategories = parsedCategories,
            parsedGoals = parsedGoals,
            parsedSettings = parsedSettings
        )
    }

    private fun parseTransactionJson(obj: JSONObject): TransactionEntity {
        val amount = obj.optDouble("amount", 0.0)
        val type = obj.optString("type", "EXPENSE").uppercase()
        val categoryName = obj.optString("categoryName", if (type == "INCOME") "Salary" else "Other")
        val categoryIcon = obj.optString("categoryIcon", if (type == "INCOME") "payments" else "category")
        val categoryColor = obj.optString("categoryColor", if (type == "INCOME") "#10B981" else "#4F46E5")
        val note = obj.optString("note", "")
        val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        val savingsGoalId = if (obj.has("savingsGoalId") && !obj.isNull("savingsGoalId")) obj.getLong("savingsGoalId") else null
        val savingsGoalTitle = obj.optString("savingsGoalTitle", null)

        return TransactionEntity(
            amount = amount,
            type = type,
            categoryId = obj.optLong("categoryId", 0L),
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            note = note,
            savingsGoalId = savingsGoalId,
            savingsGoalTitle = savingsGoalTitle,
            timestamp = timestamp
        )
    }

    private fun parseCsvLedger(csvText: String, fileName: String): ImportPreviewData {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) throw IllegalArgumentException("The CSV file is empty")

        val headerTokens = parseCsvRow(lines.first()).map { it.lowercase() }

        // Find column indices
        var amountIdx = headerTokens.indexOfFirst { it.contains("amount") || it.contains("cost") || it.contains("sum") || it.contains("value") || it.contains("price") }
        var typeIdx = headerTokens.indexOfFirst { it.contains("type") || it.contains("kind") || it.contains("direction") }
        var categoryIdx = headerTokens.indexOfFirst { it.contains("category") || it.contains("cat") || it.contains("tag") || it.contains("group") }
        var noteIdx = headerTokens.indexOfFirst { it.contains("note") || it.contains("description") || it.contains("memo") || it.contains("comment") || it.contains("title") }
        var dateIdx = headerTokens.indexOfFirst { it.contains("date") || it.contains("time") || it.contains("timestamp") || it.contains("day") }

        // Default fallbacks if header doesn't match standard names
        if (amountIdx == -1 && headerTokens.size >= 5) amountIdx = 4
        if (categoryIdx == -1 && headerTokens.size >= 4) categoryIdx = 3
        if (typeIdx == -1 && headerTokens.size >= 3) typeIdx = 2
        if (dateIdx == -1 && headerTokens.size >= 2) dateIdx = 1
        if (noteIdx == -1 && headerTokens.size >= 6) noteIdx = 5

        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        )

        val parsedTransactions = mutableListOf<TransactionEntity>()

        for (i in 1 until lines.size) {
            val tokens = parseCsvRow(lines[i])
            if (tokens.isEmpty()) continue

            var amount = 0.0
            if (amountIdx in tokens.indices) {
                val rawAmount = tokens[amountIdx].replace("[^0-9.-]".toRegex(), "")
                amount = rawAmount.toDoubleOrNull() ?: 0.0
            }
            if (amount == 0.0) continue

            var rawType = if (typeIdx in tokens.indices) tokens[typeIdx].uppercase() else ""
            val rawCategory = if (categoryIdx in tokens.indices) tokens[categoryIdx].trim() else "Other"
            val rawNote = if (noteIdx in tokens.indices) tokens[noteIdx].trim() else ""

            var timestamp = System.currentTimeMillis()
            if (dateIdx in tokens.indices) {
                val rawDate = tokens[dateIdx].trim()
                val millis = rawDate.toLongOrNull()
                if (millis != null && millis > 1000000000L) {
                    timestamp = millis
                } else {
                    for (format in dateFormats) {
                        try {
                            val parsedDate = format.parse(rawDate)
                            if (parsedDate != null) {
                                timestamp = parsedDate.time
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // Infer type if unknown
            val finalType = when {
                rawType.contains("INCOME") || rawType.contains("DEPOSIT") || rawType.contains("EARNING") || rawType.contains("CREDIT") -> "INCOME"
                rawType.contains("VAULT") || rawType.contains("SAVING") -> "SAVINGS_DEPOSIT"
                rawType.contains("EXPENSE") || rawType.contains("SPEND") || rawType.contains("DEBIT") -> "EXPENSE"
                amount < 0 -> "EXPENSE"
                rawCategory.contains("Salary", ignoreCase = true) || rawCategory.contains("Income", ignoreCase = true) -> "INCOME"
                else -> "EXPENSE"
            }

            val absAmount = Math.abs(amount)
            val cleanCategory = if (rawCategory.isBlank()) if (finalType == "INCOME") "Salary" else "Other" else rawCategory
            val defaultColor = if (finalType == "INCOME") "#10B981" else "#4F46E5"
            val defaultIcon = if (finalType == "INCOME") "payments" else "category"

            parsedTransactions.add(
                TransactionEntity(
                    amount = absAmount,
                    type = finalType,
                    categoryId = 0L,
                    categoryName = cleanCategory,
                    categoryIcon = defaultIcon,
                    categoryColor = defaultColor,
                    note = rawNote,
                    timestamp = timestamp
                )
            )
        }

        return ImportPreviewData(
            fileName = fileName,
            format = "CSV Ledger",
            transactionCount = parsedTransactions.size,
            categoryCount = 0,
            goalCount = 0,
            parsedTransactions = parsedTransactions
        )
    }

    private fun parseCsvRow(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        for (c in line) {
            when (c) {
                '"' -> inQuotes = !inQuotes
                ',', ';' -> {
                    if (inQuotes) {
                        sb.append(c)
                    } else {
                        tokens.add(sb.toString().trim())
                        sb.clear()
                    }
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) result = it.getString(idx)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    // --- FULL JSON BACKUP EXPORT ---
    fun exportFullBackupJson(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = allTransactions.value
                val categories = allCategories.value
                val goals = allSavingsGoals.value
                val settings = userSettings.value

                val root = JSONObject()
                root.put("app", "VaultFlow")
                root.put("schemaVersion", 1)
                root.put("exportTimestamp", System.currentTimeMillis())

                // Settings
                val sObj = JSONObject()
                sObj.put("currencyCode", settings.currencyCode)
                sObj.put("currencySymbol", settings.currencySymbol)
                sObj.put("monthlyBudget", settings.monthlyBudget)
                sObj.put("themeMode", settings.themeMode)
                sObj.put("accentTheme", settings.accentTheme)
                root.put("settings", sObj)

                // Categories
                val catArray = JSONArray()
                for (cat in categories) {
                    val cObj = JSONObject()
                    cObj.put("id", cat.id)
                    cObj.put("name", cat.name)
                    cObj.put("iconName", cat.iconName)
                    cObj.put("colorHex", cat.colorHex)
                    cObj.put("isIncome", cat.isIncome)
                    cObj.put("isDefault", cat.isDefault)
                    cObj.put("sortOrder", cat.sortOrder)
                    catArray.put(cObj)
                }
                root.put("categories", catArray)

                // Goals
                val goalArray = JSONArray()
                for (g in goals) {
                    val gObj = JSONObject()
                    gObj.put("id", g.id)
                    gObj.put("title", g.title)
                    gObj.put("targetAmount", g.targetAmount)
                    gObj.put("currentAmount", g.currentAmount)
                    gObj.put("iconName", g.iconName)
                    gObj.put("colorHex", g.colorHex)
                    if (g.targetDate != null) gObj.put("targetDate", g.targetDate)
                    gObj.put("isCompleted", g.isCompleted)
                    gObj.put("createdAt", g.createdAt)
                    goalArray.put(gObj)
                }
                root.put("savingsGoals", goalArray)

                // Transactions
                val txArray = JSONArray()
                for (tx in transactions) {
                    val tObj = JSONObject()
                    tObj.put("id", tx.id)
                    tObj.put("amount", tx.amount)
                    tObj.put("type", tx.type)
                    tObj.put("categoryId", tx.categoryId)
                    tObj.put("categoryName", tx.categoryName)
                    tObj.put("categoryIcon", tx.categoryIcon)
                    tObj.put("categoryColor", tx.categoryColor)
                    tObj.put("note", tx.note)
                    if (tx.savingsGoalId != null) tObj.put("savingsGoalId", tx.savingsGoalId)
                    if (tx.savingsGoalTitle != null) tObj.put("savingsGoalTitle", tx.savingsGoalTitle)
                    tObj.put("timestamp", tx.timestamp)
                    txArray.put(tObj)
                }
                root.put("transactions", txArray)

                val file = File(context.cacheDir, "VaultFlow_Backup_${System.currentTimeMillis()}.json")
                val writer = FileWriter(file)
                writer.write(root.toString(2))
                writer.flush()
                writer.close()

                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "application/json"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    val chooser = Intent.createChooser(sendIntent, "Export VaultFlow Backup JSON")
                    chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- CSV Export ---
    fun exportTransactionsCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = allTransactions.value
                val file = File(context.cacheDir, "VaultFlow_Ledger_${System.currentTimeMillis()}.csv")
                val writer = FileWriter(file)

                writer.append("ID,Date,Type,Category,Amount,Note\n")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                for (tx in transactions) {
                    val dateStr = dateFormat.format(Date(tx.timestamp))
                    val cleanNote = tx.note.replace(",", " ")
                    val cleanCat = tx.categoryName.replace(",", " ")
                    writer.append("${tx.id},$dateStr,${tx.type},$cleanCat,${tx.amount},$cleanNote\n")
                }
                writer.flush()
                writer.close()

                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "text/csv"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    val chooser = Intent.createChooser(sendIntent, "Export Ledger CSV")
                    chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Period Calculation Helpers ---
    private fun getPeriodRange(period: PeriodFilter): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        return when (period) {
            PeriodFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                Pair(start, calendar.timeInMillis)
            }
            PeriodFilter.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                Pair(start, now)
            }
            PeriodFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                Pair(start, now)
            }
            PeriodFilter.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                Pair(start, now)
            }
            PeriodFilter.ALL -> Pair(0L, Long.MAX_VALUE)
        }
    }

    private fun calculateAnalytics(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        settings: UserSettingsEntity,
        period: PeriodFilter
    ): AnalyticsState {
        val range = getPeriodRange(period)
        val periodTransactions = if (period == PeriodFilter.ALL) {
            transactions
        } else {
            transactions.filter { it.timestamp in range.first..range.second }
        }

        var totalSpent = 0.0
        var totalIncome = 0.0
        var totalSavedInVaults = 0.0

        val expenseCategoryMap = mutableMapOf<String, Triple<Double, Int, Pair<String, String>>>()
        val incomeCategoryMap = mutableMapOf<String, Triple<Double, Int, Pair<String, String>>>()

        for (tx in periodTransactions) {
            val matchedCat = categories.firstOrNull { it.id == tx.categoryId || it.name.equals(tx.categoryName, ignoreCase = true) }
            val catName = matchedCat?.name ?: tx.categoryName
            val catIcon = matchedCat?.iconName ?: tx.categoryIcon
            val catColor = matchedCat?.colorHex ?: tx.categoryColor

            when (tx.type) {
                "EXPENSE" -> {
                    totalSpent += tx.amount
                    val prev = expenseCategoryMap[catName]
                    val prevAmount = prev?.first ?: 0.0
                    val prevCount = prev?.second ?: 0
                    expenseCategoryMap[catName] = Triple(
                        prevAmount + tx.amount,
                        prevCount + 1,
                        Pair(catIcon, catColor)
                    )
                }
                "SAVINGS_DEPOSIT" -> {
                    totalSavedInVaults += tx.amount
                    totalSpent += tx.amount
                    val prev = expenseCategoryMap[catName]
                    val prevAmount = prev?.first ?: 0.0
                    val prevCount = prev?.second ?: 0
                    expenseCategoryMap[catName] = Triple(
                        prevAmount + tx.amount,
                        prevCount + 1,
                        Pair(catIcon, catColor)
                    )
                }
                "INCOME" -> {
                    totalIncome += tx.amount
                    val prev = incomeCategoryMap[catName]
                    val prevAmount = prev?.first ?: 0.0
                    val prevCount = prev?.second ?: 0
                    incomeCategoryMap[catName] = Triple(
                        prevAmount + tx.amount,
                        prevCount + 1,
                        Pair(catIcon, catColor)
                    )
                }
            }
        }

        val expenseCategorySummaries = expenseCategoryMap.map { (catName, data) ->
            val percentage = if (totalSpent > 0) ((data.first / totalSpent) * 100.0).toFloat() else 0f
            CategorySpendSummary(
                categoryName = catName,
                iconName = data.third.first,
                colorHex = data.third.second,
                totalAmount = data.first,
                percentage = percentage,
                transactionCount = data.second
            )
        }.sortedByDescending { it.totalAmount }

        val incomeCategorySummaries = incomeCategoryMap.map { (catName, data) ->
            val percentage = if (totalIncome > 0) ((data.first / totalIncome) * 100.0).toFloat() else 0f
            CategorySpendSummary(
                categoryName = catName,
                iconName = data.third.first,
                colorHex = data.third.second,
                totalAmount = data.first,
                percentage = percentage,
                transactionCount = data.second
            )
        }.sortedByDescending { it.totalAmount }

        // Budget Calculations
        val budget = settings.monthlyBudget
        val budgetRemaining = budget - totalSpent
        val budgetProgress = if (budget > 0) (totalSpent / budget).toFloat().coerceIn(0f, 1f) else 0f

        // Daily average & Pace calculation
        val calendar = Calendar.getInstance()
        val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyAverage = if (currentDayOfMonth > 0) totalSpent / currentDayOfMonth.toDouble() else totalSpent
        val projectedMonthTotal = dailyAverage * daysInMonth.toDouble()

        val netBalance = totalIncome - totalSpent
        val savingsRate = if (totalIncome > 0.0) {
            ((netBalance / totalIncome) * 100.0).toFloat().coerceIn(-100f, 100f)
        } else {
            0f
        }

        // Calculate Daily Trends (Expenses vs Income last 7 days)
        val dailyTrends = mutableListOf<DailySpendPoint>()
        val incomeTrends = mutableListOf<DailySpendPoint>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayKey = dayKeyFormat.format(Date())

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val expenseTotal = transactions.filter {
                (it.type == "EXPENSE" || it.type == "SAVINGS_DEPOSIT") && it.timestamp in dayStart..dayEnd
            }.sumOf { it.amount }

            val incomeTotal = transactions.filter {
                it.type == "INCOME" && it.timestamp in dayStart..dayEnd
            }.sumOf { it.amount }

            val label = dayFormat.format(Date(dayStart))
            val isCurrentDay = dayKeyFormat.format(Date(dayStart)) == todayKey

            dailyTrends.add(DailySpendPoint(dayLabel = label, amount = expenseTotal, isToday = isCurrentDay))
            incomeTrends.add(DailySpendPoint(dayLabel = label, amount = incomeTotal, isToday = isCurrentDay))
        }

        return AnalyticsState(
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            totalSavedInVaults = totalSavedInVaults,
            netBalance = netBalance,
            savingsRate = savingsRate,
            budget = budget,
            budgetRemaining = budgetRemaining,
            budgetProgress = budgetProgress,
            dailyAverage = dailyAverage,
            projectedMonthTotal = projectedMonthTotal,
            categoryBreakdowns = expenseCategorySummaries,
            incomeCategoryBreakdowns = incomeCategorySummaries,
            dailyTrends = dailyTrends,
            incomeTrends = incomeTrends
        )
    }
}
