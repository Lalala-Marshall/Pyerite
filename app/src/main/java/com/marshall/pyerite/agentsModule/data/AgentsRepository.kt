package com.marshall.pyerite.agentsModule.data

import com.marshall.pyerite.sdeModule.room.RoomProvider
import com.marshall.pyerite.sdeModule.room.npc.AgentQueryRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AgentsRepository(
    private val roomProvider: RoomProvider,
) {
    suspend fun loadAgents(): List<AgentQueryRow> = withContext(Dispatchers.IO) {
        roomProvider.getDatabase().agentDao().getAllAgents()
    }
}
