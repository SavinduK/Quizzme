package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.QuizzMeRepository
import com.example.ui.components.TermFolderDrawerContent
import com.example.ui.screens.AddNotesScreen
import com.example.ui.screens.AddPastPaperScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LessonReaderScreen
import com.example.ui.screens.LessonsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrackerScreen
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.viewmodel.AddContentViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LessonReaderViewModel
import com.example.ui.viewmodel.LessonsViewModel
import com.example.ui.viewmodel.ReaderTab
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TrackerViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object LessonsRoute

@Serializable
data object TrackerRoute

@Serializable
data object SettingsRoute

@Serializable
data object AddNotesRoute

@Serializable
data object AddPaperRoute

@Serializable
data class LessonReaderRoute(
    val lectureId: String,
    val initialTab: String = ReaderTab.NOTES.name
)

private data class TopLevelTab(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val topLevelTabs = listOf(
    TopLevelTab("Home", HomeRoute, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    TopLevelTab("Lessons", LessonsRoute, Icons.Rounded.School, Icons.Outlined.School, "nav_lessons"),
    TopLevelTab("Tracker", TrackerRoute, Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tracker"),
    TopLevelTab("Settings", SettingsRoute, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun QuizzMeApp(
    repository: QuizzMeRepository,
    widthSizeClass: WindowWidthSizeClass
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val ext = LocalQuizzMeColors.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isHome = currentDestination == null || currentDestination.hasRoute<HomeRoute>()
    val isLessons = currentDestination?.hasRoute<LessonsRoute>() == true
    val isTopLevel = isHome ||
        isLessons ||
        currentDestination?.hasRoute<TrackerRoute>() == true ||
        currentDestination?.hasRoute<SettingsRoute>() == true
    val drawerEnabled = isHome || isLessons

    val useNavRail = widthSizeClass != WindowWidthSizeClass.Compact

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val homeViewModel: HomeViewModel = viewModel(
        factory = simpleFactory { HomeViewModel(repository) }
    )
    val lessonsViewModel: LessonsViewModel = viewModel(
        factory = simpleFactory { LessonsViewModel(repository) }
    )
    val trackerViewModel: TrackerViewModel = viewModel(
        factory = simpleFactory { TrackerViewModel(repository) }
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = simpleFactory { SettingsViewModel(repository) }
    )

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val lessonsState by lessonsViewModel.uiState.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerEnabled,
        drawerContent = {
            if (isLessons) {
                TermFolderDrawerContent(
                    headerTitle = "Lecture Folders",
                    items = lessonsState.drawerItems,
                    selectedTerm = lessonsState.selectedTerm,
                    selectedItemId = null,
                    sortOrder = lessonsState.sortOrder,
                    onSortOrderChange = lessonsViewModel::setSortOrder,
                    onSelectAll = {
                        lessonsViewModel.resetFilters()
                        scope.launch { drawerState.close() }
                    },
                    onSelectTerm = { term ->
                        lessonsViewModel.setSelectedTerm(term)
                        scope.launch { drawerState.close() }
                    },
                    onSelectItem = { item ->
                        scope.launch { drawerState.close() }
                        navController.navigate(
                            LessonReaderRoute(
                                lectureId = item.id,
                                initialTab = ReaderTab.NOTES.name
                            )
                        )
                    },
                    onClose = { scope.launch { drawerState.close() } }
                )
            } else {
                TermFolderDrawerContent(
                    headerTitle = "Quiz Folders",
                    items = homeState.drawerItems,
                    selectedTerm = homeState.selectedTerm,
                    selectedItemId = homeState.selectedDrawerItemId,
                    sortOrder = homeState.sortOrder,
                    onSortOrderChange = homeViewModel::setSortOrder,
                    onSelectAll = {
                        homeViewModel.clearDrawerSelection()
                        scope.launch { drawerState.close() }
                    },
                    onSelectTerm = { term ->
                        homeViewModel.setSelectedTerm(term)
                        scope.launch { drawerState.close() }
                    },
                    onSelectItem = { item ->
                        homeViewModel.selectDrawerItem(item)
                        scope.launch { drawerState.close() }
                    },
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = ext.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevel && !useNavRail) {
                    Box {
                        HorizontalDivider(color = ext.border, thickness = 1.dp)
                        NavigationBar(
                            containerColor = ext.card,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            topLevelTabs.forEach { tab ->
                                val selected = when (tab.route) {
                                    HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                    LessonsRoute -> currentDestination?.hasRoute<LessonsRoute>() == true
                                    TrackerRoute -> currentDestination?.hasRoute<TrackerRoute>() == true
                                    SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ext.accent,
                                        selectedTextColor = ext.accent,
                                        unselectedIconColor = ext.subtext,
                                        unselectedTextColor = ext.subtext,
                                        indicatorColor = ext.accentSoft
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useNavRail) {
                    NavigationRail(
                        containerColor = ext.card,
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        topLevelTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                LessonsRoute -> currentDestination?.hasRoute<LessonsRoute>() == true
                                TrackerRoute -> currentDestination?.hasRoute<TrackerRoute>() == true
                                SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ext.accent,
                                    selectedTextColor = ext.accent,
                                    unselectedIconColor = ext.subtext,
                                    unselectedTextColor = ext.subtext,
                                    indicatorColor = ext.accentSoft
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                    VerticalDivider(color = ext.border)
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateToAddPaper = { navController.navigate(AddPaperRoute) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<LessonsRoute> {
                        LessonsScreen(
                            viewModel = lessonsViewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onAddNotesClick = { navController.navigate(AddNotesRoute) },
                            onOpenLesson = { lectureId, initialTab ->
                                navController.navigate(
                                    LessonReaderRoute(
                                        lectureId = lectureId,
                                        initialTab = initialTab.name
                                    )
                                )
                            },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<TrackerRoute> {
                        TrackerScreen(
                            viewModel = trackerViewModel,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    composable<AddNotesRoute> {
                        val addVm: AddContentViewModel = viewModel(
                            factory = simpleFactory { AddContentViewModel(repository) }
                        )
                        AddNotesScreen(
                            viewModel = addVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AddPaperRoute> {
                        val addVm: AddContentViewModel = viewModel(
                            factory = simpleFactory { AddContentViewModel(repository) }
                        )
                        AddPastPaperScreen(
                            viewModel = addVm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<LessonReaderRoute> { entry ->
                        val args = entry.toRoute<LessonReaderRoute>()
                        val tab = runCatching { ReaderTab.valueOf(args.initialTab) }
                            .getOrDefault(ReaderTab.NOTES)
                        val readerVm: LessonReaderViewModel = viewModel(
                            key = "reader_${args.lectureId}",
                            factory = simpleFactory {
                                LessonReaderViewModel(
                                    lectureId = args.lectureId,
                                    initialTab = tab,
                                    repository = repository
                                )
                            }
                        )
                        LessonReaderScreen(
                            viewModel = readerVm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

private inline fun <reified VM : ViewModel> simpleFactory(
    crossinline creator: () -> VM
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }
}
