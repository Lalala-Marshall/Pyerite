package com.marshall.pyerite.agentsModule.navHost

import com.marshall.pyerite.agentsModule.model.AgentsConfig

internal object AgentsNavArgs {
    const val FACTION_ID = "factionId"
    const val CORPORATION_ID = "corporationId"
    const val DIVISION_ID = "divisionId"
    const val AGENT_LEVEL = "agentLevel"
}

internal sealed class AgentsRoute(val route: String) {
    data object Graph : AgentsRoute("agents")

    data object Search : AgentsRoute("agents/search")

    data object Regions : AgentsRoute("agents/regions")

    data object Systems : AgentsRoute("agents/systems")

    data object Results : AgentsRoute(
        "agents/results?${AgentsNavArgs.FACTION_ID}={${AgentsNavArgs.FACTION_ID}}" +
            "&${AgentsNavArgs.CORPORATION_ID}={${AgentsNavArgs.CORPORATION_ID}}" +
            "&${AgentsNavArgs.DIVISION_ID}={${AgentsNavArgs.DIVISION_ID}}" +
            "&${AgentsNavArgs.AGENT_LEVEL}={${AgentsNavArgs.AGENT_LEVEL}}",
    ) {
        fun create(
            factionId: Int = AgentsConfig.UNSET_ID,
            corporationId: Long = AgentsConfig.UNSET_ID.toLong(),
            divisionId: Int = AgentsConfig.UNSET_ID,
            agentLevel: Int = AgentsConfig.UNSET_ID,
        ): String =
            "agents/results?${AgentsNavArgs.FACTION_ID}=$factionId" +
                "&${AgentsNavArgs.CORPORATION_ID}=$corporationId" +
                "&${AgentsNavArgs.DIVISION_ID}=$divisionId" +
                "&${AgentsNavArgs.AGENT_LEVEL}=$agentLevel"
    }
}

internal fun Int.toOptionalAgentId(): Int? = takeUnless { it == AgentsConfig.UNSET_ID }

internal fun Long.toOptionalAgentId(): Long? = takeUnless { it == AgentsConfig.UNSET_ID.toLong() }
