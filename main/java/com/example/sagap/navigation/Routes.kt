package com.example.sagap.navigation

sealed class Route(val path: String) {
    object Test : Route("test")
    object Locations : Route("locations")
    object Testing : Route("testing")
    object Map : Route("map")
    object History : Route("history")
    object Report : Route("report")
}