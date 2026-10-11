package com.marshall.pyerite.characterTransactionsModule.data

import android.content.Context
import com.marshall.pyerite.characterTransactionsModule.model.CharacterMarketTransaction
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionLocation
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsConfig
import com.marshall.pyerite.characterTransactionsModule.model.CharacterTransactionsDateFormatter
import com.marshall.pyerite.infra.network.PyeriteJson
import kotlinx.serialization.Serializable
import java.io.File
import java.util.Calendar

/**
 * Full personal wallet-transaction history for one character.
 * A refresh appends newer rows; it does not replace this file with the latest ESI page.
 */
internal class CharacterTransactionsDiskCache(
    context: Context,
) {
    private val directory = File(
        context.applicationContext.filesDir,
        CharacterTransactionsConfig.DISK_CACHE_DIR,
    )

    fun read(characterId: Long): CachedCharacterTransactions? {
        val file = fileFor(characterId)
        if (!file.isFile) return null
        return runCatching {
            PyeriteJson.decodeFromString<CachedCharacterTransactionsFile>(file.readText()).toCached()
        }.getOrElse {
            file.delete()
            null
        }
    }

    fun write(
        characterId: Long,
        savedAtEpochMs: Long,
        transactions: List<CharacterMarketTransaction>,
    ) {
        directory.mkdirs()
        val target = fileFor(characterId)
        val payload = CachedCharacterTransactionsFile.from(
            savedAtEpochMs = savedAtEpochMs,
            transactions = transactions,
        )
        val temporary = File(directory, target.name + CharacterTransactionsConfig.DISK_CACHE_TMP_SUFFIX)
        temporary.writeText(PyeriteJson.encodeToString(payload))
        if (target.exists() && !target.delete()) return
        if (!temporary.renameTo(target)) {
            target.writeText(temporary.readText())
            temporary.delete()
        }
    }

    /**
     * When every character file together holds at least
     * [CharacterTransactionsConfig.CLEANUP_MIN_ROW_COUNT] rows, drop rows older than
     * [CharacterTransactionsConfig.CLEANUP_RETENTION_YEARS].
     */
    fun removeExpiredTransactions(nowEpochMs: Long = System.currentTimeMillis()) {
        if (!directory.isDirectory) return
        val snapshots = directory.listFiles().orEmpty().mapNotNull { file ->
            val characterId = characterIdFrom(file) ?: return@mapNotNull null
            val cached = read(characterId) ?: return@mapNotNull null
            characterId to cached
        }
        val totalRows = snapshots.sumOf { (_, cached) -> cached.transactions.size }
        if (totalRows < CharacterTransactionsConfig.CLEANUP_MIN_ROW_COUNT) return
        val cutoffEpochMs = retentionCutoffEpochMs(nowEpochMs)
        snapshots.forEach { (characterId, cached) ->
            val kept = cached.transactions.filter { it.dateEpochMs >= cutoffEpochMs }
            if (kept.size == cached.transactions.size) return@forEach
            write(
                characterId = characterId,
                savedAtEpochMs = cached.savedAtEpochMs,
                transactions = kept,
            )
        }
    }

    private fun fileFor(characterId: Long): File = File(
        directory,
        CharacterTransactionsConfig.DISK_CACHE_FILE_PREFIX +
            characterId +
            CharacterTransactionsConfig.DISK_CACHE_FILE_SUFFIX,
    )

    private fun characterIdFrom(file: File): Long? {
        val name = file.name
        if (!name.startsWith(CharacterTransactionsConfig.DISK_CACHE_FILE_PREFIX) ||
            !name.endsWith(CharacterTransactionsConfig.DISK_CACHE_FILE_SUFFIX)
        ) {
            return null
        }
        return name
            .removePrefix(CharacterTransactionsConfig.DISK_CACHE_FILE_PREFIX)
            .removeSuffix(CharacterTransactionsConfig.DISK_CACHE_FILE_SUFFIX)
            .toLongOrNull()
    }

    private fun retentionCutoffEpochMs(nowEpochMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowEpochMs
        calendar.add(Calendar.YEAR, -CharacterTransactionsConfig.CLEANUP_RETENTION_YEARS)
        return calendar.timeInMillis
    }
}

