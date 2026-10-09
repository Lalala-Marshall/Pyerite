package com.marshall.pyerite.databaseHierarchyModule.typeDetailPage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.marshall.pyerite.R
import com.marshall.pyerite.databaseHierarchyModule.navHost.rememberDatabaseRootBackStackEntry
import com.marshall.pyerite.sdeModule.room.type.TypeEntity
import com.marshall.pyerite.databaseHierarchyModule.viewModel.DatabaseViewModel
import com.marshall.pyerite.ui.golbalComponents.PageTitle
import com.marshall.pyerite.ui.golbalComponents.PyeritePageScaffold
import com.marshall.pyerite.ui.golbalComponents.rememberNavigateUpAction
import com.marshall.pyerite.ui.golbalComponents.rememberScrollTitleCollapsed
import org.koin.androidx.compose.koinViewModel

internal enum class TypeDetailSlot {
    Title,
    Summary,
    Market,
    BaseInfo,
    Variants,
    Fitting,
    Shield,
    Armor,
    Structure,
    Capacitor,
    Targeting,
    Navigation,
    Drones,
    HangarsBays,
    EwarResistances,
    Misc,
    Bonuses,
    Skills,
    SkillLevelDetail,
    SkillLevelApplies,
    Industry,
    Manufacturing,
    MaterialResearch,
    TimeResearch,
    Copy,
    Invention,
}

