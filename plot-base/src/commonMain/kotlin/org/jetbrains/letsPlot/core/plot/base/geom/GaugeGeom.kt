/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.geom

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.values.Colors.withOpacity
import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.core.plot.base.*
import org.jetbrains.letsPlot.core.plot.base.aes.AesScaling
import org.jetbrains.letsPlot.core.plot.base.aes.AestheticsUtil
import org.jetbrains.letsPlot.core.plot.base.geom.util.GeomHelper
import org.jetbrains.letsPlot.core.plot.base.geom.util.HintColorUtil
import org.jetbrains.letsPlot.core.plot.base.render.SvgRoot
import org.jetbrains.letsPlot.core.plot.base.render.svg.LinePath
import org.jetbrains.letsPlot.core.plot.base.tooltip.GeomTargetCollector
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgPathDataBuilder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class GaugeGeom : GeomBase() {
    var value: Double = DEF_VALUE
    var width: Double = DEF_WIDTH

    override fun buildIntern(
        root: SvgRoot,
        aesthetics: Aesthetics,
        pos: PositionAdjustment,
        coord: CoordinateSystem,
        ctx: GeomContext,
    ) {
        val gaugeValue = value.takeIf(::isValidValue) ?: return
        if (!isValidWidth(width)) return
        val geomHelper = GeomHelper(pos, coord, ctx)
        val colorMarkerMapper = HintColorUtil.createColorMarkerMapper(GeomKind.GAUGE, ctx)

        for (p in aesthetics.dataPoints()) {
            val (x, y) = p.finiteOrNull(Aes.X, Aes.Y) ?: continue
            val center = geomHelper.toClient(x, y, p) ?: continue
            val radius = AesScaling.pieDiameter(p) / 2.0
            if (!radius.isFinite() || radius <= 0.0) continue
            val fillColor = p.fill()!!
            val fillAlpha = AestheticsUtil.alpha(fillColor, p)
            val borderColor = p.color()!!
            val borderWidth = AesScaling.strokeWidth(p, DataPointAesthetics::stroke)
            val backgroundBandPoints = bandPoints(
                center = center,
                innerRadius = 0.0,
                outerRadius = radius,
                fromAngle = START_ANGLE,
                toAngle = END_ANGLE,
            )

            root.add(
                createBand(
                    center = center,
                    radius = radius,
                    fromAngle = START_ANGLE,
                    toAngle = END_ANGLE,
                    fillColor = withOpacity(fillColor, (fillAlpha * BACKGROUND_ALPHA)),
                    borderColor = Color.TRANSPARENT,
                    borderWidth = 0.0
                ).rootGroup
            )
            ctx.targetCollector.addPolygon(
                points = backgroundBandPoints,
                index = p.index(),
                tooltipParams = GeomTargetCollector.TooltipParams(
                    markerColors = colorMarkerMapper(p)
                ),
            )

            if (gaugeValue > 0.0) {
                val valueEndAngle = START_ANGLE - SWEEP_ANGLE * gaugeValue
                root.add(
                    createBand(
                        center = center,
                        radius = radius,
                        fromAngle = START_ANGLE,
                        toAngle = valueEndAngle,
                        fillColor = withOpacity(fillColor, fillAlpha),
                        borderColor = borderColor,
                        borderWidth = borderWidth
                    ).rootGroup
                )
            }
        }
    }

    private fun createBand(
        center: DoubleVector,
        radius: Double,
        fromAngle: Double,
        toAngle: Double,
        fillColor: Color,
        borderColor: Color,
        borderWidth: Double,
    ): LinePath {
        val startPoint = arcPoint(center, radius, fromAngle)

        val path = SvgPathDataBuilder(true).apply {
            moveTo(center)
            lineTo(startPoint)
            cubicBezierArc(center, radius, fromAngle, toAngle)
            lineTo(center)
            closePath()
        }

        return LinePath(path).apply {
            fill().set(fillColor)
            color().set(borderColor)
            width().set(borderWidth)
        }
    }

    private fun SvgPathDataBuilder.cubicBezierArc(
        center: DoubleVector,
        radius: Double,
        fromAngle: Double,
        toAngle: Double,
    ) {
        val totalAngle = toAngle - fromAngle
        if (totalAngle == 0.0) {
            return
        }

        val segments = ceil(abs(totalAngle) / MAX_BEZIER_ARC_SEGMENT_ANGLE).toInt().coerceAtLeast(1)
        val segmentAngle = totalAngle / segments

        var startAngle = fromAngle
        repeat(segments) {
            val endAngle = startAngle + segmentAngle
            val startPoint = arcPoint(center, radius, startAngle)
            val endPoint = arcPoint(center, radius, endAngle)

            val k = 4.0 / 3.0 * tan((endAngle - startAngle) / 4.0)
            val startTangent = arcTangent(startAngle)
            val endTangent = arcTangent(endAngle)

            val controlStart = startPoint.add(startTangent.mul(radius * k))
            val controlEnd = endPoint.subtract(endTangent.mul(radius * k))

            curveTo(controlStart = controlStart, controlEnd = controlEnd, to = endPoint)
            startAngle = endAngle
        }
    }

    private fun arcPoint(center: DoubleVector, radius: Double, angle: Double): DoubleVector {
        return center.add(DoubleVector(radius * cos(angle), -radius * sin(angle)))
    }

    private fun arcTangent(angle: Double): DoubleVector {
        return DoubleVector(-sin(angle), -cos(angle))
    }

    private fun bandPoints(
        center: DoubleVector,
        innerRadius: Double,
        outerRadius: Double,
        fromAngle: Double,
        toAngle: Double,
    ): List<DoubleVector> {
        val outerArc = arcPoints(center, outerRadius, fromAngle, toAngle)
        val innerArc = arcPoints(center, innerRadius, fromAngle, toAngle).reversed()
        return outerArc + innerArc
    }

    private fun arcPoints(
        center: DoubleVector,
        radius: Double,
        fromAngle: Double,
        toAngle: Double,
    ): List<DoubleVector> {
        return (0..ARC_SEGMENTS).map { step ->
            val t = step.toDouble() / ARC_SEGMENTS
            val angle = fromAngle + (toAngle - fromAngle) * t
            center.add(DoubleVector(radius * cos(angle), -radius * sin(angle)))
        }
    }

    private fun isValidValue(value: Double): Boolean {
        return value.isFinite() && value in 0.0..1.0
    }

    private fun isValidWidth(width: Double): Boolean {
        return width.isFinite() && width > 0.0
    }

    companion object {
        const val HANDLES_GROUPS = false

        private const val DEF_VALUE = 0.0
        private const val DEF_WIDTH = 2.2
        private const val BACKGROUND_ALPHA = 0.2
        private const val ARC_SEGMENTS = 48
        private const val MAX_BEZIER_ARC_SEGMENT_ANGLE = PI / 2
        private const val START_ANGLE = PI
        private const val END_ANGLE = 0.0
        private const val SWEEP_ANGLE = PI
    }
}
