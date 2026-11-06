package dev.yas.ledger.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.yas.ledger.data.db.AccountDao
import dev.yas.ledger.data.db.AccountEntity
import dev.yas.ledger.data.db.TransactionDao
import dev.yas.ledger.data.db.TransactionEntity
import dev.yas.ledger.data.db.TransactionWithAccount
import dev.yas.ledger.ledger.Account
import dev.yas.ledger.ledger.AccountKind
import dev.yas.ledger.ledger.AccountsSummary
import dev.yas.ledger.ledger.Direction
import dev.yas.ledger.ledger.LedgerTransaction
import dev.yas.ledger.ledger.TxSource
import dev.yas.ledger.ledger.TxType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val DAILY_LIMIT_PAISE = longPreferencesKey("daily_limit_paise")
private val LIMIT_PROMPTED = booleanPreferencesKey("limit_prompted")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Daily limit in paise; null until the user sets one. */
    val dailyLimitPaise: Flow<Long?> = context.settingsStore.data.map { it[DAILY_LIMIT_PAISE] }

    /** True once the first-launch question about the limit has been answered or skipped. */
    val limitPrompted: Flow<Boolean> = context.settingsStore.data.map { it[LIMIT_PROMPTED] ?: false }

    suspend fun setDailyLimit(paise: Long?) {
        context.settingsStore.edit { prefs ->
            if (paise == null) prefs.remove(DAILY_LIMIT_PAISE) else prefs[DAILY_LIMIT_PAISE] = paise
            prefs[LIMIT_PROMPTED] = true
        }
    }

    suspend fun markLimitPrompted() {
        context.settingsStore.edit { it[LIMIT_PROMPTED] = true }
    }
}

@Singleton
class AccountRepository @Inject constructor(
    private val dao: AccountDao,
) {
    fun observeSummary(): Flow<AccountsSummary> = dao.observeMine().map { rows -> rows.toSummary() }

    private fun List<AccountEntity>.toSummary(): AccountsSummary {
        val banks = filter { it.kind == AccountKind.BANK_ACCOUNT }.map { it.toDomain() }
        val wallets = filter { it.kind == AccountKind.WALLET }.map { it.toDomain() }
        val known = banks.mapNotNull { it.balancePaise }
        return AccountsSummary(
            banks = banks,
            wallets = wallets,
            totalBalancePaise = if (known.isEmpty()) null else known.sum(),
            knownBalanceCount = known.size,
        )
    }

    private fun AccountEntity.toDomain() = Account(
        id = id,
        kind = kind,
        displayName = displayName,
        balancePaise = balancePaise,
        balanceAsOf = balanceAsOf,
    )
}

@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) {
    fun observeRecent(limit: Int): Flow<List<LedgerTransaction>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    /** Plain debits in [fromMillis, toMillis), the figure behind the ring and the month line. */
    fun observeSpent(fromMillis: Long, toMillis: Long): Flow<Long> = dao.observeSpent(fromMillis, toMillis)

    suspend fun addManual(amountPaise: Long, direction: Direction, note: String?, nowMillis: Long) {
        dao.insert(
            TransactionEntity(
                accountId = null,
                amountPaise = amountPaise,
                direction = direction,
                type = if (direction == Direction.DEBIT) TxType.DEBIT else TxType.CREDIT,
                occurredAt = nowMillis,
                source = TxSource.MANUAL,
                rawSmsId = null,
                upiRef = null,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                deleted = false,
                createdAt = nowMillis,
            ),
        )
    }

    private fun TransactionWithAccount.toDomain() = LedgerTransaction(
        id = transaction.id,
        accountName = accountName,
        amountPaise = transaction.amountPaise,
        direction = transaction.direction,
        occurredAt = transaction.occurredAt,
        note = transaction.note,
    )
}
