package com.marshall.pyerite.databaseHierarchyModule.typeDetailPage

import com.marshall.pyerite.sdeModule.room.dogma.TypeAttributeDetail
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import com.marshall.pyerite.sdeModule.room.industry.TypeApplicableBlueprintCount
import com.marshall.pyerite.sdeModule.room.industry.BlueprintCopyDetail
import com.marshall.pyerite.sdeModule.room.industry.BlueprintInventionMaterial
import com.marshall.pyerite.sdeModule.room.industry.BlueprintInventionProduct
import com.marshall.pyerite.sdeModule.room.industry.BlueprintInventionSkill
import com.marshall.pyerite.sdeModule.room.industry.BlueprintManufacturingMaterial
import com.marshall.pyerite.sdeModule.room.industry.BlueprintManufacturingProduct
import com.marshall.pyerite.sdeModule.room.industry.BlueprintManufacturingSkill
import com.marshall.pyerite.sdeModule.room.industry.TypeBlueprintDetail
import com.marshall.pyerite.sdeModule.room.industry.TypeRefiningOutputSummary
import com.marshall.pyerite.sdeModule.room.industry.TypeRefiningSourceCount

/**
 * A type can be sold only when the SDE gives it a market group.
 * Stars and other non-market types have none. An empty order book is separate:
 * those types still have a market group, so the section stays.
 */
internal fun hasMarketSectionContent(type: TypeEntity?): Boolean = type?.marketGroupID != null

internal fun hasDisplayableDogmaRows(attributes: List<TypeAttributeDetail>): Boolean =
    attributes.any { it.displayName != null && it.value != null }

internal fun hasFittingSectionContent(attributes: List<TypeAttributeDetail>): Boolean =
    attributes
        .asSequence()
        .filter { it.displayName != null && it.value != null }
        .any { it.name != FITTING_EXCLUDED_ATTRIBUTE_NAME }

internal fun hasNavigationSectionContent(attributes: List<TypeAttributeDetail>): Boolean =
    attributes
        .asSequence()
        .filter { it.displayName != null && it.value != null }
        .any { it.name != NAVIGATION_EXCLUDED_ATTRIBUTE_NAME }

internal fun hasIndustrySectionContent(
    blueprints: List<TypeBlueprintDetail>,
    refiningOutputSummary: TypeRefiningOutputSummary?,
    applicableBlueprintCount: TypeApplicableBlueprintCount?,
    refiningSourceCount: TypeRefiningSourceCount?,
): Boolean {
    val showRefiningOutput = (refiningOutputSummary?.outputMaterialCount ?: 0) > 0
    val showApplicable = (applicableBlueprintCount?.count ?: 0) > 0
    val showRefiningSource = (refiningSourceCount?.count ?: 0) > 0
    return blueprints.isNotEmpty() || showRefiningOutput || showApplicable || showRefiningSource
}

internal fun shouldShowIndustryCount(count: Int?): Boolean = (count ?: 0) > 0

/** EVE SDE category for blueprint types. */
internal const val BLUEPRINT_CATEGORY_ID = 9

internal fun hasBlueprintResearchSectionContent(
    categoryId: Int?,
    baseTimeSeconds: Int?,
): Boolean {
    if (categoryId != BLUEPRINT_CATEGORY_ID) return false
    return (baseTimeSeconds ?: 0) > 0
}

internal fun hasMaterialResearchSectionContent(
    categoryId: Int?,
    researchMaterialTimeSeconds: Int?,
): Boolean = hasBlueprintResearchSectionContent(categoryId, researchMaterialTimeSeconds)

internal fun hasTimeResearchSectionContent(
    categoryId: Int?,
    researchTimeTimeSeconds: Int?,
): Boolean = hasBlueprintResearchSectionContent(categoryId, researchTimeTimeSeconds)

internal fun hasCopySectionContent(
    categoryId: Int?,
    copyDetail: BlueprintCopyDetail?,
): Boolean {
    if (categoryId != BLUEPRINT_CATEGORY_ID) return false
    return (copyDetail?.copyingTimeSeconds ?: 0) > 0 ||
        (copyDetail?.maxRunsPerCopy ?: 0) > 0
}

internal fun hasInventionSectionContent(
    categoryId: Int?,
    products: List<BlueprintInventionProduct>,
    materials: List<BlueprintInventionMaterial>,
    skills: List<BlueprintInventionSkill>,
    inventionTimeSeconds: Int?,
): Boolean {
    if (categoryId != BLUEPRINT_CATEGORY_ID) return false
    return products.isNotEmpty() ||
        materials.isNotEmpty() ||
        skills.isNotEmpty() ||
        (inventionTimeSeconds != null && inventionTimeSeconds > 0)
}

internal fun hasManufacturingSectionContent(
    categoryId: Int?,
    products: List<BlueprintManufacturingProduct>,
    materials: List<BlueprintManufacturingMaterial>,
    skills: List<BlueprintManufacturingSkill>,
    manufacturingTimeSeconds: Int?,
): Boolean {
    if (categoryId != BLUEPRINT_CATEGORY_ID) return false
    return products.isNotEmpty() ||
        materials.isNotEmpty() ||
        skills.isNotEmpty() ||
        (manufacturingTimeSeconds != null && manufacturingTimeSeconds > 0)
}

internal const val FITTING_EXCLUDED_ATTRIBUTE_NAME = "upgradeSlotsLeft"
internal const val NAVIGATION_EXCLUDED_ATTRIBUTE_NAME = "warpSpeedMultiplier"
