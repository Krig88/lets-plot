package org.jetbrains.letsPlot.core.plot.base.render.style

fun interface StrokeAdjuster {
    fun adjustWidth(base: Double): Double
}
