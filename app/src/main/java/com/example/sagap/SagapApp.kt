package com.example.sagap

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sagap.data.repository.SpotRepository
import com.example.sagap.data.sample.SampleData
import com.example.sagap.navigation.Route
import com.example.sagap.ui.SagapViewModel
import com.example.sagap.ui.components.PlaceholderScreen
import com.example.sagap.ui.components.SagapTopBar
import com.example.sagap.ui.screens.history.HistoryScreen
import com.example.sagap.ui.screens.map.MapScreen
import com.example.sagap.ui.screens.map.SpotDetailScreen
import com.example.sagap.ui.screens.report.ReportScreen
import com.example.sagap.ui.screens.test.LocationPickerScreen
import com.example.sagap.ui.screens.test.ResultScreen
import com.example.sagap.ui.screens.test.TestHomeScreen
import com.example.sagap.ui.screens.test.TestingScreen
import com.example.sagap.ui.theme.Navy

private data class Tab(val route: Route, val label: String, val icon: ImageVector)

// Screens that belong to the "Test" tab
private val testFlow = setOf(
    Route.Test.path, Route.Locations.path, Route.Testing.path, Route.Result.path
)

@Composable
fun SagapApp(vm: SagapViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val spots = SampleData.spots

    // Switch tabs. restore = false opens the tab fresh instead of bringing back its old screens.
    fun goToTab(route: Route, restore: Boolean = true) {
        nav.navigate(route.path) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = restore
        }
    }

    // Back to the Student Home (Test tab root)
    fun goHome() {
        if (!nav.popBackStack(Route.Test.path, inclusive = false)) {
            nav.navigate(Route.Test.path) { launchSingleTop = true }
        }
    }

    // Testing and Result must ALWAYS sit on top of the Test tab, never inside the Map tab.
    fun startTestFlow() {
        vm.startTest()
        if (current !in testFlow) goToTab(Route.Test)
        nav.navigate(Route.Testing.path)
    }

    val tabs = listOf(
        Tab(Route.Test, "Test", Icons.Outlined.Speed),
        Tab(Route.Map, "Map", Icons.Outlined.Map),
        Tab(Route.History, "History", Icons.Outlined.History),
        Tab(Route.Report, "Report", Icons.Outlined.Flag)
    )

    Scaffold(
        topBar = { SagapTopBar() },   // one shared header for every tab
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEach { tab ->
                    val selected = when (tab.route) {
                        Route.Test -> current in testFlow
                        Route.Map -> current == Route.Map.path || current == Route.SpotDetail.path
                        else -> current == tab.route.path
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { goToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Navy,
                            indicatorColor = Navy
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Route.Test.path,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ---------- Test tab ----------
            composable(Route.Test.path) {
                TestHomeScreen(
                    spot = state.selectedSpot,
                    latest = SpotRepository.latest(state.results, state.selectedSpot.id),
                    onChangeLocation = { nav.navigate(Route.Locations.path) },
                    onStartTest = { startTestFlow() },
                    onFindSpot = { goToTab(Route.Map) },
                    onBeforeExam = { startTestFlow() }   // opens "Testing your Wi-Fi"
                )
            }
            composable(Route.Locations.path) {
                LocationPickerScreen(
                    spots = spots,
                    results = state.results,
                    selectedId = state.selectedSpot.id,
                    onConfirm = { vm.selectSpot(it); nav.popBackStack() },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Route.Testing.path) {
                TestingScreen(
                    spot = state.selectedSpot,
                    state = state.test,
                    onCancel = { vm.cancelTest(); nav.popBackStack() },
                    onFinished = {
                        nav.navigate(Route.Result.path) {
                            popUpTo(Route.Testing.path) { inclusive = true }
                        }
                    }
                )
            }
            composable(Route.Result.path) {
                val result = state.lastResult
                if (result == null) {
                    PlaceholderScreen("No result yet")
                } else {
                    ResultScreen(
                        spot = spots.firstOrNull { it.id == result.spotId } ?: state.selectedSpot,
                        result = result,
                        onViewMap = { goToTab(Route.Map, restore = false) },
                        onTestAgain = {
                            vm.startTest()
                            nav.navigate(Route.Testing.path) {
                                popUpTo(Route.Result.path) { inclusive = true }
                            }
                        },
                        onGoHome = { goHome() }
                    )
                }
            }

            // ---------- Map tab ----------
            composable(Route.Map.path) {
                MapScreen(
                    spots = spots,
                    results = state.results,
                    onSpotClick = { nav.navigate(Route.SpotDetail.create(it.id)) }
                )
            }
            composable(
                route = Route.SpotDetail.path,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: 1
                val spot = spots.firstOrNull { it.id == id } ?: spots.first()
                SpotDetailScreen(
                    spot = spot,
                    results = state.results,
                    isSelected = state.selectedSpot.id == spot.id,
                    onBack = { nav.popBackStack() },
                    onTest = {
                        vm.selectSpot(spot)
                        startTestFlow()
                    },
                    onReport = {
                        vm.setReportSpot(spot.id)
                        goToTab(Route.Report)
                    }
                )
            }

            // ---------- History + Report ----------
            composable(Route.History.path) {
                HistoryScreen(history = state.history, spots = spots)
            }
            composable(Route.Report.path) {
                ReportScreen(
                    spots = spots,
                    initialSpotId = state.reportSpotId ?: state.selectedSpot.id,
                    latestTest = state.history.firstOrNull(),
                    onSubmit = { _, _, _, _ -> /* TODO: send to Firestore or email later */ }
                )
            }
        }
    }
}