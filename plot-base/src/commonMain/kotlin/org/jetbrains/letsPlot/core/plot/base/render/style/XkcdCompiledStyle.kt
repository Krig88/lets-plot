package org.jetbrains.letsPlot.core.plot.base.render.style

import org.jetbrains.letsPlot.commons.geometry.DoubleRectangle
import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgPathData
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgPathDataBuilder
import org.jetbrains.letsPlot.datamodel.svg.dom.slim.SvgSlimElements
import org.jetbrains.letsPlot.datamodel.svg.dom.slim.SvgSlimShape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

object XkcdCompiledStyle : CompiledStyle {
    var roughness: Double = DEFAULT_ROUGHNESS

    override fun styleLineString(points: List<DoubleVector>, closePath: Boolean): StyledPrimitive<List<DoubleVector>> {
        val source = if (closePath && points.size > 1 && points.first() == points.last()) {
            points.dropLast(1)
        } else {
            points
        }

        if (source.size <= 1) {
            return StyledPrimitive(source)
        }

        val segmentCount = if (closePath) source.size else source.size - 1
        val styledPoints = ArrayList<DoubleVector>(source.size + segmentCount * MAX_KINK_COUNT)
        styledPoints.add(source.first())

        for (segmentIndex in 0 until segmentCount) {
            val start = source[segmentIndex]
            val end = source[(segmentIndex + 1) % source.size]
            val segmentLength = end.subtract(start).length()
            val kinkCount = segmentKinkCount(segmentLength)

            for (kinkIndex in 1..kinkCount) {
                val t = kinkIndex.toDouble() / (kinkCount + 1)
                val basePoint = start.add(end.subtract(start).mul(t))
                val offset = segmentOffset(
                    start = start,
                    end = end,
                    segmentIndex = segmentIndex,
                    segmentCount = segmentCount,
                    closePath = closePath,
                    kinkIndex = kinkIndex,
                    kinkCount = kinkCount,
                    t = t
                )
                styledPoints.add(basePoint.add(offset))
            }

            if (!closePath || segmentIndex < segmentCount - 1) {
                styledPoints.add(end)
            }
        }

        return StyledPrimitive(styledPoints)
    }

    override fun stylePath(points: List<DoubleVector>, closePath: Boolean): StyledPrimitive<SvgPathData> {
        if (points.isEmpty()) {
            return StyledPrimitive(SvgPathData.EMPTY)
        }

        val styledPoints = styleLineString(points, closePath).primitive
        if (styledPoints.isEmpty()) {
            return StyledPrimitive(SvgPathData.EMPTY)
        }

        val pathData = SvgPathDataBuilder(true).apply {
            moveTo(styledPoints.first())
            styledPoints
                .asSequence()
                .drop(1)
                .forEach(::lineTo)

            if (closePath) {
                closePath()
            }
        }.build()

        return StyledPrimitive(pathData)
    }

    override fun stylePath(pathData: SvgPathData): StyledPrimitive<SvgPathData> {
        return StyledPrimitive(pathData)
    }

    override fun styleRect(rect: DoubleRectangle): StyledPrimitive<SvgPathData> {
        return stylePath(
            points = listOf(
                DoubleVector(rect.left, rect.top),
                DoubleVector(rect.right, rect.top),
                DoubleVector(rect.right, rect.bottom),
                DoubleVector(rect.left, rect.bottom),
            ),
            closePath = true
        )
    }

    override fun styleCircle(center: DoubleVector, radius: Double): StyledPrimitive<SvgPathData> {
        if (radius <= 0.0) {
            val pathData = SvgPathDataBuilder(true)
                .moveTo(center)
                .build()
            return StyledPrimitive(pathData)
        }

        val points = ArrayList<DoubleVector>(CIRCLE_SEGMENTS)
        for (i in 0 until CIRCLE_SEGMENTS) {
            val angle = i.toDouble() * 2.0 * PI / CIRCLE_SEGMENTS
            points.add(
                center.add(
                    DoubleVector(
                        x = radius * cos(angle),
                        y = radius * sin(angle)
                    )
                )
            )
        }
        return stylePath(points, closePath = true)
    }

    override fun styleSlimPath(pathData: Any): StyledPrimitive<SvgSlimShape> {
        return StyledPrimitive(SvgSlimElements.path(pathData))
    }

