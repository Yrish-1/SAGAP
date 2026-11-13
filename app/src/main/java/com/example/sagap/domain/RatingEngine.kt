package com.example.sagap.domain

import com.example.sagap.data.model.Grade

object RatingEngine {
    // Each metric gets a level 0..3; the lowest level sets the overall grade.
    fun grade(mbps: Double, pingMs: Int, dbm: Int): Grade {
        val download = when {
            mbps >= 25 -> 3
            mbps >= 10 -> 2
            mbps >= 2 -> 1
            else -> 0
        }
        val ping = when {
            pingMs <= 30 -> 3
            pingMs <= 60 -> 2
            pingMs <= 120 -> 1
            else -> 0
        }
        val signal = when {
            dbm >= -60 -> 3
            dbm >= -67 -> 2
            dbm >= -75 -> 1
            else -> 0
        }
        return when (minOf(download, ping, signal)) {
            3 -> Grade.EXCELLENT
            2 -> Grade.GOOD
            1 -> Grade.FAIR
            else -> Grade.POOR
        }
    }
}