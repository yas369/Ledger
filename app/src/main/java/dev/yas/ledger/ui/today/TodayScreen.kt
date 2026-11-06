package dev.yas.ledger.ui.today

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yas.ledger.R
import dev.yas.ledger.ledger.Account
import dev.yas.ledger.ledger.AccountsSummary
import dev.yas.ledger.ledger.Direction
import dev.yas.ledger.ledger.LedgerTransaction
import dev.yas.ledger.ledger.SpendLevel
import dev.yas.ledger.ledger.SpendRing
import dev.yas.ledger.ledger.formatPlainRupees
import dev.yas.ledger.ledger.formatRupees
import dev.yas.ledger.ledger.parseRupeesToPaise
import dev.yas.ledger.ui.components.AmountField
import dev.yas.ledger.ui.components.EyeIcon
import dev.yas.ledger.ui.components.GroupedSection
import dev.yas.ledger.ui.components.LText
import dev.yas.ledger.ui.components.LedgerSheet
import dev.yas.ledger.ui.components.PlainTextButton
import dev.yas.ledger.ui.components.PlusIcon
import dev.yas.ledger.ui.components.PrimaryButton
import dev.yas.ledger.ui.components.SectionDivider
import dev.yas.ledger.ui.components.SegmentedControl
import dev.yas.ledger.ui.components.pressable
import dev.yas.ledger.ui.theme.LedgerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun TodayRoute(viewModel: TodayViewModel) {
    val state by viewModel.state.collectAsState()
    val balancesShown by viewModel.balancesShown.collectAsState()
    TodayScreen(
        state = state,
        balancesShown = balancesShown,
        onToggleBalances = viewModel::toggleBalances,
        onRetry = viewModel::retry,
        onSaveLimit = viewModel::saveLimit,
        onSkipLimit = viewModel::skipLimit,
        onAddManual = viewModel::addManual,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    balancesShown: Boolean,
    onToggleBalances: () -> Unit,
    onRetry: () -> Unit,
    onSaveLimit: (Long?) -> Unit,
    onSkipLimit: () -> Unit,
    onAddManual: (Long, Direction, String?) -> Unit,
) {
    val colors = LedgerTheme.colors
    Box(modifier = Modifier.fillMaxSize().background(colors.grouped)) {
        when (state) {
            TodayUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LText(stringResource(R.string.loading), LedgerTheme.type.body, colors.textSecondary)
            }

            TodayUiState.Error -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LText(stringResource(R.string.error_load), LedgerTheme.type.body, colors.textSecondary)
                PlainTextButton(label = stringResource(R.string.try_again), onClick = onRetry)
            }

            is TodayUiState.Ready -> if (state.limitPromptPending) {
                FirstRunLimit(onContinue = { onSaveLimit(it) }, onSkip = onSkipLimit)
            } else {
                ReadyContent(state, balancesShown, onToggleBalances, onSaveLimit, onAddManual)
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: TodayUiState.Ready,
    balancesShown: Boolean,
    onToggleBalances: () -> Unit,
    onSaveLimit: (Long?) -> Unit,
    onAddManual: (Long, Direction, String?) -> Unit,
) {
    val colors = LedgerTheme.colors
    var showAdd by remember { mutableStateOf(false) }
    var showLimit by remember { mutableStateOf(false) }
    val addDescription = stringResource(R.string.add_button_description)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                val dateText = state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                LText(dateText, LedgerTheme.type.footnote, colors.textSecondary)
            }
            item { SpendRingSection(state.ring, onChangeLimit = { showLimit = true }) }
            item {
                LText(
                    text = stringResource(
                        R.string.month_spent,
                        formatRupees(state.monthSpentPaise),
                        state.monthName,
                    ),
                    style = LedgerTheme.type.subheadlineAmount,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            item { AccountsCard(state.accounts, balancesShown, onToggleBalances) }
            item { RecentSection(state.recent, state.today, onAdd = { showAdd = true }) }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
                .size(56.dp)
                .semantics { contentDescription = addDescription }
                .pressable(onClick = { showAdd = true })
                .clip(CircleShape)
                .background(colors.card),
            contentAlignment = Alignment.Center,
        ) {
            PlusIcon(tint = colors.accent)
        }

        if (showAdd) {
            AddSheet(
                onCancel = { showAdd = false },
                onSave = { amount, direction, note ->
                    onAddManual(amount, direction, note)
                    showAdd = false
                },
            )
        }
        if (showLimit) {
            LimitSheet(
                currentPaise = state.ring.limitPaise,
                onCancel = { showLimit = false },
                onSave = { paise ->
                    onSaveLimit(paise)
                    showLimit = false
                },
            )
        }
    }
}

@Composable
private fun SpendRingSection(ring: SpendRing, onChangeLimit: () -> Unit) {
    val colors = LedgerTheme.colors
    val type = LedgerTheme.type
    val fill = when (ring.level) {
        SpendLevel.OK -> colors.ok
        SpendLevel.NEAR -> colors.near
        SpendLevel.OVER -> colors.over
    }
    val animated by animateFloatAsState(targetValue = ring.fraction, label = "ring")
    val spent = formatRupees(ring.spentPaise)
    val remaining = ring.remainingPaise
    val description = when {
        remaining == null -> stringResource(R.string.spend_ring_description_no_limit, spent)
        remaining < 0 -> stringResource(
            R.string.spend_ring_description_over,
            spent,
            formatRupees(ring.limitPaise ?: 0L),
            formatRupees(abs(remaining)),
        )
        else -> stringResource(
            R.string.spend_ring_description,
            spent,
            formatRupees(ring.limitPaise ?: 0L),
            formatRupees(remaining),
        )
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .semantics(mergeDescendants = true) { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 12.dp.toPx()
                val topLeft = Offset(strokePx / 2, strokePx / 2)
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                drawArc(
                    color = colors.separator,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx),
                )
                if (animated > 0f) {
                    drawArc(
                        color = fill,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (remaining == null) {
                    LText(spent, type.largeTitleAmount, colors.text)
                    LText(stringResource(R.string.spent_today), type.footnote, colors.textSecondary)
                } else {
                    val over = remaining < 0
                    LText(
                        text = formatRupees(abs(remaining)),
                        style = type.largeTitleAmount,
                        color = if (ring.level == SpendLevel.OK) colors.text else fill,
                    )
                    LText(
                        text = stringResource(if (over) R.string.over_limit else R.string.left_today),
                        style = type.footnote,
                        color = if (over) colors.over else colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (remaining == null) {
            LText(stringResource(R.string.no_limit_set), type.subheadline, colors.textSecondary)
            PlainTextButton(label = stringResource(R.string.set_daily_limit), onClick = onChangeLimit)
        } else {
            LText(
                text = stringResource(
                    R.string.spent_of_limit,
                    spent,
                    formatRupees(ring.limitPaise ?: 0L),
                ),
                style = type.subheadlineAmount,
                color = colors.textSecondary,
            )
            PlainTextButton(label = stringResource(R.string.change_limit), onClick = onChangeLimit)
        }
    }
}

@Composable
private fun AccountsCard(summary: AccountsSummary, shown: Boolean, onToggle: () -> Unit) {
    val colors = LedgerTheme.colors
    val type = LedgerTheme.type
    val context = LocalContext.current
    val hasAccounts = summary.banks.isNotEmpty() || summary.wallets.isNotEmpty()

    GroupedSection {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                if (!hasAccounts) {
                    LText(stringResource(R.string.no_accounts), type.body, colors.textSecondary)
                } else {
                    val count = summary.bankCount
                    LText(
                        text = context.resources.getQuantityString(R.plurals.accounts_count, count, count),
                        style = type.body,
                        color = colors.text,
                    )
                    if (!shown) {
                        LText(stringResource(R.string.balances_hidden), type.subheadline, colors.textSecondary)
                    } else if (summary.knownBalanceCount < summary.bankCount) {
                        LText(
                            text = stringResource(
                                R.string.balances_partial,
                                summary.knownBalanceCount,
                                summary.bankCount,
                            ),
                            style = type.subheadline,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            if (hasAccounts) {
                if (shown) {
                    LText(
                        text = summary.totalBalancePaise?.let(::formatRupees)
                            ?: stringResource(R.string.balance_unknown),
                        style = type.headlineAmount,
                        color = colors.text,
                    )
                } else {
                    LText(stringResource(R.string.balance_mask), type.headline, colors.textSecondary)
                }
                val eyeLabel = stringResource(if (shown) R.string.hide_balances else R.string.show_balances)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = eyeLabel }
                        .pressable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    EyeIcon(open = shown, tint = colors.accent)
                }
            }
        }
        if (shown) {
            summary.banks.forEach { account ->
                SectionDivider()
                BalanceRow(account)
            }
            if (summary.wallets.isNotEmpty()) {
                SectionDivider()
                LText(
                    text = stringResource(R.string.wallets_label),
                    style = type.subheadline,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                )
                summary.wallets.forEach { account ->
                    SectionDivider()
                    BalanceRow(account)
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(account: Account) {
    val colors = LedgerTheme.colors
    val type = LedgerTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LText(account.displayName, type.body, colors.text)
            val asOf = account.balanceAsOf
            LText(
                text = if (asOf == null) {
                    stringResource(R.string.balance_none_yet)
                } else {
                    stringResource(R.string.balance_as_of, formatDateTime(asOf))
                },
                style = type.subheadline,
                color = colors.textSecondary,
            )
        }
        LText(
            text = account.balancePaise?.let(::formatRupees) ?: stringResource(R.string.balance_unknown),
            style = type.headlineAmount,
            color = colors.text,
        )
    }
}

@Composable
private fun RecentSection(recent: List<LedgerTransaction>, today: LocalDate, onAdd: () -> Unit) {
    val colors = LedgerTheme.colors
    val type = LedgerTheme.type
    Column(modifier = Modifier.fillMaxWidth()) {
        LText(
            text = stringResource(R.string.recent_title),
            style = type.title3,
            color = colors.text,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        GroupedSection {
            if (recent.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LText(stringResource(R.string.recent_empty), type.body, colors.textSecondary)
                    PlainTextButton(label = stringResource(R.string.add_one), onClick = onAdd)
                }
            } else {
                recent.forEachIndexed { index, tx ->
                    if (index > 0) SectionDivider()
                    TransactionRow(tx, today)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: LedgerTransaction, today: LocalDate) {
    val colors = LedgerTheme.colors
    val type = LedgerTheme.type
    val context = LocalContext.current
    val fallbackTitle = stringResource(if (tx.direction == Direction.DEBIT) R.string.money_out else R.string.money_in)
    val title = tx.note ?: fallbackTitle
    val account = tx.accountName ?: stringResource(R.string.manual_account)
    val whenText = describeWhen(context, tx.occurredAt, today, stringResource(R.string.day_yesterday))
    val amount = formatRupees(tx.amountPaise)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LText(title, type.body, colors.text, maxLines = 1)
            LText(
                text = stringResource(R.string.row_caption, account, whenText),
                style = type.subheadline,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        if (tx.direction == Direction.CREDIT) {
            LText(stringResource(R.string.credit_amount, amount), type.headlineAmount, colors.credit)
        } else {
            LText(amount, type.headlineAmount, colors.text)
        }
    }
}

@Composable
private fun AddSheet(onCancel: () -> Unit, onSave: (Long, Direction, String?) -> Unit) {
    val colors = LedgerTheme.colors
    var amount by remember { mutableStateOf("") }
    var directionIndex by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    val paise = parseRupeesToPaise(amount)
    val direction = if (directionIndex == 0) Direction.DEBIT else Direction.CREDIT

    LedgerSheet(
        title = stringResource(R.string.add_title),
        cancelLabel = stringResource(R.string.cancel),
        confirmLabel = stringResource(R.string.save),
        confirmEnabled = paise != null,
        onCancel = onCancel,
        onConfirm = { if (paise != null) onSave(paise, direction, note.ifBlank { null }) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.grouped)
                .padding(16.dp),
        ) {
            LText(stringResource(R.string.amount_label), LedgerTheme.type.footnote, colors.textSecondary)
            AmountField(value = amount, onValueChange = { amount = it })
        }
        Spacer(Modifier.height(16.dp))
        SegmentedControl(
            options = listOf(stringResource(R.string.debit), stringResource(R.string.credit)),
            selectedIndex = directionIndex,
            onSelect = { directionIndex = it },
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.grouped)
                .padding(16.dp),
        ) {
            LText(stringResource(R.string.note_label), LedgerTheme.type.footnote, colors.textSecondary)
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LedgerTheme.type.body.copy(color = colors.text),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                cursorBrush = SolidColor(colors.accent),
            )
        }
    }
}

@Composable
private fun LimitSheet(currentPaise: Long?, onCancel: () -> Unit, onSave: (Long?) -> Unit) {
    val colors = LedgerTheme.colors
    var text by remember { mutableStateOf(currentPaise?.let(::formatPlainRupees).orEmpty()) }
    val parsed = parseRupeesToPaise(text)
    val valid = text.isBlank() || parsed != null

    LedgerSheet(
        title = stringResource(R.string.limit_sheet_title),
        cancelLabel = stringResource(R.string.cancel),
        confirmLabel = stringResource(R.string.save),
        confirmEnabled = valid,
        onCancel = onCancel,
        onConfirm = { onSave(parsed) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.grouped)
                .padding(16.dp),
        ) {
            LText(stringResource(R.string.limit_hint), LedgerTheme.type.footnote, colors.textSecondary)
            AmountField(value = text, onValueChange = { text = it })
        }
    }
}

@Composable
private fun FirstRunLimit(onContinue: (Long?) -> Unit, onSkip: () -> Unit) {
    val colors = LedgerTheme.colors
    var text by remember { mutableStateOf("") }
    val parsed = parseRupeesToPaise(text)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LText(
            text = stringResource(R.string.first_run_limit_title),
            style = LedgerTheme.type.title2,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        AmountField(value = text, onValueChange = { text = it })
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            label = stringResource(R.string.continue_label),
            onClick = { onContinue(parsed) },
            enabled = parsed != null,
        )
        PlainTextButton(
            label = stringResource(R.string.set_later),
            onClick = onSkip,
            color = colors.textSecondary,
        )
    }
}

private fun formatDateTime(millis: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        .format(Date(millis))

/** "4:05 pm" today, "Yesterday, 4:05 pm" the day before, otherwise "3 Nov, 4:05 pm". */
private fun describeWhen(
    context: Context,
    millis: Long,
    today: LocalDate,
    yesterdayWord: String,
): String {
    val time = DateFormat.getTimeFormat(context).format(Date(millis))
    val day = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (day) {
        today -> time
        today.minusDays(1) -> "$yesterdayWord, $time"
        else -> "${day.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}, $time"
    }
}
