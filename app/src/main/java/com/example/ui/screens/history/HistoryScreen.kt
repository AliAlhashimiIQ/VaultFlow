package com.example.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CurrencyHelper
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.FilterPillGroup
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.TransactionTypeFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()

    val extendedColors = LocalExtendedColors.current
    var showSearchBar by remember { mutableStateOf(false) }
    var selectedTransactionForEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    val groupedTransactions = remember(transactions) {
        val groups = linkedMapOf<String, MutableList<TransactionEntity>>()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (tx in transactions) {
            val key = dayFormat.format(Date(tx.timestamp))
            val list = groups.getOrPut(key) { mutableListOf() }
            list.add(tx)
        }
        groups
    }

    val totalAmount = remember(transactions, selectedTypeFilter) {
        when (selectedTypeFilter) {
            TransactionTypeFilter.INCOME -> transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            TransactionTypeFilter.SAVINGS -> transactions.filter { it.type == "SAVINGS_DEPOSIT" }.sumOf { it.amount }
            else -> transactions.filter { it.type != "INCOME" }.sumOf { it.amount }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- TOP APP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRANSACTION LEDGER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) viewModel.setHistorySearchQuery("")
                },
                modifier = Modifier.testTag("history_search_toggle_button")
            ) {
                Icon(
                    imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = "Search Transactions",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // --- OPTIONAL SEARCH BAR ---
        AnimatedVisibility(
            visible = showSearchBar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setHistorySearchQuery(it) },
                placeholder = { Text("Search by category, note, or amount...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("history_search_text_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedContainerColor = extendedColors.cardBackground
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setHistorySearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }

        // --- PERIOD FILTER PILLS ---
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            FilterPillGroup(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { viewModel.setPeriodFilter(it) }
            )
        }

        // --- TYPE SELECTOR: [ ALL | EXPENSES | INCOME | VAULT ] ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                TransactionTypeFilter.entries.forEach { filter ->
                    val isSelected = selectedTypeFilter == filter
                    Surface(
                        onClick = { viewModel.setTypeFilter(filter) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- CATEGORY HORIZONTAL FILTER CHIPS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "All Categories" chip
            val isAllSelected = selectedCategoryFilter == null
            Surface(
                onClick = { viewModel.setSelectedCategoryFilter(null) },
                shape = RoundedCornerShape(12.dp),
                color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else extendedColors.cardBackground,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAllSelected) MaterialTheme.colorScheme.primary else extendedColors.borderSubtle
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "All Categories",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            categories.forEach { cat ->
                val isSelected = selectedCategoryFilter.equals(cat.name, ignoreCase = true)
                Surface(
                    onClick = {
                        if (isSelected) viewModel.setSelectedCategoryFilter(null)
                        else viewModel.setSelectedCategoryFilter(cat.name)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else extendedColors.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else extendedColors.borderSubtle
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryIconBadge(
                            iconName = cat.iconName,
                            colorHex = cat.colorHex,
                            size = 18,
                            iconSize = 12
                        )
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- SUB-HEADER: TRANSACTION SUMMARY HERO CARD ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = extendedColors.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, extendedColors.borderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (selectedTypeFilter) {
                            TransactionTypeFilter.INCOME -> "Total Income"
                            TransactionTypeFilter.SAVINGS -> "Total In Vaults"
                            TransactionTypeFilter.EXPENSES -> "Total Expenses"
                            TransactionTypeFilter.ALL -> "Net Period Total"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${transactions.size} Record${if (transactions.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                val isIncomeFilter = selectedTypeFilter == TransactionTypeFilter.INCOME
                val totalPrefix = if (isIncomeFilter) "+" else ""
                val totalColor = if (isIncomeFilter) extendedColors.incomeGreen else MaterialTheme.colorScheme.onSurface
                Text(
                    text = "$totalPrefix${CurrencyHelper.formatCurrency(totalAmount, settings.currencyCode, settings.currencySymbol)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = totalColor
                )
            }
        }

        // --- TRANSACTIONS LIST ---
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No records found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try switching period filters or add a new transaction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedTransactions.forEach { (dateKey, itemsInDate) ->
                    item(key = "header_$dateKey") {
                        val headerLabel = formatHeaderDate(dateKey)
                        val dayExpenses = itemsInDate.filter { it.type != "INCOME" }.sumOf { it.amount }
                        val dayIncome = itemsInDate.filter { it.type == "INCOME" }.sumOf { it.amount }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = headerLabel.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (dayIncome > 0) {
                                    Text(
                                        text = "+${CurrencyHelper.formatCurrency(dayIncome, settings.currencyCode, settings.currencySymbol)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = extendedColors.incomeGreen
                                    )
                                }
                                if (dayExpenses > 0) {
                                    Text(
                                        text = "-${CurrencyHelper.formatCurrency(dayExpenses, settings.currencyCode, settings.currencySymbol)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = extendedColors.expenseRed
                                    )
                                }
                            }
                        }
                    }

                    items(
                        items = itemsInDate,
                        key = { it.id },
                        contentType = { "transaction_item" }
                    ) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            categories = categories,
                            currencyCode = settings.currencyCode,
                            currencySymbol = settings.currencySymbol,
                            onClick = { selectedTransactionForEdit = tx }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // --- EDIT TRANSACTION DIALOG ---
    selectedTransactionForEdit?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            categories = categories,
            currencySymbol = settings.currencySymbol,
            onDismiss = { selectedTransactionForEdit = null },
            onSave = { updated ->
                viewModel.updateTransaction(updated)
                selectedTransactionForEdit = null
            },
            onDelete = { toDelete ->
                viewModel.deleteTransaction(toDelete)
                selectedTransactionForEdit = null
            }
        )
    }
}

@Composable
private fun TransactionRowItem(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    currencyCode: String,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(transaction.timestamp) { timeFormat.format(Date(transaction.timestamp)) }

    val isIncome = transaction.type == "INCOME"
    val isSavings = transaction.type == "SAVINGS_DEPOSIT"

    val matchedCategory = remember(categories, transaction.categoryId, transaction.categoryName, transaction.type) {
        if (isSavings) {
            null
        } else {
            categories.firstOrNull { it.isIncome == isIncome && (it.id == transaction.categoryId || it.name.equals(transaction.categoryName, ignoreCase = true)) }
                ?: categories.firstOrNull { it.name.equals(transaction.categoryName, ignoreCase = true) }
        }
    }
    val displayIcon = when {
        isSavings -> transaction.categoryIcon
        matchedCategory != null -> matchedCategory.iconName
        isIncome -> "payments"
        else -> transaction.categoryIcon
    }
    val displayColor = when {
        isSavings -> transaction.categoryColor
        matchedCategory != null -> matchedCategory.colorHex
        isIncome -> "#10B981"
        else -> transaction.categoryColor
    }
    val displayName = when {
        isSavings -> transaction.categoryName
        matchedCategory != null -> matchedCategory.name
        isIncome -> if (transaction.categoryName.isBlank() || transaction.categoryName.equals("Transport", ignoreCase = true)) "Salary" else transaction.categoryName
        else -> transaction.categoryName
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = extendedColors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, extendedColors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_row_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                CategoryIconBadge(
                    iconName = displayIcon,
                    colorHex = displayColor,
                    size = 46,
                    iconSize = 24
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (transaction.note.isNotBlank()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = transaction.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val sign = when {
                    isIncome -> "+"
                    isSavings -> "↓ "
                    else -> "-"
                }
                val amountColor = when {
                    isIncome -> extendedColors.incomeGreen
                    isSavings -> MaterialTheme.colorScheme.primary
                    else -> extendedColors.expenseRed
                }

                Text(
                    text = "$sign${CurrencyHelper.formatCurrency(transaction.amount, currencyCode, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                val typeLabel = when {
                    isIncome -> "Income"
                    isSavings -> "Vault Deposit"
                    else -> "Expense"
                }
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatHeaderDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = inputFormat.parse(dateString) ?: return dateString

        val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = todayFormat.format(Date())

        val calYesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = todayFormat.format(calYesterday.time)

        when (dateString) {
            todayStr -> "Today"
            yesterdayStr -> "Yesterday"
            else -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        dateString
    }
}
