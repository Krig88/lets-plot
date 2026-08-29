package org.jetbrains.letsPlot.core.plot.base.render.style

import org.jetbrains.letsPlot.commons.geometry.DoubleVector

interface PathStylizer {
    fun apply(
        points: List<DoubleVector>,
        amplitudeScale: Double = 1.0,
        segmentLengthScale: Double = 1.0,
    ): List<DoubleVector>
}
