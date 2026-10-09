package com.marshall.pyerite.sdeModule.room.market

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MarketGroupDao {
    @Query(
        """
        SELECT * FROM marketGroups
        WHERE show = 1
        ORDER BY group_id
        """,
    )
    suspend fun getVisibleGroups(): List<MarketGroupEntity>

    @Query(
        """
        SELECT * FROM marketGroups
        WHERE group_id = :groupId
        LIMIT 1
        """,
    )
    suspend fun getGroup(groupId: Int): MarketGroupEntity?
}
