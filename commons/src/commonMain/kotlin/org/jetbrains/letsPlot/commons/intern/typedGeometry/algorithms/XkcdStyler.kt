package org.jetbrains.letsPlot.commons.intern.typedGeometry.algorithms

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.intern.math.distance
import kotlin.random.Random

object XkcdStyler {
    const val DEFAULT_WOBBLE_AMPLITUDE = 2.0
    const val DEFAULT_SEGMENT_LENGTH = 10.0
    private const val DEFAULT_SEED: Long = 42

    fun stylize(
        points: List<DoubleVector>,
        amplitude: Double = DEFAULT_WOBBLE_AMPLITUDE,
        segmentLength: Double = DEFAULT_SEGMENT_LENGTH,
        seed: Long = DEFAULT_SEED
    ): List<DoubleVector> {
        if (points.size < 2) return points

        val random = Random(seed)
        val result = mutableListOf<DoubleVector>()
        result.add(points.first())

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val subdivided = subdivideSegment(p1, p2, segmentLength, amplitude, random)
            result.addAll(subdivided.drop(1))
        }

        return result
    }

    private fun subdivideSegment(
        p1: DoubleVector,
        p2: DoubleVector,
        segmentLength: Double,
        amplitude: Double,
        random: Random
    ): List<DoubleVector> {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val length = distance(p1.x, p1.y, p2.x, p2.y)

        if (length < segmentLength) {
            return listOf(p1, p2)
        }

        val numSegments = (length / segmentLength).toInt()
        val result = mutableListOf<DoubleVector>()
        result.add(p1)

        val perpX = -dy / length
        val perpY = dx / length

        for (j in 1 until numSegments) {
            val t = j.toDouble() / numSegments

            val baseX = p1.x + dx * t
            val baseY = p1.y + dy * t

            val offset = (random.nextDouble() * 2 - 1) * amplitude

            result.add(DoubleVector(baseX + perpX * offset, baseY + perpY * offset))
        }

        result.add(p2)
        return result
    }
}
