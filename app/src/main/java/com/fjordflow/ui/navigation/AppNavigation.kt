package com.fjordflow.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fjordflow.data.db.AppDatabase
import com.fjordflow.data.repository.FlashCardRepository
import com.fjordflow.data.repository.RoadmapRepository
import com.fjordflow.data.repository.WordRepository
import com.fjordflow.ui.screens.flashcards.FlashcardsScreen
import com.fjordflow.ui.screens.flashcards.FlashcardsViewModel
import com.fjordflow.ui.screens.reader.ReaderScreen
import com.fjordflow.ui.screens.reader.ReaderViewModel
import com.fjordflow.ui.screens.roadmap.RoadmapScreen
import com.fjordflow.ui.screens.roadmap.RoadmapViewModel

sealed class NavRoute(val route: String, val label: String, val icon: ImageVector) {
    object Reader     : NavRoute("reader",     "Reader",     Icons.Outlined.AutoStories)
    object Flashcards : NavRoute("flashcards", "Flashcards", Icons.Outlined.Style)
    object Roadmap    : NavRoute("roadmap",    "Roadmap",    Icons.Outlined.Map)
}

private val navItems = listOf(NavRoute.Reader, NavRoute.Flashcards, NavRoute.Roadmap)

@Composable
fun AppNavigation(db: AppDatabase) {
    val navController = rememberNavController()

    // Repositories (in a real app, inject via Hilt/Koin)
    val wordRepo      = remember { WordRepository(db.wordDao(), db.flashCardDao()) }
    val cardRepo      = remember { FlashCardRepository(db.flashCardDao()) }
    val roadmapRepo   = remember { RoadmapRepository(db.roadmapDao()) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = androidx.compose.ui.unit.Dp(0f)
            ) {
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentDest = currentBackStack?.destination
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Reader.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn() + slideInHorizontally() },
            exitTransition   = { fadeOut() },
            popEnterTransition  = { fadeIn() },
            popExitTransition   = { fadeOut() + slideOutHorizontally() }
        ) {
            composable(NavRoute.Reader.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel<ReaderViewModel>(
                    factory = ReaderViewModel.Factory(wordRepo)
                )
                ReaderScreen(vm)
            }
            composable(NavRoute.Flashcards.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel<FlashcardsViewModel>(
                    factory = FlashcardsViewModel.Factory(cardRepo)
                )
                FlashcardsScreen(vm)
            }
            composable(NavRoute.Roadmap.route) {
                val vm = androidx.lifecycle.viewmodel.compose.viewModel<RoadmapViewModel>(
                    factory = RoadmapViewModel.Factory(roadmapRepo)
                )
                RoadmapScreen(vm)
            }
        }
    }
}
