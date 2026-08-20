package com.example.ui.screens.tap

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CategoryIconMapper
import com.example.ui.components.CurrencyHelper
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay

@Composable
fun TapScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val amountString by viewModel.tapAmountString.collectAsStateWithLifecycle()
    val transactionType by viewModel.tapTransactionType.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.tapSelectedCategory.collectAsStateWithLifecycle()
    val selectedGoal by viewModel.tapSelectedGoal.collectAsStateWithLifecycle()
    val note by viewModel.tapNote.collectAsStateWithLifecycle()
    val timestamp by viewModel.tapTimestamp.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.allSavingsGoals.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val savedEvent by viewModel.transactionSavedEvent.collectAsStateWithLifecycle()

    val extendedColors = LocalExtendedColors.current
    val haptic = LocalHapticFeedback.current
    var showSuccessBanner by remember { mutableStateOf(false) }

    val formattedAmount = try {
        val num = amountString.toDoubleOrNull() ?: 0.0
        if (amountString.endsWith(".")) {
            "${CurrencyHelper.formatCurrency(num, settings.currencyCode, settings.currencySymbol)}."
        } else {
            CurrencyHelper.formatCurrency(num, settings.currencyCode, settings.currencySymbol)
        }
    } catch (e: Exception) {
        "${settings.currencySymbol} $amountString"
    }

    val amountColor = when (transactionType) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
        TransactionType.INCOME -> extendedColors.incomeGreen
        TransactionType.SAVINGS_DEPOSIT -> extendedColors.vaultViolet
    }

    LaunchedEffect(savedEvent) {
        if (savedEvent) {
            showSuccessBanner = true
            delay(900)
            showSuccessBanner = false
            viewModel.consumeSavedEvent()
        }
    }

    val displayCategories = remember(transactionType, categories) {
        val filtered = if (transactionType == TransactionType.INCOME) {
            categories.filter { it.isIncome }
        } else {
            categories.filter { !it.isIncome }
        }
        if (filtered.isNotEmpty()) {
            filtered
        } else {
            if (transactionType == TransactionType.INCOME) {
                listOf(
                    CategoryEntity(id = -101L, name = "Salary", iconName = "payments", colorHex = "#10B981", isIncome = true, isDefault = true),
                    CategoryEntity(id = -102L, name = "Freelance", iconName = "laptop", colorHex = "#06B6D4", isIncome = true, isDefault = true),
                    CategoryEntity(id = -103L, name = "Investments", iconName = "trending_up", colorHex = "#3B82F6", isIncome = true, isDefault = true),
                    CategoryEntity(id = -104L, name = "Bonus / Gift", iconName = "redeem", colorHex = "#F59E0B", isIncome = true, isDefault = true),
                    CategoryEntity(id = -105L, name = "Other Income", iconName = "account_balance_wallet", colorHex = "#8B5CF6", isIncome = true, isDefault = true)
                )
            } else {
                listOf(
                    CategoryEntity(id = -1L, name = "Groceries", iconName = "shopping_cart", colorHex = "#10B981", isIncome = false, isDefault = true),
                    CategoryEntity(id = -2L, name = "Dining Out", iconName = "restaurant", colorHex = "#F59E0B", isIncome = false, isDefault = true),
                    CategoryEntity(id = -3L, name = "Transport", iconName = "directions_car", colorHex = "#3B82F6", isIncome = false, isDefault = true),
                    CategoryEntity(id = -4L, name = "Other", iconName = "category", colorHex = "#64748B", isIncome = false, isDefault = true)
                )
            }
        }
    }

    LaunchedEffect(transactionType, displayCategories) {
        if (transactionType == TransactionType.SAVINGS_DEPOSIT) {
            if (selectedGoal == null && savingsGoals.isNotEmpty()) {
                viewModel.setTapGoal(savingsGoals.first())
            }
        } else {
            // If current selected category doesn't match the active type (e.g. was expense, now income), select first valid category
            if (displayCategories.isNotEmpty() && (selectedCategory == null || displayCategories.none { it.id == selectedCategory?.id && it.name == selectedCategory?.name })) {
                viewModel.setTapCategory(displayCategories.first())
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- TOP SECTION: Type selector & Amount Display ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type Selector: [ Expense | Income | Savings Vault ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (extendedColors.isDark) Color(0xFF141D2E) else Color(0xFFE2E8F0))
                    .border(1.dp, extendedColors.borderSubtle, RoundedCornerShape(26.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TypeTabButton(
                    title = "Expense",
                    isSelected = transactionType == TransactionType.EXPENSE,
                    activeBgColor = if (extendedColors.isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    activeTextColor = Color.White,
                    onClick = { viewModel.setTapTransactionType(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f)
                )
                TypeTabButton(
                    title = "Income",
                    isSelected = transactionType == TransactionType.INCOME,
                    activeBgColor = extendedColors.incomeGreen,
                    activeTextColor = Color.White,
                    onClick = { viewModel.setTapTransactionType(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f)
                )
                TypeTabButton(
                    title = "Savings Vault",
                    isSelected = transactionType == TransactionType.SAVINGS_DEPOSIT,
                    activeBgColor = extendedColors.vaultViolet,
                    activeTextColor = Color.White,
                    onClick = { viewModel.setTapTransactionType(TransactionType.SAVINGS_DEPOSIT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle & Amount
            Text(
                text = when (transactionType) {
                    TransactionType.EXPENSE -> "HOW MUCH DID YOU SPEND?"
                    TransactionType.INCOME -> "HOW MUCH DID YOU EARN?"
                    TransactionType.SAVINGS_DEPOSIT -> "HOW MUCH TO DEPOSIT IN VAULT?"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Display Amount Hero Container (CashApp / Linear style animated digits)
            AnimatedContent(
                targetState = formattedAmount,
                transitionSpec = {
                    if (targetState.length > initialState.length) {
                        (fadeIn(animationSpec = tween(100)) +
                         scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)))
                            .togetherWith(fadeOut(animationSpec = tween(80)))
                    } else {
                        fadeIn(animationSpec = tween(100))
                            .togetherWith(fadeOut(animationSpec = tween(80)) + scaleOut(targetScale = 0.96f))
                    }
                },
                label = "amount_morph"
            ) { targetText: String ->
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (targetText.length > 8) 36.sp else 46.sp,
                        lineHeight = if (targetText.length > 8) 42.sp else 52.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .testTag("tap_amount_display")
                )
            }

            // Success feedback animation banner (Spring entrance)
            AnimatedVisibility(
                visible = showSuccessBanner,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) +
                        androidx.compose.animation.scaleIn(initialScale = 0.85f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)),
                exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(120))
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(extendedColors.incomeGreen.copy(alpha = 0.18f))
                        .border(1.dp, extendedColors.incomeGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = extendedColors.incomeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saved to Ledger!",
                        style = MaterialTheme.typography.labelMedium,
                        color = extendedColors.incomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- CATEGORIES / GOALS SELECTOR ROW ---
            if (transactionType == TransactionType.SAVINGS_DEPOSIT) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = savingsGoals,
                        key = { if (it.id != 0L) "goal_${it.id}" else "goal_${it.title}_${it.colorHex}" }
                    ) { goal ->
                        val isSelected = selectedGoal?.id == goal.id
                        GoalChip(
                            goal = goal,
                            isSelected = isSelected,
                            onClick = { viewModel.setTapGoal(goal) }
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = displayCategories,
                        key = { if (it.id != 0L) "cat_${it.id}" else "cat_${it.name}_${it.isIncome}" }
                    ) { category ->
                        val isSelected = selectedCategory?.id == category.id
                        CategoryChip(
                            category = category,
                            isSelected = isSelected,
                            onClick = { viewModel.setTapCategory(category) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- NORMAL PERMANENT NOTE INPUT FIELD (IMPROVED UX) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.setTapNote(it) },
                    placeholder = {
                        Text(
                            text = "Add note / description...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (note.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.setTapNote("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear note",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = extendedColors.cardBackground,
                        unfocusedContainerColor = extendedColors.cardBackground,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("tap_note_input")
                )

                // Date Toggle Button
                Surface(
                    onClick = {
                        val oneDay = 24 * 60 * 60 * 1000L
                        val isToday = CurrencyHelper.formatDateHeader(timestamp) == "TODAY"
                        viewModel.setTapTimestamp(if (isToday) System.currentTimeMillis() - oneDay else System.currentTimeMillis())
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("date_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CurrencyHelper.formatDateHeader(timestamp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // --- MIDDLE SECTION: Numpad Grid ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = if (settings.currencyCode == "IQD") {
                listOf(500.0, 1000.0, 5000.0, 10000.0)
            } else {
                listOf(5.0, 10.0, 25.0, 50.0)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    Surface(
                        onClick = { viewModel.onNumpadAddPreset(preset) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("preset_${preset.toInt()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "+${if (preset % 1.0 == 0.0) preset.toLong().toString() else preset.toString()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeypadButton("1", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("1") }
                KeypadButton("2", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("2") }
                KeypadButton("3", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("3") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeypadButton("4", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("4") }
                KeypadButton("5", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("5") }
                KeypadButton("6", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("6") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeypadButton("7", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("7") }
                KeypadButton("8", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("8") }
                KeypadButton("9", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("9") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeypadButton(".", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit(".") }
                KeypadButton("0", modifier = Modifier.weight(1f)) { viewModel.onNumpadDigit("0") }
                KeypadIconButton(
                    icon = Icons.AutoMirrored.Rounded.Backspace,
                    modifier = Modifier.weight(1f),
                    onLongClick = { viewModel.onNumpadClear() },
                    onClick = { viewModel.onNumpadBackspace() }
                )
            }
        }

        // --- BOTTOM SECTION: Save Button (Physical tactile press feedback)
        val canSave = (amountString.toDoubleOrNull() ?: 0.0) > 0.0
        val saveInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        val isSavePressed by saveInteractionSource.collectIsPressedAsState()
        val saveScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isSavePressed && canSave) 0.96f else 1.0f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            ),
            label = "save_button_scale"
        )

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.saveTransaction()
            },
            enabled = canSave,
            interactionSource = saveInteractionSource,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (transactionType) {
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.primary
                    TransactionType.INCOME -> extendedColors.incomeGreen
                    TransactionType.SAVINGS_DEPOSIT -> extendedColors.vaultViolet
                },
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (canSave) 3.dp else 0.dp,
                pressedElevation = 1.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .scale(saveScale)
                .testTag("save_transaction_button")
        ) {
            Text(
                text = "Save",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (canSave) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun TypeTabButton(
    title: String,
    isSelected: Boolean,
    activeBgColor: Color,
    activeTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) activeBgColor else Color.Transparent,
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeTextColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else extendedColors.cardBackground,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, extendedColors.borderSubtle)
        },
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier
            .height(40.dp)
            .testTag("category_chip_${category.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconName = category.iconName,
                colorHex = category.colorHex,
                size = 24,
                iconSize = 14
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GoalChip(
    goal: SavingsGoalEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else extendedColors.cardBackground,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, extendedColors.borderSubtle)
        },
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier
            .height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconName = goal.iconName,
                colorHex = goal.colorHex,
                size = 24,
                iconSize = 14
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = goal.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "keypad_press_scale"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = if (isPressed) extendedColors.keypadKeyActive else extendedColors.keypadKey,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else extendedColors.borderSubtle
        ),
        shadowElevation = if (isPressed) 0.dp else 1.dp,
        modifier = modifier
            .height(58.dp)
            .scale(scale)
            .testTag("keypad_$text")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "keypad_icon_scale"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isPressed) extendedColors.keypadKeyActive else extendedColors.keypadKey,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else extendedColors.borderSubtle
        ),
        shadowElevation = if (isPressed) 0.dp else 1.dp,
        modifier = modifier
            .height(58.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("keypad_backspace")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Backspace",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