    override fun styleSlimRect(rect: DoubleRectangle): StyledPrimitive<SvgSlimShape> {
        val pathData = styleRect(rect).primitive
        return StyledPrimitive(SvgSlimElements.path(pathData))
    }

    override fun styleSlimCircle(center: DoubleVector, radius: Double): StyledPrimitive<SvgSlimShape> {
        val pathData = styleCircle(center, radius).primitive
        return StyledPrimitive(SvgSlimElements.path(pathData))
    }

    override fun styleSlimLine(start: DoubleVector, end: DoubleVector): StyledPrimitive<SvgSlimShape> {
        val pathData = stylePath(listOf(start, end), closePath = false).primitive
        return StyledPrimitive(SvgSlimElements.path(pathData))
    }

    private fun segmentOffset(
        start: DoubleVector,
        end: DoubleVector,
        segmentIndex: Int,
        segmentCount: Int,
        closePath: Boolean,
        kinkIndex: Int,
        kinkCount: Int,
        t: Double,
    ): DoubleVector {
        val segment = end.subtract(start)
        val segmentLength = segment.length()
        if (segmentLength <= 1e-6) {
            return DoubleVector.ZERO
        }

        val amplitude = roughness * min(MAX_WOBBLE_PX, segmentLength * WOBBLE_RATIO)
        if (amplitude < MIN_WOBBLE_PX) {
            return DoubleVector.ZERO
        }

        val tangent = segment.mul(1 / segmentLength)
        val normal = DoubleVector(-tangent.y, tangent.x)

        val seed = segmentSeed(start, end, segmentIndex, segmentCount, closePath, kinkIndex, kinkCount)
        val zigzagSign = if ((segmentIndex + kinkIndex) % 2 == 0) 1.0 else -1.0
        val centerWeight = 1.0 - 0.7 * abs(0.5 - t)

        val normalPart = amplitude * centerWeight *
            (ZIGZAG_RATIO * zigzagSign + RANDOM_NORMAL_RATIO * signedNoise(seed))
        val tangentPart = amplitude * TANGENT_WOBBLE_RATIO * signedNoise(seed xor TANGENT_SEED_MASK)

        return normal.mul(normalPart).add(tangent.mul(tangentPart))
    }

    private fun segmentSeed(
        start: DoubleVector,
        end: DoubleVector,
        segmentIndex: Int,
        segmentCount: Int,
        closePath: Boolean,
        kinkIndex: Int,
        kinkCount: Int,
    ): Int {
        var seed = BASE_SEED
        seed = mix(seed, start.x.hashCode())
        seed = mix(seed, start.y.hashCode())
        seed = mix(seed, end.x.hashCode())
        seed = mix(seed, end.y.hashCode())
        seed = mix(seed, segmentIndex)
        seed = mix(seed, segmentCount)
        seed = mix(seed, if (closePath) 1 else 0)
        seed = mix(seed, kinkIndex)
        seed = mix(seed, kinkCount)
        return seed
    }

    private fun segmentKinkCount(segmentLength: Double): Int {
        val baseKinkCount = when {
            segmentLength > 120.0 -> 5
            segmentLength > 50.0 -> 3
            else -> 2
        }

        return (baseKinkCount * roughness)
            .roundToInt()
            .coerceIn(1, MAX_KINK_COUNT)
    }

    private fun signedNoise(initialSeed: Int): Double {
        return Random(initialSeed).nextDouble(from = -1.0, until = 1.0)
    }

    private fun mix(seed: Int, value: Int): Int {
        return seed + value
    }

    private const val CIRCLE_SEGMENTS = 24

    private const val DEFAULT_ROUGHNESS = 0.75

    private const val WOBBLE_RATIO = 0.25
    private const val TANGENT_WOBBLE_RATIO = 0.45
    private const val MIN_WOBBLE_PX = 0.05
    private const val MAX_WOBBLE_PX = 4.0

    private const val ZIGZAG_RATIO = 0.5
    private const val RANDOM_NORMAL_RATIO = 0.5
    private const val MAX_KINK_COUNT = 10

    private const val BASE_SEED = 42
    private const val TANGENT_SEED_MASK = 43
}
