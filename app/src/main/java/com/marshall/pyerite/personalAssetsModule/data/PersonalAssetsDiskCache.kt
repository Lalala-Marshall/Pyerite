package com.marshall.pyerite.personalAssetsModule.data

import android.content.Context
import com.marshall.pyerite.infra.network.PyeriteJson
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetContainerView
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetLocationView
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetRegionSection
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsConfig
import com.marshall.pyerite.personalAssetsModule.model.PersonalAssetsSnapshot
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Assembled personal-asset tree on disk, one file per character.
 * A newer app install drops the file so cached string-resource ids cannot outlive a build.
 */
internal class PersonalAssetsDiskCache(
    context: Context,
) {
    private val directory = File(context.applicationContext.filesDir, PersonalAssetsConfig.DISK_CACHE_DIR)
    @Suppress("DEPRECATION")
    private val installedAtEpochMs = context.applicationContext.packageManager
        .getPackageInfo(context.applicationContext.packageName, 0)
        .lastUpdateTime

    fun readIfFresh(
        characterId: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): PersonalAssetsSnapshot? {
        val file = fileFor(characterId)
        if (!file.isFile) return null
        val cached = runCatching {
            PyeriteJson.decodeFromString<CachedPersonalAssetsFile>(file.readText())
        }.getOrElse {
            file.delete()
            return null
        }
        val ageMs = nowEpochMs - cached.savedAtEpochMs
        val fresh = cached.installedAtEpochMs == installedAtEpochMs &&
            ageMs >= 0L &&
            ageMs < PersonalAssetsConfig.DISK_CACHE_TTL_MS
        if (!fresh) {
            file.delete()
            return null
        }
        return cached.toSnapshot()
    }

    fun write(characterId: Long, snapshot: PersonalAssetsSnapshot) {
        directory.mkdirs()
        val target = fileFor(characterId)
        val payload = CachedPersonalAssetsFile(
            savedAtEpochMs = System.currentTimeMillis(),
            installedAtEpochMs = installedAtEpochMs,
            regions = snapshot.regions,
            locations = snapshot.locations.values.toList(),
            containers = snapshot.containers.values.toList(),
        )
        val temporary = File(directory, target.name + PersonalAssetsConfig.DISK_CACHE_TMP_SUFFIX)
        temporary.writeText(PyeriteJson.encodeToString(payload))
        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) {
            target.writeText(temporary.readText())
            temporary.delete()
        }
    }

    private fun fileFor(characterId: Long): File = File(
        directory,
        PersonalAssetsConfig.DISK_CACHE_FILE_PREFIX +
            characterId +
            PersonalAssetsConfig.DISK_CACHE_FILE_SUFFIX,
    )
}

@Serializable
private data class CachedPersonalAssetsFile(
    val savedAtEpochMs: Long,
    val installedAtEpochMs: Long,
    val regions: List<PersonalAssetRegionSection> = emptyList(),
    val locations: List<PersonalAssetLocationView> = emptyList(),
    val containers: List<PersonalAssetContainerView> = emptyList(),
) {
    fun toSnapshot(): PersonalAssetsSnapshot = PersonalAssetsSnapshot(
        regions = regions,
        locations = locations.associateBy { it.key },
        containers = containers.associateBy { it.itemId },
    )
}
