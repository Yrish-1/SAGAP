package com.example.sagap

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sagap.data.sample.SampleData
import com.example.sagap.navigation.Route
import com.example.sagap.ui.components.PlaceholderScreen
import com.example.sagap.ui.screens.test.TestHomeScreen
import com.example.sagap.ui.theme.Navy

private data class Tab(val route: Route, val label: String, val icon: ImageVector)

// Screens that belong to the "Test" tab
private val testFlow = setOf(Route.Test.path, Route.Locations.path, Route.Testing.path)

@Composable
fun SagapApp() {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route

    // Temporary: later this moves into a ViewModel
    var selectedSpot by remember { mutableStateOf(SampleData.spots.first()) }

    fun goToTab(route: Route) {
        nav.navigate(route.path) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val tabs = listOf(
        Tab(Route.Test, "Test", Icons.Outlined.Speed),
        Tab(Route.Map, "Map", Icons.Outlined.Map),
        Tab(Route.History, "History", Icons.Outlined.History),
        Tab(Route.Report, "Report", Icons.Outlined.Flag),
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEach { tab ->
                    val selected = if (tab.route == Route.Test) current in testFlow
                    else current == tab.route.path
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
            composable(Route.Test.path) {
                TestHomeScreen(
                    spot = selectedSpot,
                    latest = SampleData.results.firstOrNull { it.spotId == selectedSpot.id },
                    onChangeLocation = { nav.navigate(Route.Locations.path) },
                    onStartTest = { nav.navigate(Route.Testing.path) },
                    onFindSpot = { goToTab(Route.Map) }
                )
            }
            composable(Route.Locations.path) { PlaceholderScreen("Choose your location") }
            composable(Route.Testing.path) { PlaceholderScreen("Testing your Wi-Fi") }
            composable(Route.Map.path) { PlaceholderScreen("Map") }
            composable(Route.History.path) { PlaceholderScreen("History") }
            composable(Route.Report.path) { PlaceholderScreen("Report") }
        }
    }
}