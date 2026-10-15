package com.marshall.pyerite.sdeModule.room.loyalty

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loyalty_offers",
    primaryKeys = ["corporation_id", "offer_id"],
    indices = [
        // WITHOUT ROWID: SQLite appends the remaining PK column; Room reads it via index_xinfo.
        Index(
            value = ["corporation_id", "offer_id"],
            name = "idx_loyalty_offers_corporation_id",
        ),
    ],
)
data class LoyaltyOfferEntity(
    @ColumnInfo(name = "corporation_id") val corporationId: Long,
    @ColumnInfo(name = "offer_id") val offerId: Int,
)

@Entity(
    tableName = "loyalty_offer_outputs",
    indices = [
        Index(value = ["lp_cost"], name = "idx_loyalty_offer_outputs_lp_cost"),
        Index(value = ["type_id"], name = "idx_loyalty_offer_outputs_type_id"),
    ],
)
data class LoyaltyOfferOutputEntity(
    @PrimaryKey
    @ColumnInfo(name = "offer_id")
    val offerId: Int?,
    @ColumnInfo(name = "type_id") val typeId: Int,
    /** Defaults match the prepackaged SDE `CREATE TABLE` (Room schema validation). */
    @ColumnInfo(name = "quantity", defaultValue = "1") val quantity: Int,
    @ColumnInfo(name = "isk_cost", defaultValue = "0") val iskCost: Long,
    @ColumnInfo(name = "lp_cost", defaultValue = "0") val lpCost: Long,
    @ColumnInfo(name = "ak_cost", defaultValue = "0") val akCost: Long,
)

@Entity(
    tableName = "loyalty_offer_requirements",
    primaryKeys = ["offer_id", "required_type_id"],
    indices = [
        // WITHOUT ROWID: SQLite appends the other PK column; Room reads it via index_xinfo.
        Index(
            value = ["offer_id", "required_type_id"],
            name = "idx_loyalty_offer_requirements_offer_id",
        ),
        Index(
            value = ["required_type_id", "offer_id"],
            name = "idx_loyalty_offer_requirements_type_id",
        ),
    ],
)
data class LoyaltyOfferRequirementEntity(
    @ColumnInfo(name = "offer_id") val offerId: Int,
    @ColumnInfo(name = "required_type_id") val requiredTypeId: Int,
    @ColumnInfo(name = "required_quantity") val requiredQuantity: Int,
)
