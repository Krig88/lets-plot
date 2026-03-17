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
import org.jetbrains.letsPlot.core.plot.base.geom.util.GeomHelper
import org.jetbrains.letsPlot.core.plot.base.render.SvgRoot
import org.jetbrains.letsPlot.core.plot.base.render.svg.LinePath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        val gaugeWidth = width.takeIf(::isValidWidth) ?: return
        val geomHelper = GeomHelper(pos, coord, ctx)

        for (p in aesthetics.dataPoints()) {
            val (x, y) = p.finiteOrNull(Aes.X, Aes.Y) ?: continue
            val center = geomHelper.toClient(x, y, p) ?: continue
            val midRadius = AesScaling.pieDiameter(p) / 2.0
            if (!midRadius.isFinite() || midRadius <= 0.0) continue
            val alpha = (p.alpha() ?: 1.0).coerceIn(0.0, 1.0)
            val halfWidth = gaugeWidth / 2.0
            val innerRadius = (midRadius - halfWidth).coerceAtLeast(0.0)
            val outerRadius = midRadius + halfWidth
            if (!outerRadius.isFinite() || outerRadius <= 0.0) continue
            val fillColor = p.fill() ?: p.color() ?: continue
            val color = p.color() ?: fillColor

            root.add(
                createBand(
                    center = center,
                    innerRadius = innerRadius,
                    outerRadius = outerRadius,
                    fromAngle = START_ANGLE,
                    toAngle = END_ANGLE,
                    fillColor = withOpacity(fillColor, alpha * BACKGROUND_ALPHA)
                ).rootGroup
            )

            if (gaugeValue > 0.0) {
                val valueEndAngle = START_ANGLE - SWEEP_ANGLE * gaugeValue
                root.add(
                    createBand(
                        center = center,
                        innerRadius = innerRadius,
                        outerRadius = outerRadius,
                        fromAngle = START_ANGLE,
                        toAngle = valueEndAngle,
                        fillColor = withOpacity(color, alpha)
                    ).rootGroup
                )
            }
        }
    }

    private fun createBand(
        center: DoubleVector,
        innerRadius: Double,
        outerRadius: Double,
        fromAngle: Double,
        toAngle: Double,
        fillColor: Color,
    ): LinePath {
        val outerArc = arcPoints(center, outerRadius, fromAngle, toAngle)
        val innerArc = arcPoints(center, innerRadius, fromAngle, toAngle).reversed()

        return LinePath.polygon(outerArc + innerArc).apply {
            fill().set(fillColor)
            color().set(Color.TRANSPARENT)
            width().set(0.0)
        }
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
        private const val BACKGROUND_ALPHA = 0.35
        private const val ARC_SEGMENTS = 48
        private const val START_ANGLE = PI
        private const val END_ANGLE = 0.0
        private const val SWEEP_ANGLE = PI
    }
}
