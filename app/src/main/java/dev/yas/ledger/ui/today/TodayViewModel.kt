package dev.yas.ledger.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.yas.ledger.data.repo.AccountRepository
import dev.yas.ledger.data.repo.SettingsRepository
import dev.yas.ledger.data.repo.TransactionRepository
import dev.yas.ledger.ledger.AccountsSummary
import dev.yas.ledger.ledger.Direction
import dev.yas.ledger.ledger.LedgerTransaction
import dev.yas.ledger.ledger.SpendRing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data object Error : TodayUiState

    data class Ready(
        val today: LocalDate,
        val ring: SpendRing,
        val monthSpentPaise: Long,
        val monthName: String,
        val accounts: AccountsSummary,
        val recent: List<LedgerTransaction>,
        /** True until the user has answered or skipped the first-launch daily limit question. */
        val limitPromptPending: Boolean,
    ) : TodayUiState
}

private const val RECENT_LIMIT = 10
private const val CLOCK_CHECK_MILLIS = 60_000L
private const val BALANCES_AUTO_HIDE_MILLIS = 60_000L

private data class Spend(val today: LocalDate, val dayPaise: Long, val monthPaise: Long, val monthName: String)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)
    private val shown = MutableStateFlow(false)
    private var autoHide: Job? = null

    /** Whether balances are on screen. Never persisted: every launch starts hidden. */
    val balancesShown: StateFlow<Boolean> = shown.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayUiState> = retryTrigger
        .flatMapLatest { buildState().catch { emit(TodayUiState.Error) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    fun retry() {
        retryTrigger.value += 1
    }

    fun toggleBalances() {
        val show = !shown.value
        shown.value = show
        autoHide?.cancel()
        if (show) {
            autoHide = viewModelScope.launch {
                delay(BALANCES_AUTO_HIDE_MILLIS)
                shown.value = false
            }
        }
    }

    fun hideBalances() {
        autoHide?.cancel()
        shown.value = false
    }

    /** Saves the daily limit; null removes it. Either way the first-launch question counts as answered. */
    fun saveLimit(paise: Long?) {
        viewModelScope.launch { settings.setDailyLimit(paise) }
    }

    fun skipLimit() {
        viewModelScope.launch { settings.markLimitPrompted() }
    }

    fun addManual(amountPaise: Long, direction: Direction, note: String?) {
        viewModelScope.launch { transactions.addManual(amountPaise, direction, note, clock.millis()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildState(): Flow<TodayUiState> {
        val spend = dayTicker().distinctUntilChanged().flatMapLatest { date ->
            val zone = clock.zone
            fun startOf(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
            val firstOfMonth = date.withDayOfMonth(1)
            combine(
                transactions.observeSpent(startOf(date), startOf(date.plusDays(1))),
                transactions.observeSpent(startOf(firstOfMonth), startOf(firstOfMonth.plusMonths(1))),
            ) { day, month ->
                Spend(date, day, month, firstOfMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
            }
        }
        return combine(
            spend,
            settings.dailyLimitPaise,
            settings.limitPrompted,
            accounts.observeSummary(),
            transactions.observeRecent(RECENT_LIMIT),
        ) { s, limit, prompted, summary, recent ->
            TodayUiState.Ready(
                today = s.today,
                ring = SpendRing(spentPaise = s.dayPaise, limitPaise = limit),
                monthSpentPaise = s.monthPaise,
                monthName = s.monthName,
                accounts = summary,
                recent = recent,
                limitPromptPending = !prompted,
            )
        }
    }

    /** Emits today's date now and again each minute, so the day rolls over at midnight. */
    private fun dayTicker(): Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(CLOCK_CHECK_MILLIS)
        }
    }
}
