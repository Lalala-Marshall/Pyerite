package com.marshall.pyerite.appNavHost

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.marshall.pyerite.characterCalendarModule.navHost.characterCalendarNavGraph
import com.marshall.pyerite.characterClonesModule.navHost.characterClonesNavGraph
import com.marshall.pyerite.characterMailModule.navHost.characterMailNavGraph
import com.marshall.pyerite.characterMasteryModule.navHost.characterMasteryNavGraph
import com.marshall.pyerite.characterSheetModule.navHost.characterSheetNavGraph
import com.marshall.pyerite.characterSkillsModule.navHost.characterSkillsNavGraph
import com.marshall.pyerite.charactersListModule.navHost.charactersListNavGraph
import com.marshall.pyerite.charactersListModule.viewModel.CharacterViewModel
import com.marshall.pyerite.databaseHierarchyModule.navHost.databaseNavGraph
import com.marshall.pyerite.entityProfileModule.ui.EntityProfileBottomSheet
import com.marshall.pyerite.entityProfileModule.viewModel.EntityProfileViewModel
import com.marshall.pyerite.mainPageModule.navHost.MainRoute
import com.marshall.pyerite.mainPageModule.navHost.mainNavGraph
import com.marshall.pyerite.loyaltyPointsModule.navHost.loyaltyPointsNavGraph
import com.marshall.pyerite.peoplePlacesModule.navHost.peoplePlacesNavGraph
import com.marshall.pyerite.corporationModule.industry.navHost.corporationIndustryNavGraph
import com.marshall.pyerite.corporationModule.members.navHost.corporationMembersNavGraph
import com.marshall.pyerite.corporationModule.structures.navHost.corporationStructuresNavGraph
import com.marshall.pyerite.corporationModule.wallet.navHost.corporationWalletNavGraph
import com.marshall.pyerite.personalPropertyModule.navHost.personalPropertyNavGraph
import com.marshall.pyerite.ui.golbalComponents.LocalOpenEntityProfile
import com.marshall.pyerite.ui.golbalComponents.UniverseEntityRef
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val characterViewModel: CharacterViewModel = koinViewModel()
    val entityProfileViewModel: EntityProfileViewModel = koinViewModel()
    val currentCharacter by characterViewModel.currentCharacter.collectAsState()
    val entityProfileState by entityProfileViewModel.uiState.collectAsState()
    val viewerCharacterId = currentCharacter?.characterId
    val openEntityProfile: (UniverseEntityRef) -> Unit = remember(
        viewerCharacterId,
        entityProfileViewModel,
    ) {
        { ref ->
            if (viewerCharacterId != null) {
                entityProfileViewModel.open(ref, viewerCharacterId)
            }
        }
    }

    CompositionLocalProvider(LocalOpenEntityProfile provides openEntityProfile) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = MainRoute.Root.route,
            ) {
                mainNavGraph(navController)
                databaseNavGraph(navController)
                charactersListNavGraph(navController)
                characterSheetNavGraph(navController)
                characterClonesNavGraph(navController)
                characterSkillsNavGraph(navController)
                characterMasteryNavGraph(navController)
                characterMailNavGraph(navController)
                characterCalendarNavGraph(navController)
                personalPropertyNavGraph(navController)
                corporationWalletNavGraph(navController)
                corporationMembersNavGraph(navController)
                corporationStructuresNavGraph(navController)
                corporationIndustryNavGraph(navController)
                loyaltyPointsNavGraph(navController)
                peoplePlacesNavGraph(navController)
            }
            if (entityProfileState.stack.isNotEmpty()) {
                EntityProfileBottomSheet(viewModel = entityProfileViewModel)
            }
        }
    }
}
