package com.marshall.pyerite.sdeModule.room.type

import androidx.room.Dao
import androidx.room.Query

@Dao
interface TypeDao {
    @Query("SELECT * FROM types WHERE groupID = :groupId ORDER BY type_id")
    suspend fun getTypesByGroup(groupId: Int): List<TypeEntity>

    @Query(
        """
        SELECT * FROM types
        WHERE zh_name LIKE :pattern COLLATE NOCASE
           OR en_name LIKE :pattern COLLATE NOCASE
           OR name LIKE :pattern COLLATE NOCASE
        ORDER BY groupID, type_id
        LIMIT :limit
        """,
    )
    suspend fun searchTypes(pattern: String, limit: Int): List<TypeEntity>

    @Query("SELECT * FROM types WHERE type_id = :typeId")
    suspend fun getTypeById(typeId: Int): TypeEntity?

    @Query("SELECT * FROM types WHERE type_id IN (:typeIds)")
    suspend fun getTypesByIds(typeIds: List<Int>): List<TypeEntity>

    @Query(
        """
        SELECT type_id FROM types
        WHERE zh_name = :name COLLATE NOCASE
        ORDER BY type_id
        LIMIT 1
        """,
    )
    suspend fun findTypeIdByZhName(name: String): Int?

    @Query(
        """
        SELECT type_id FROM types
        WHERE en_name = :name COLLATE NOCASE
        ORDER BY type_id
        LIMIT 1
        """,
    )
    suspend fun findTypeIdByEnName(name: String): Int?

    @Query(
        """
        SELECT type_id FROM types
        WHERE name = :name COLLATE NOCASE
        ORDER BY type_id
        LIMIT 1
        """,
    )
    suspend fun findTypeIdByName(name: String): Int?

    @Query(
        """
        SELECT icon_filename
        FROM types
        WHERE type_id = :typeId
        LIMIT 1
        """,
    )
    suspend fun getTypeIconFilename(typeId: Int): String?

    @Query(
        """
        SELECT type_id AS id, name, zh_name AS zhName, en_name AS enName
        FROM types
        WHERE type_id = :typeId
        LIMIT 1
        """,
    )
    suspend fun getTypeDisplayName(typeId: Int): TypeDisplayNameRow?

    @Query(
        """
        SELECT
            da.attribute_id AS attributeId,
            da.name AS attributeName,
            da.display_name AS attributeDisplayName,
            da.icon_filename AS attributeIconFilename,
            CAST(ta.value AS INTEGER) AS groupId
        FROM typeAttributes ta
        JOIN dogmaAttributes da ON ta.attribute_id = da.attribute_id
        WHERE ta.type_id = :typeId
          AND ta.value IS NOT NULL
          AND ta.value > 0
          AND (da.name GLOB 'chargeGroup*' OR da.name GLOB 'launcherGroup*')
        ORDER BY da.attribute_id
        """,
    )
    suspend fun getCompatibleGroupRefs(typeId: Int): List<TypeCompatibleGroupRef>

    @Query(
        """
        SELECT COUNT(*) FROM types
        WHERE (type_id = :rootTypeId OR variationParentTypeID = :rootTypeId)
          AND published = 1
        """,
    )
    suspend fun getVariantCount(rootTypeId: Int): Int

    @Query(
        """
        SELECT * FROM types
        WHERE (type_id = :rootTypeId OR variationParentTypeID = :rootTypeId)
          AND published = 1
        ORDER BY metaGroupID, type_id
        """,
    )
    suspend fun getVariantsByRoot(rootTypeId: Int): List<TypeEntity>

    @Query(
        """
        SELECT * FROM types
        WHERE marketGroupID = :marketGroupId
        ORDER BY metaGroupID, type_id
        """,
    )
    suspend fun getTypesByMarketGroup(marketGroupId: Int): List<TypeEntity>

    @Query(
        """
        SELECT * FROM types
        WHERE marketGroupID IN (:marketGroupIds)
          AND (
            name LIKE :pattern COLLATE NOCASE
            OR zh_name LIKE :pattern COLLATE NOCASE
            OR en_name LIKE :pattern COLLATE NOCASE
          )
        ORDER BY metaGroupID, type_id
        LIMIT :limit
        """,
    )
    suspend fun searchTypesInMarketGroups(
        marketGroupIds: List<Int>,
        pattern: String,
        limit: Int,
    ): List<TypeEntity>
}