internal data class CachedCharacterTransactions(
    val savedAtEpochMs: Long,
    val transactions: List<CharacterMarketTransaction>,
) {
    fun isFresh(nowEpochMs: Long): Boolean {
        if (transactions.isEmpty()) return false
        val ageMs = nowEpochMs - savedAtEpochMs
        return ageMs >= 0L && ageMs < CharacterTransactionsConfig.DISK_CACHE_TTL_MS
    }
}

@Serializable
private data class CachedCharacterTransactionsFile(
    val savedAtEpochMs: Long,
    val transactions: List<CachedCharacterMarketTransaction> = emptyList(),
) {
    fun toCached(): CachedCharacterTransactions = CachedCharacterTransactions(
        savedAtEpochMs = savedAtEpochMs,
        transactions = transactions.map { it.toModel() },
    )

    companion object {
        fun from(
            savedAtEpochMs: Long,
            transactions: List<CharacterMarketTransaction>,
        ): CachedCharacterTransactionsFile = CachedCharacterTransactionsFile(
            savedAtEpochMs = savedAtEpochMs,
            transactions = transactions.map(CachedCharacterMarketTransaction::from),
        )
    }
}

@Serializable
private data class CachedCharacterMarketTransaction(
    val transactionId: Long,
    val dateEpochMs: Long,
    val typeId: Int,
    val locationId: Long,
    val zhName: String? = null,
    val enName: String? = null,
    val name: String? = null,
    val iconFilename: String? = null,
    val quantity: Long,
    val unitPrice: Double,
    val isBuy: Boolean,
    val location: CachedCharacterTransactionLocation? = null,
) {
    fun toModel(): CharacterMarketTransaction = CharacterMarketTransaction(
        transactionId = transactionId,
        dateEpochMs = dateEpochMs,
        dayKey = CharacterTransactionsDateFormatter.dayKey(dateEpochMs),
        typeId = typeId,
        locationId = locationId,
        zhName = zhName,
        enName = enName,
        name = name,
        iconFilename = iconFilename,
        quantity = quantity,
        unitPrice = unitPrice,
        isBuy = isBuy,
        location = location?.toModel(),
    )

    companion object {
        fun from(transaction: CharacterMarketTransaction): CachedCharacterMarketTransaction =
            CachedCharacterMarketTransaction(
                transactionId = transaction.transactionId,
                dateEpochMs = transaction.dateEpochMs,
                typeId = transaction.typeId,
                locationId = transaction.locationId,
                zhName = transaction.zhName,
                enName = transaction.enName,
                name = transaction.name,
                iconFilename = transaction.iconFilename,
                quantity = transaction.quantity,
                unitPrice = transaction.unitPrice,
                isBuy = transaction.isBuy,
                location = transaction.location?.let(CachedCharacterTransactionLocation::from),
            )
    }
}

@Serializable
private data class CachedCharacterTransactionLocation(
    val securityStatus: Double? = null,
    val systemZhName: String? = null,
    val systemEnName: String? = null,
    val systemName: String? = null,
    val placeName: String? = null,
) {
    fun toModel(): CharacterTransactionLocation = CharacterTransactionLocation(
        securityStatus = securityStatus,
        systemZhName = systemZhName,
        systemEnName = systemEnName,
        systemName = systemName,
        placeName = placeName,
    )

    companion object {
        fun from(location: CharacterTransactionLocation): CachedCharacterTransactionLocation =
            CachedCharacterTransactionLocation(
                securityStatus = location.securityStatus,
                systemZhName = location.systemZhName,
                systemEnName = location.systemEnName,
                systemName = location.systemName,
                placeName = location.placeName,
            )
    }
}
