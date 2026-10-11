package com.marshall.pyerite.characterWalletJournalModule.data

import android.content.Context
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalConfig
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDateFormatter
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDescription
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalDescriptionKind
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalEntry
import com.marshall.pyerite.characterWalletJournalModule.model.CharacterWalletJournalFilterType
import com.marshall.pyerite.infra.network.PyeriteJson
import com.marshall.pyerite.walletJournal.model.WalletJournalCategory
import com.marshall.pyerite.walletJournal.model.WalletJournalRef
import kotlinx.serialization.Serializable
import java.io.File
import java.util.Calendar

/**
 * Full personal wallet-journal history for one character.
 * A refresh appends rows whose id is newer than the saved maximum; it does not
 * replace this file with the latest ESI page.
 */
internal class CharacterWalletJournalDiskCache(
    context: Context,
) {
    private val directory = File(
        context.applicationContext.filesDir,
        CharacterWalletJournalConfig.DISK_CACHE_DIR,
    )

    fun read(characterId: Long): CachedCharacterWalletJournal? {
        val file = fileFor(characterId)
        if (!file.isFile) return null
        return runCatching {
            PyeriteJson.decodeFromString<CachedCharacterWalletJournalFile>(file.readText()).toCached()
        }.getOrElse {
            file.delete()
            null
        }
    }

    fun write(
        characterId: Long,
        savedAtEpochMs: Long,
        entries: List<CharacterWalletJournalEntry>,
    ) {
        directory.mkdirs()
        val target = fileFor(characterId)
        val payload = CachedCharacterWalletJournalFile.from(
            savedAtEpochMs = savedAtEpochMs,
            entries = entries,
        )
        val temporary = File(directory, target.name + CharacterWalletJournalConfig.DISK_CACHE_TMP_SUFFIX)
        temporary.writeText(PyeriteJson.encodeToString(payload))
        if (target.exists() && !target.delete()) return
        if (!temporary.renameTo(target)) {
            target.writeText(temporary.readText())
            temporary.delete()
        }
    }

    /**
     * When every character file together holds at least
     * [CharacterWalletJournalConfig.CLEANUP_MIN_ROW_COUNT] rows, drop rows older than
     * [CharacterWalletJournalConfig.CLEANUP_RETENTION_YEARS].
     */
    fun removeExpiredJournal(nowEpochMs: Long = System.currentTimeMillis()) {
        if (!directory.isDirectory) return
        val snapshots = directory.listFiles().orEmpty().mapNotNull { file ->
            val characterId = characterIdFrom(file) ?: return@mapNotNull null
            val cached = read(characterId) ?: return@mapNotNull null
            characterId to cached
        }
        val totalRows = snapshots.sumOf { (_, cached) -> cached.entries.size }
        if (totalRows < CharacterWalletJournalConfig.CLEANUP_MIN_ROW_COUNT) return
        val cutoffEpochMs = retentionCutoffEpochMs(nowEpochMs)
        snapshots.forEach { (characterId, cached) ->
            val kept = cached.entries.filter { it.dateEpochMs >= cutoffEpochMs }
            if (kept.size == cached.entries.size) return@forEach
            write(
                characterId = characterId,
                savedAtEpochMs = cached.savedAtEpochMs,
                entries = kept,
            )
        }
    }

    private fun fileFor(characterId: Long): File = File(
        directory,
        CharacterWalletJournalConfig.DISK_CACHE_FILE_PREFIX +
            characterId +
            CharacterWalletJournalConfig.DISK_CACHE_FILE_SUFFIX,
    )

    private fun characterIdFrom(file: File): Long? {
        val name = file.name
        if (!name.startsWith(CharacterWalletJournalConfig.DISK_CACHE_FILE_PREFIX) ||
            !name.endsWith(CharacterWalletJournalConfig.DISK_CACHE_FILE_SUFFIX)
        ) {
            return null
        }
        return name
            .removePrefix(CharacterWalletJournalConfig.DISK_CACHE_FILE_PREFIX)
            .removeSuffix(CharacterWalletJournalConfig.DISK_CACHE_FILE_SUFFIX)
            .toLongOrNull()
    }

    private fun retentionCutoffEpochMs(nowEpochMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowEpochMs
        calendar.add(Calendar.YEAR, -CharacterWalletJournalConfig.CLEANUP_RETENTION_YEARS)
        return calendar.timeInMillis
    }
}

internal data class CachedCharacterWalletJournal(
    val savedAtEpochMs: Long,
    val entries: List<CharacterWalletJournalEntry>,
) {
    fun isFresh(nowEpochMs: Long): Boolean {
        if (entries.isEmpty()) return false
        val ageMs = nowEpochMs - savedAtEpochMs
        return ageMs >= 0L && ageMs < CharacterWalletJournalConfig.DISK_CACHE_TTL_MS
    }
}

@Serializable
private data class CachedCharacterWalletJournalFile(
    val savedAtEpochMs: Long,
    val entries: List<CachedCharacterWalletJournalEntry> = emptyList(),
) {
    fun toCached(): CachedCharacterWalletJournal = CachedCharacterWalletJournal(
        savedAtEpochMs = savedAtEpochMs,
        entries = entries.map { it.toModel() },
    )

    companion object {
        fun from(
            savedAtEpochMs: Long,
            entries: List<CharacterWalletJournalEntry>,
        ): CachedCharacterWalletJournalFile = CachedCharacterWalletJournalFile(
            savedAtEpochMs = savedAtEpochMs,
            entries = entries.map(CachedCharacterWalletJournalEntry::from),
        )
    }
}

@Serializable
private data class CachedCharacterWalletJournalEntry(
    val id: Long,
    val dateEpochMs: Long,
    val amount: Double,
    val balance: Double? = null,
    val refType: String,
    val contextId: Long? = null,
    val fallbackText: String,
    val firstPartyName: String? = null,
    val secondPartyName: String? = null,
) {
    fun toModel(): CharacterWalletJournalEntry {
        val category = WalletJournalRef.categoryFor(refType)
        val kind = if (category == WalletJournalCategory.INDUSTRY) {
            CharacterWalletJournalDescriptionKind.INDUSTRY_PROJECT
        } else {
            CharacterWalletJournalDescriptionKind.FALLBACK
        }
        return CharacterWalletJournalEntry(
            id = id,
            dateEpochMs = dateEpochMs,
            dayKey = CharacterWalletJournalDateFormatter.dayKey(dateEpochMs),
            amount = amount,
            balance = balance,
            refType = refType,
            titleRes = CharacterWalletJournalFilterType.titleResFor(refType),
            description = CharacterWalletJournalDescription(
                kind = kind,
                firstPartyName = firstPartyName,
                secondPartyName = secondPartyName,
                contextId = contextId,
                fallbackText = fallbackText,
            ),
        )
    }

    companion object {
        fun from(entry: CharacterWalletJournalEntry): CachedCharacterWalletJournalEntry =
            CachedCharacterWalletJournalEntry(
                id = entry.id,
                dateEpochMs = entry.dateEpochMs,
                amount = entry.amount,
                balance = entry.balance,
                refType = entry.refType,
                contextId = entry.description.contextId,
                fallbackText = entry.description.fallbackText,
                firstPartyName = entry.description.firstPartyName,
                secondPartyName = entry.description.secondPartyName,
            )
    }
}
