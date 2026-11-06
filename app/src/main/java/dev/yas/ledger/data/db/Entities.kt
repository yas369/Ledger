package dev.yas.ledger.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.yas.ledger.ledger.AccountKind
import dev.yas.ledger.ledger.Direction
import dev.yas.ledger.ledger.IgnoreReason
import dev.yas.ledger.ledger.Ownership
import dev.yas.ledger.ledger.RawSmsStatus
import dev.yas.ledger.ledger.TxSource
import dev.yas.ledger.ledger.TxType

/** The original message. Append-only: rows are never deleted so history can be re-read. */
@Entity(tableName = "raw_sms", indices = [Index(value = ["contentHash"], unique = true)])
data class RawSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderRaw: String,
    val senderKey: String,
    val body: String,
    val receivedAt: Long,
    val contentHash: String,
    val status: RawSmsStatus,
    val ignoreReason: IgnoreReason?,
    val createdAt: Long,
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: AccountKind,
    val ownership: Ownership,
    val displayName: String,
    /** Last balance a bank SMS reported, in paise. Null until one is seen. */
    val balancePaise: Long?,
    /** Receive time of the SMS that balance came from. */
    val balanceAsOf: Long?,
    val createdAt: Long,
)

/** One way an account appears in messages: a sender plus the cleaned digits. */
@Entity(
    tableName = "account_identifiers",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["accountId"]), Index(value = ["senderKey", "digits"], unique = true)],
)
data class AccountIdentifierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val senderKey: String,
    /** Cleaned digit run; empty for wallets. */
    val digits: String,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = RawSmsEntity::class,
            parentColumns = ["id"],
            childColumns = ["rawSmsId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["accountId"]), Index(value = ["rawSmsId"]), Index(value = ["occurredAt"])],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Null for manual entries made without choosing an account. */
    val accountId: Long?,
    /** Integer paise; floating point never touches an amount. */
    val amountPaise: Long,
    val direction: Direction,
    val type: TxType,
    val occurredAt: Long,
    val source: TxSource,
    val rawSmsId: Long?,
    val upiRef: String?,
    val note: String?,
    val deleted: Boolean,
    val createdAt: Long,
)
