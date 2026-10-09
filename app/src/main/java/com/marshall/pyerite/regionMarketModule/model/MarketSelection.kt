package com.marshall.pyerite.regionMarketModule.model

/**
 * App-wide market place. Persisted as `region:{id}`, `system:{id}`, or `structure:{id}`.
 * Structure ids stay [Long] so they are not packed into a 32-bit virtual region id.
 */
internal sealed interface MarketSelection {
    val persistKey: String

    data class Region(val regionId: Int) : MarketSelection {
        override val persistKey: String = "$KIND_REGION$regionId"
    }

    data class System(val systemId: Int) : MarketSelection {
        override val persistKey: String = "$KIND_SYSTEM$systemId"
    }

    data class Structure(val structureId: Long) : MarketSelection {
        override val persistKey: String = "$KIND_STRUCTURE$structureId"
    }

    companion object {
        private const val KIND_REGION = "region:"
        private const val KIND_SYSTEM = "system:"
        private const val KIND_STRUCTURE = "structure:"

        fun default(): MarketSelection = Region(MarketConfig.DEFAULT_REGION_ID)

        fun parse(raw: String?): MarketSelection {
            if (raw.isNullOrBlank()) return default()
            return when {
                raw.startsWith(KIND_REGION) ->
                    raw.removePrefix(KIND_REGION).toIntOrNull()?.let(::Region) ?: default()
                raw.startsWith(KIND_SYSTEM) ->
                    raw.removePrefix(KIND_SYSTEM).toIntOrNull()?.let(::System) ?: default()
                raw.startsWith(KIND_STRUCTURE) ->
                    raw.removePrefix(KIND_STRUCTURE).toLongOrNull()?.let(::Structure) ?: default()
                else -> default()
            }
        }
    }
}
