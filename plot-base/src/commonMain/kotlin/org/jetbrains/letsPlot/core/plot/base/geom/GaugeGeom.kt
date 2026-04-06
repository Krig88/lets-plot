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

class GaugeGeom : GeomBase() {
    var hole: Double = DEF_HOLE

    override fun buildIntern(
        root: SvgRoot,
        aesthetics: Aesthetics,
        pos: PositionAdjustment,
        coord: CoordinateSystem,
        ctx: GeomContext,
    ) {
        val geomHelper = GeomHelper(pos, coord, ctx)
        val colorsByDataPoint = HintColorUtil.createColorMarkerMapper(GeomKind.GAUGE, ctx)

        for (p in aesthetics.dataPoints()) {
            val (x, y) = p.finiteOrNull(Aes.X, Aes.Y) ?: continue
            val gaugeValue = p.value() ?: continue
            val center = geomHelper.toClient(x, y, p) ?: continue
            val radius = AesScaling.pieDiameter(p) / 2.0
            val holeRadius = radius * hole
            val backgroundBand = GaugeBand(
                center = center,
                innerRadius = holeRadius,
                outerRadius = radius,
                fromAngle = START_ANGLE,
                toAngle = END_ANGLE
            )
            val fillColor = p.fill()!!
            val fillAlpha = AestheticsUtil.alpha(fillColor, p)
            val borderColor = p.color()!!
            val borderWidth = AesScaling.strokeWidth(p, DataPointAesthetics::stroke) * GAUGE_STROKE_SCALE

            root.add(
                buildSvgBand(
                    band = backgroundBand,
                    fillColor = withOpacity(fillColor, (fillAlpha * BACKGROUND_ALPHA)),
                    borderColor = Color.TRANSPARENT,
                    borderWidth = 0.0
                ).rootGroup
            )
            ctx.targetCollector.addPoint(
                index = p.index(),
                point = center,
                radius = radius,
                tooltipParams = GeomTargetCollector.TooltipParams(
                    markerColors = colorsByDataPoint(p)
                ),
            )

            if (gaugeValue > 0.0) {
                val valueBand = GaugeBand(
                    center = center,
                    innerRadius = holeRadius,
                    outerRadius = radius,
                    fromAngle = START_ANGLE,
                    toAngle = START_ANGLE - SWEEP_ANGLE * gaugeValue
                )
                root.add(
                    buildSvgBand(
                        band = valueBand,
                        fillColor = withOpacity(fillColor, fillAlpha),
                        borderColor = borderColor,
                        borderWidth = borderWidth
                    ).rootGroup
                )
            }
        }
    }

    private fun buildSvgBand(
        band: GaugeBand,
        fillColor: Color,
        borderColor: Color,
        borderWidth: Double,
    ): LinePath {
        val path = SvgPathDataBuilder(true).apply {
            moveTo(band.outerArcStart)
            svgOuterArc(band)
            if (band.innerRadius > 0.0) {
                lineTo(band.innerArcEnd)
                svgInnerArc(band)
            } else {
                lineTo(band.center)
            }
            closePath()
        }

        return LinePath(path).apply {
            fill().set(fillColor)
            color().set(borderColor)
            width().set(borderWidth)
        }
    }

    private fun SvgPathDataBuilder.svgOuterArc(band: GaugeBand) {
        if (band.isDegenerate) {
            return
        }

        ellipticalArc(
            rx = band.outerRadius,
            ry = band.outerRadius,
            xAxisRotation = 0.0,
            largeArc = band.largeArc,
            sweep = band.outerSweep,
            to = band.outerArcEnd
        )
    }

    private fun SvgPathDataBuilder.svgInnerArc(band: GaugeBand) {
        if (band.innerRadius <= 0.0 || band.isDegenerate) {
            return
        }

        ellipticalArc(
            rx = band.innerRadius,
            ry = band.innerRadius,
            xAxisRotation = 0.0,
            largeArc = band.largeArc,
            sweep = !band.outerSweep,
            to = band.innerArcStart
        )
    }

    private data class GaugeBand(
        val center: DoubleVector,
        val innerRadius: Double,
        val outerRadius: Double,
        val fromAngle: Double,
        val toAngle: Double,
    ) {
        val outerArcStart: DoubleVector
            get() = arcPoint(outerRadius, fromAngle)

        val outerArcEnd: DoubleVector
            get() = arcPoint(outerRadius, toAngle)

        val innerArcStart: DoubleVector
            get() = arcPoint(innerRadius, fromAngle)

        val innerArcEnd: DoubleVector
            get() = arcPoint(innerRadius, toAngle)

        val largeArc: Boolean
            get() = abs(toAngle - fromAngle) > PI

        val outerSweep: Boolean
            get() = toAngle < fromAngle

        private fun arcPoint(radius: Double, angle: Double): DoubleVector {
            return center.add(DoubleVector(radius, 0.0).rotate(-angle))
        }

        val isDegenerate: Boolean
            get() = toAngle == fromAngle
    }
    companion object {
        const val HANDLES_GROUPS = false

        private const val DEF_HOLE = 0.0
        private const val GAUGE_STROKE_SCALE = 0.3
        private const val BACKGROUND_ALPHA = 0.2
        private const val START_ANGLE = PI
        private const val END_ANGLE = 0.0
        private const val SWEEP_ANGLE = PI
    }
}
