package com.example.sagap.navigation

sealed class Route(val path: String) {
    object Test : Route("test")
    object Locations : Route("locations")
    object Testing : Route("testing")
    object Result : Route("result")
    object Map : Route("map")
    object SpotDetail : Route("spot/{id}") {
        fun create(id: Int) = "spot/$id"
    }
    object History : Route("history")
    object Report : Route("report")
}
