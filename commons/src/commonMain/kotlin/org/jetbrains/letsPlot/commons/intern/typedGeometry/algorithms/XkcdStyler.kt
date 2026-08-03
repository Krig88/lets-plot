package org.jetbrains.letsPlot.commons.intern.typedGeometry.algorithms

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.intern.math.distance
import kotlin.random.Random

object XkcdStyler {
    const val DEFAULT_WOBBLE_AMPLITUDE = 2.0
    const val DEFAULT_SEGMENT_LENGTH = 10.0

    private const val ANCHOR_SPACING_JITTER = 0.3
    private const val SUBDIVISION_JITTER = 0.25

    fun stylize(
        points: List<DoubleVector>,
        amplitude: Double = DEFAULT_WOBBLE_AMPLITUDE,
        segmentLength: Double = DEFAULT_SEGMENT_LENGTH,
        seed: Long? = null
    ): List<DoubleVector> {
        if (points.size < 2 || amplitude <= 0.0 || segmentLength <= 0.0) return points

        val random = seed?.let { Random(it) } ?: Random.Default

        val cumLen = DoubleArray(points.size)
        for (i in 1 until points.size) {
            cumLen[i] = cumLen[i - 1] + distance(points[i - 1].x, points[i - 1].y, points[i].x, points[i].y)
        }
        val totalLength = cumLen.last()
        if (totalLength < 1e-6) return points

        val anchorPositions = mutableListOf(0.0)
        val anchorOffsets = mutableListOf(0.0)
        var s = 0.0
        while (true) {
            val step = segmentLength * (1.0 + (random.nextDouble() * 2 - 1) * ANCHOR_SPACING_JITTER)
            s += step
            if (s >= totalLength - segmentLength * 0.5) break
            anchorPositions.add(s)
            anchorOffsets.add((random.nextDouble() * 2 - 1) * amplitude)
        }
        if (anchorPositions.size == 1 && totalLength > 2.0 * amplitude) {
            anchorPositions.add(totalLength * 0.5)
            anchorOffsets.add((random.nextDouble() * 2 - 1) * amplitude)
        }
        anchorPositions.add(totalLength)
        anchorOffsets.add(0.0)

        fun wobbleAt(pos: Double): Double {
            if (pos <= 0.0) return anchorOffsets.first()
            if (pos >= totalLength) return anchorOffsets.last()
            var idx = anchorPositions.binarySearch(pos)
            if (idx < 0) idx = -idx - 1
            val i = (idx - 1).coerceIn(0, anchorPositions.size - 2)
            val s0 = anchorPositions[i]
            val s1 = anchorPositions[i + 1]
            val t = ((pos - s0) / (s1 - s0)).coerceIn(0.0, 1.0)
            val ts = t * t * (3.0 - 2.0 * t)
            return anchorOffsets[i] * (1.0 - ts) + anchorOffsets[i + 1] * ts
        }

        val result = mutableListOf<DoubleVector>()
        result.add(points.first())

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val segLen = cumLen[i + 1] - cumLen[i]
            if (segLen < 1e-6) continue

            val perpX = -(p2.y - p1.y) / segLen
            val perpY = (p2.x - p1.x) / segLen

            if (segLen > segmentLength) {
                val n = (segLen / segmentLength).toInt().coerceAtLeast(1)
                for (k in 1 until n) {
                    val jitter = (random.nextDouble() * 2 - 1) * SUBDIVISION_JITTER / n
                    val alpha = (k.toDouble() / n + jitter).coerceIn(0.0, 1.0)
                    val bx = p1.x + (p2.x - p1.x) * alpha
                    val by = p1.y + (p2.y - p1.y) * alpha
                    val w = wobbleAt(cumLen[i] + segLen * alpha)
                    result.add(DoubleVector(bx + perpX * w, by + perpY * w))
                }
            }

            val wEnd = wobbleAt(cumLen[i + 1])
            result.add(DoubleVector(p2.x + perpX * wEnd, p2.y + perpY * wEnd))
        }

        return result
    }
}
