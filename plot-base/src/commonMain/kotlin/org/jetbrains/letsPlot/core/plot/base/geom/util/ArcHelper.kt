package org.jetbrains.letsPlot.core.plot.base.geom.util

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.intern.typedGeometry.algorithms.XkcdStyler
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object ArcHelper {
    private const val ARC_SEGMENT_LENGTH = 10.0
    private const val ARC_WOBBLE_AMPLITUDE = 0.5
    fun arcToPoints(
        center: DoubleVector,
        radius: Double,
        startAngle: Double,
        endAngle: Double,
        segmentLength: Double = ARC_SEGMENT_LENGTH
    ): List<DoubleVector> {
        if (radius <= 0) return listOf(center)

        val arcLength = radius * abs(endAngle - startAngle)
        val numSegments = maxOf(2, (arcLength / segmentLength).toInt())

        val points = mutableListOf<DoubleVector>()

        for (i in 0..numSegments) {
            val t = i.toDouble() / numSegments
            val angle = startAngle + (endAngle - startAngle) * t
            val x = radius * cos(angle)
            val y = radius * sin(angle)
            val offset = DoubleVector(x, y)
            val point = center.add(offset)
            points.add(point)
        }

        return points
    }


    fun stylizedArc(
        center: DoubleVector,
        radius: Double,
        startAngle: Double,
        endAngle: Double
    ): List<DoubleVector> {
        val points = arcToPoints(center, radius, startAngle, endAngle)
        return XkcdStyler.stylize(points, amplitude = ARC_WOBBLE_AMPLITUDE)
    }


    fun stylizedLine(points: List<DoubleVector>): List<DoubleVector> {
        return XkcdStyler.stylize(points, amplitude = ARC_WOBBLE_AMPLITUDE)
    }
}