@Composable
fun TypeDetailPage(
    typeId: Int,
    navController: NavController,
    backStackEntry: NavBackStackEntry,
) {
    val databaseBackStackEntry = rememberDatabaseRootBackStackEntry(navController, backStackEntry)
    val viewModel: DatabaseViewModel = koinViewModel(viewModelStoreOwner = databaseBackStackEntry)

    val scrollState = rememberTypeDetailScrollState(typeId, viewModel)

    val type by remember(typeId) { viewModel.typeDetail(typeId) }
        .collectAsState(initial = null)

    val visibleSlots = rememberVisibleTypeDetailSlots(typeId, viewModel)
    val pageTitle = stringResource(R.string.type_info)
    val onBack = navController.rememberNavigateUpAction()
    val showCollapsedTitle = rememberScrollTitleCollapsed(scrollState)

    type?.let { entity ->
        PyeritePageScaffold(
            title = pageTitle,
            showCollapsedTitle = showCollapsedTitle,
            onBack = onBack,
        ) { topBarPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(topBarPadding)
                    .verticalScroll(scrollState),
            ) {
                visibleSlots.forEach { slot ->
                    TypeDetailSlotContent(
                        slot = slot,
                        typeId = typeId,
                        entity = entity,
                        navController = navController,
                    )
                    TypeDetailSlotTrailingSpacer(
                        slot = slot,
                        isLastSlot = slot == visibleSlots.last(),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberTypeDetailScrollState(
    typeId: Int,
    viewModel: DatabaseViewModel,
): ScrollState {
    val scrollState = remember(typeId) {
        ScrollState(viewModel.typeDetailScrollOffset(typeId))
    }

    DisposableEffect(typeId, scrollState) {
        onDispose {
            viewModel.saveTypeDetailScrollOffset(typeId, scrollState.value)
        }
    }

    LaunchedEffect(typeId) {
        val target = viewModel.typeDetailScrollOffset(typeId)
        if (target <= 0) return@LaunchedEffect
        snapshotFlow { scrollState.maxValue }.collect { max ->
            if (max <= 0) return@collect
            val desired = target.coerceAtMost(max)
            if (scrollState.value < desired) {
                scrollState.scrollTo(desired)
            }
        }
    }

    return scrollState
}

@Composable
private fun rememberVisibleTypeDetailSlots(
    typeId: Int,
    viewModel: DatabaseViewModel,
): List<TypeDetailSlot> {
    val attributes by remember(typeId) { viewModel.typeAttributes(typeId) }
        .collectAsState(initial = emptyList())
    val applicableBlueprintCount by remember(typeId) { viewModel.applicableBlueprintCount(typeId) }
        .collectAsState(initial = null)
    val refiningSourceCount by remember(typeId) { viewModel.refiningSourceCount(typeId) }
        .collectAsState(initial = null)
    val blueprints by remember(typeId) { viewModel.blueprintsForProduct(typeId) }
        .collectAsState(initial = emptyList())
    val refiningOutputSummary by remember(typeId) { viewModel.refiningOutputSummary(typeId) }
        .collectAsState(initial = null)
    val skillRequirements by remember(typeId) { viewModel.skillRequirements(typeId) }
        .collectAsState(initial = emptyList())
    val variantCount by remember(typeId) { viewModel.variantCount(typeId) }
        .collectAsState(initial = 0)
    val skillMiscRows by remember(typeId) { viewModel.skillMiscRows(typeId) }
        .collectAsState(initial = emptyList())
    val skillLevelSpRows by remember(typeId) { viewModel.skillLevelSpRows(typeId) }
        .collectAsState(initial = emptyList())
    val skillUnlockLevels by remember(typeId) { viewModel.skillUnlockLevels(typeId) }
        .collectAsState(initial = emptyList())
    val type by remember(typeId) { viewModel.typeDetail(typeId) }
        .collectAsState(initial = null)
    val manufacturingProducts by remember(typeId) { viewModel.blueprintManufacturingProducts(typeId) }
        .collectAsState(initial = emptyList())
    val manufacturingMaterials by remember(typeId) { viewModel.blueprintManufacturingMaterials(typeId) }
        .collectAsState(initial = emptyList())
    val manufacturingSkills by remember(typeId) { viewModel.blueprintManufacturingSkills(typeId) }
        .collectAsState(initial = emptyList())
    val manufacturingTime by remember(typeId) { viewModel.blueprintManufacturingTime(typeId) }
        .collectAsState(initial = null)
    val researchMaterialTime by remember(typeId) { viewModel.blueprintResearchMaterialTime(typeId) }
        .collectAsState(initial = null)
    val researchTimeTime by remember(typeId) { viewModel.blueprintResearchTimeTime(typeId) }
        .collectAsState(initial = null)
    val copyDetail by remember(typeId) { viewModel.blueprintCopyDetail(typeId) }
        .collectAsState(initial = null)
    val inventionProducts by remember(typeId) { viewModel.blueprintInventionProducts(typeId) }
        .collectAsState(initial = emptyList())
    val inventionMaterials by remember(typeId) { viewModel.blueprintInventionMaterials(typeId) }
        .collectAsState(initial = emptyList())
    val inventionSkills by remember(typeId) { viewModel.blueprintInventionSkills(typeId) }
        .collectAsState(initial = emptyList())
    val inventionTime by remember(typeId) { viewModel.blueprintInventionTime(typeId) }
        .collectAsState(initial = null)

    return remember(
        attributes,
        applicableBlueprintCount,
        refiningSourceCount,
        blueprints,
        refiningOutputSummary,
        skillRequirements,
        variantCount,
        skillMiscRows,
        skillLevelSpRows,
        skillUnlockLevels,
        type,
        manufacturingProducts,
        manufacturingMaterials,
        manufacturingSkills,
        manufacturingTime,
        researchMaterialTime,
        researchTimeTime,
        copyDetail,
        inventionProducts,
        inventionMaterials,
        inventionSkills,
        inventionTime,
    ) {
        buildList {
            add(TypeDetailSlot.Title)
            add(TypeDetailSlot.Summary)
            if (hasMarketSectionContent(type)) {
                add(TypeDetailSlot.Market)
            }
            add(TypeDetailSlot.BaseInfo)
            if (variantCount > 1) {
                add(TypeDetailSlot.Variants)
            }
            if (hasFittingSectionContent(attributes.inDogmaCategory(DogmaCategory.FITTING))) {
                add(TypeDetailSlot.Fitting)
            }
            if (hasDefenseSectionContent(attributes.inDogmaCategory(DogmaCategory.SHIELD))) {
                add(TypeDetailSlot.Shield)
            }
            if (hasDefenseSectionContent(attributes.inDogmaCategory(DogmaCategory.ARMOR))) {
                add(TypeDetailSlot.Armor)
            }
            if (hasDefenseSectionContent(attributes.inDogmaCategory(DogmaCategory.STRUCTURE))) {
                add(TypeDetailSlot.Structure)
            }
            if (hasDisplayableDogmaRows(attributes.inDogmaCategory(DogmaCategory.CAPACITOR))) {
                add(TypeDetailSlot.Capacitor)
            }
            if (hasDisplayableDogmaRows(attributes.inDogmaCategory(DogmaCategory.TARGETING))) {
                add(TypeDetailSlot.Targeting)
            }
            if (hasNavigationSectionContent(attributes.inDogmaCategory(DogmaCategory.NAVIGATION))) {
                add(TypeDetailSlot.Navigation)
            }
            if (hasDisplayableDogmaRows(attributes.inDogmaCategory(DogmaCategory.DRONES))) {
                add(TypeDetailSlot.Drones)
            }
            if (hasDisplayableDogmaRows(attributes.inDogmaCategory(DogmaCategory.HANGARS_BAYS))) {
                add(TypeDetailSlot.HangarsBays)
            }
            if (hasDisplayableDogmaRows(attributes.inDogmaCategory(DogmaCategory.EWAR_RESISTANCES))) {
                add(TypeDetailSlot.EwarResistances)
            }
            if (hasMiscSectionContent(attributes, skillMiscRows = skillMiscRows)) {
                add(TypeDetailSlot.Misc)
            }
            if (hasDisplayableDogmaRows(attributes.inDogmaCategory(DogmaCategory.BONUSES))) {
                add(TypeDetailSlot.Bonuses)
            }
            if (hasSkillsSectionContent(
                    skillRequirements,
                    attributes.inDogmaCategory(DogmaCategory.SKILLS),
                )
            ) {
                add(TypeDetailSlot.Skills)
            }
            if (hasSkillLevelDetailContent(skillLevelSpRows)) {
                add(TypeDetailSlot.SkillLevelDetail)
            }
            if (hasSkillLevelAppliesContent(skillUnlockLevels)) {
                add(TypeDetailSlot.SkillLevelApplies)
            }
            if (hasIndustrySectionContent(
                    blueprints,
                    refiningOutputSummary,
                    applicableBlueprintCount,
                    refiningSourceCount,
                )
            ) {
                add(TypeDetailSlot.Industry)
            }
            if (hasManufacturingSectionContent(
                    categoryId = type?.categoryID,
                    products = manufacturingProducts,
                    materials = manufacturingMaterials,
                    skills = manufacturingSkills,
                    manufacturingTimeSeconds = manufacturingTime,
                )
            ) {
                add(TypeDetailSlot.Manufacturing)
            }
            if (hasMaterialResearchSectionContent(
                    categoryId = type?.categoryID,
                    researchMaterialTimeSeconds = researchMaterialTime,
                )
            ) {
                add(TypeDetailSlot.MaterialResearch)
            }
            if (hasTimeResearchSectionContent(
                    categoryId = type?.categoryID,
                    researchTimeTimeSeconds = researchTimeTime,
                )
            ) {
                add(TypeDetailSlot.TimeResearch)
            }
            if (hasCopySectionContent(
                    categoryId = type?.categoryID,
                    copyDetail = copyDetail,
                )
            ) {
                add(TypeDetailSlot.Copy)
            }
            if (hasInventionSectionContent(
                    categoryId = type?.categoryID,
                    products = inventionProducts,
                    materials = inventionMaterials,
                    skills = inventionSkills,
                    inventionTimeSeconds = inventionTime,
                )
            ) {
                add(TypeDetailSlot.Invention)
            }
        }
    }
}

@Composable
private fun TypeDetailSlotContent(
    slot: TypeDetailSlot,
    typeId: Int,
    entity: TypeEntity,
    navController: NavController,
) {
    when (slot) {
        TypeDetailSlot.Title -> {
            PageTitle(text = stringResource(R.string.type_info))
        }

        TypeDetailSlot.Summary -> TypeSummarySectionItem(typeId = typeId, entity = entity)
        TypeDetailSlot.Market -> TypeDetailMarketSlot(
            onOpenRegionMarket = {
                navController.navigate(
                    com.marshall.pyerite.regionMarketModule.navHost.RegionMarketRoute.Detail.create(typeId),
                )
            },
        )
        TypeDetailSlot.BaseInfo -> TypeDetailBaseInfoSectionItem(entity = entity)
        TypeDetailSlot.Variants -> TypeDetailVariantsSectionItem(typeId = typeId, navController = navController)
        TypeDetailSlot.Fitting -> TypeDetailFittingSectionItem(typeId)
        TypeDetailSlot.Shield -> TypeDetailShieldSectionItem(typeId)
        TypeDetailSlot.Armor -> TypeDetailArmorSectionItem(typeId)
        TypeDetailSlot.Structure -> TypeDetailStructureSectionItem(typeId)
        TypeDetailSlot.Capacitor -> TypeDetailCapacitorSectionItem(typeId)
        TypeDetailSlot.Targeting -> TypeDetailTargetingSectionItem(typeId)
        TypeDetailSlot.Navigation -> TypeDetailNavigationSectionItem(typeId)
        TypeDetailSlot.Drones -> TypeDetailDronesSectionItem(typeId)
        TypeDetailSlot.HangarsBays -> TypeDetailHangarsBaysSectionItem(typeId)
        TypeDetailSlot.EwarResistances -> TypeDetailEwarResistancesSectionItem(typeId)
        TypeDetailSlot.Misc -> TypeDetailMiscSectionItem(typeId)
        TypeDetailSlot.Bonuses -> TypeDetailBonusesSectionItem(typeId)
        TypeDetailSlot.Skills -> TypeDetailSkillsSectionItem(typeId, navController)
        TypeDetailSlot.SkillLevelDetail -> TypeDetailSkillLevelDetailSectionItem(typeId)
        TypeDetailSlot.SkillLevelApplies -> TypeDetailSkillLevelAppliesSectionItem(typeId, navController)
        TypeDetailSlot.Industry -> TypeDetailIndustrySectionItem(
            typeId = typeId,
            navController = navController,
        )
        TypeDetailSlot.Manufacturing -> TypeDetailManufacturingSectionItem(
            typeId = typeId,
            navController = navController,
        )
        TypeDetailSlot.MaterialResearch -> TypeDetailMaterialResearchSectionItem(typeId = typeId)
        TypeDetailSlot.TimeResearch -> TypeDetailTimeResearchSectionItem(typeId = typeId)
        TypeDetailSlot.Copy -> TypeDetailCopySectionItem(typeId = typeId)
        TypeDetailSlot.Invention -> TypeDetailInventionSectionItem(
            typeId = typeId,
            navController = navController,
        )
    }
}

@Composable
private fun TypeDetailSlotTrailingSpacer(
    slot: TypeDetailSlot,
    isLastSlot: Boolean,
) {
    when (slot) {
        TypeDetailSlot.Title -> Unit
        else -> {
            val gap = dimensionResource(
                if (isLastSlot) R.dimen.type_detail_bottom_padding else R.dimen.type_detail_section_gap,
            )
            Spacer(Modifier.height(gap))
        }
    }
}
