package dev.yas.ledger.data

import androidx.room.Room
import dev.yas.ledger.data.db.AccountEntity
import dev.yas.ledger.data.db.LedgerDatabase
import dev.yas.ledger.data.db.TransactionEntity
import dev.yas.ledger.ledger.AccountKind
import dev.yas.ledger.ledger.Direction
import dev.yas.ledger.ledger.Ownership
import dev.yas.ledger.ledger.TxSource
import dev.yas.ledger.ledger.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SpentQueryTest {
    private lateinit var db: LedgerDatabase

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun close() = db.close()

    private fun account(ownership: Ownership) = AccountEntity(
        kind = AccountKind.BANK_ACCOUNT,
        ownership = ownership,
        displayName = "Test",
        balancePaise = null,
        balanceAsOf = null,
        createdAt = 0,
    )

    private fun tx(
        accountId: Long?,
        paise: Long,
        at: Long = 1_000,
        type: TxType = TxType.DEBIT,
        deleted: Boolean = false,
    ) = TransactionEntity(
        accountId = accountId,
        amountPaise = paise,
        direction = if (type == TxType.CREDIT) Direction.CREDIT else Direction.DEBIT,
        type = type,
        occurredAt = at,
        source = TxSource.MANUAL,
        rawSmsId = null,
        upiRef = null,
        note = null,
        deleted = deleted,
        createdAt = 0,
    )

    private fun spent(from: Long = 0, to: Long = 2_000): Long = runBlocking {
        db.transactionDao().observeSpent(from, to).first()
    }

    @Test
    fun `empty database spends zero`() {
        assertEquals(0L, spent())
    }

    @Test
    fun `counts manual entries and entries on my accounts`() = runBlocking {
        val mine = db.accountDao().insert(account(Ownership.MINE))
        db.transactionDao().insert(tx(accountId = null, paise = 100))
        db.transactionDao().insert(tx(accountId = mine, paise = 250))
        assertEquals(350L, spent())
    }

    @Test
    fun `ignores credits, deleted rows and accounts that are not mine`() = runBlocking {
        val mine = db.accountDao().insert(account(Ownership.MINE))
        val other = db.accountDao().insert(account(Ownership.NOT_MINE))
        val unconfirmed = db.accountDao().insert(account(Ownership.UNCONFIRMED))
        db.transactionDao().insert(tx(mine, 100, type = TxType.CREDIT))
        db.transactionDao().insert(tx(mine, 200, deleted = true))
        db.transactionDao().insert(tx(other, 300))
        db.transactionDao().insert(tx(unconfirmed, 400))
        db.transactionDao().insert(tx(mine, 500, type = TxType.SELF_TRANSFER))
        db.transactionDao().insert(tx(mine, 600, type = TxType.LENT))
        assertEquals(0L, spent())
    }

    @Test
    fun `range is inclusive at the start and exclusive at the end`() = runBlocking {
        db.transactionDao().insert(tx(null, 100, at = 1_000))
        db.transactionDao().insert(tx(null, 200, at = 2_000))
        assertEquals(100L, spent(from = 1_000, to = 2_000))
    }
}
