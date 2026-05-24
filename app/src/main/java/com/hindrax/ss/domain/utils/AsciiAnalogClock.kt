package com.hindrax.ss.domain.utils

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object AsciiAnalogClock {
    fun render(totalSeconds: Int, remainingSeconds: Int): String {
        val size = 13
        val center = size / 2
        val radius = 5
        val grid = Array(size) { CharArray(size) { ' ' } }

        for (hour in 0 until 12) {
            val angle = (hour / 12.0) * 2.0 * PI - PI / 2.0
            val x = center + (cos(angle) * radius).roundToInt()
            val y = center + (sin(angle) * radius).roundToInt()
            grid[y][x] = if (hour % 3 == 0) '+' else '.'
        }

        drawHand(grid, center, radius - 1, fraction = remainingSeconds.coerceAtLeast(0) / 60.0, marker = '*')
        val safeTotal = totalSeconds.coerceAtLeast(1)
        val progressFraction = (remainingSeconds.coerceIn(0, safeTotal).toDouble() / safeTotal.toDouble())
        drawHand(grid, center, radius - 3, fraction = progressFraction, marker = '#')
        grid[center][center] = 'o'

        return grid.joinToString("\n") { row -> row.concatToString().trimEnd() }
    }

    private fun drawHand(
        grid: Array<CharArray>,
        center: Int,
        length: Int,
        fraction: Double,
        marker: Char
    ) {
        val angle = fraction * 2.0 * PI - PI / 2.0
        for (step in 1..length) {
            val x = center + (cos(angle) * step).roundToInt()
            val y = center + (sin(angle) * step).roundToInt()
            if (y in grid.indices && x in grid[y].indices) {
                grid[y][x] = marker
            }
        }
    }
}
