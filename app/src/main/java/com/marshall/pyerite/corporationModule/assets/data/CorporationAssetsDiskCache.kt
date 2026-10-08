package com.marshall.pyerite.corporationModule.assets.data

import android.content.Context
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetContainerView
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetFolderKey
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetFolderView
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationKind
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetLocationView
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetRegionSection
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsConfig
import com.marshall.pyerite.corporationModule.assets.model.CorporationAssetsSnapshot
import com.marshall.pyerite.infra.network.PyeriteJson
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Assembled corporation-asset tree on disk.
 * A newer app install drops the file so cached string-resource ids cannot outlive a build.
 */
internal class CorporationAssetsDiskCache(
    context: Context,
) {
    private val directory = File(context.applicationContext.filesDir, CorporationAssetsConfig.DISK_CACHE_DIR)
    @Suppress("DEPRECATION")
    private val installedAtEpochMs = context.applicationContext.packageManager
        .getPackageInfo(context.applicationContext.packageName, 0)
        .lastUpdateTime

    fun readIfFresh(
        characterId: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): CorporationAssetsSnapshot? {
        val file = fileFor(characterId)
        if (!file.isFile) return null
        val cached = runCatching {
            PyeriteJson.decodeFromString<CachedCorporationAssetsFile>(file.readText())
        }.getOrElse {
            file.delete()
            return null
        }
        val ageMs = nowEpochMs - cached.savedAtEpochMs
        val fresh = cached.installedAtEpochMs == installedAtEpochMs &&
            ageMs >= 0L &&
            ageMs < CorporationAssetsConfig.DISK_CACHE_TTL_MS
        if (!fresh) {
            file.delete()
            return null
        }
        return cached.toSnapshot()
    }

    fun write(characterId: Long, snapshot: CorporationAssetsSnapshot) {
        directory.mkdirs()
        val target = fileFor(characterId)
        val payload = CachedCorporationAssetsFile(
            savedAtEpochMs = System.currentTimeMillis(),
            installedAtEpochMs = installedAtEpochMs,
            regions = snapshot.regions,
            locations = snapshot.locations.values.toList(),
            folders = snapshot.folders.map { (key, view) ->
                CachedCorporationAssetFolder(
                    kind = key.kind,
                    locationId = key.locationId,
                    routeFlag = key.routeFlag,
                    view = view,
                )
            },
            containers = snapshot.containers.values.toList(),
        )
        val temporary = File(directory, target.name + CorporationAssetsConfig.DISK_CACHE_TMP_SUFFIX)
        temporary.writeText(PyeriteJson.encodeToString(payload))
        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) {
            target.writeText(temporary.readText())
            temporary.delete()
        }
    }

    private fun fileFor(characterId: Long): File = File(
        directory,
        CorporationAssetsConfig.DISK_CACHE_FILE_PREFIX +
            characterId +
            CorporationAssetsConfig.DISK_CACHE_FILE_SUFFIX,
    )
}

@Serializable
private data class CachedCorporationAssetsFile(
    val savedAtEpochMs: Long,
    val installedAtEpochMs: Long,
    val regions: List<CorporationAssetRegionSection> = emptyList(),
    val locations: List<CorporationAssetLocationView> = emptyList(),
    val folders: List<CachedCorporationAssetFolder> = emptyList(),
    val containers: List<CorporationAssetContainerView> = emptyList(),
) {
    fun toSnapshot(): CorporationAssetsSnapshot = CorporationAssetsSnapshot(
        regions = regions,
        locations = locations.associateBy { it.key },
        folders = folders.associate { entry ->
            CorporationAssetFolderKey(
                kind = entry.kind,
                locationId = entry.locationId,
                routeFlag = entry.routeFlag,
            ) to entry.view
        },
        containers = containers.associateBy { it.itemId },
    )
}

@Serializable
private data class CachedCorporationAssetFolder(
    val kind: CorporationAssetLocationKind,
    val locationId: Long,
    val routeFlag: String,
    val view: CorporationAssetFolderView,
)
