/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.livemap.chart.gauge

import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.core.canvas.Context2d
import org.jetbrains.letsPlot.livemap.chart.ChartElementComponent
import org.jetbrains.letsPlot.livemap.chart.GaugeSpecComponent
import org.jetbrains.letsPlot.livemap.core.ecs.EcsEntity
import org.jetbrains.letsPlot.livemap.mapengine.RenderHelper
import org.jetbrains.letsPlot.livemap.mapengine.Renderer
import org.jetbrains.letsPlot.livemap.mapengine.placement.WorldOriginComponent
import org.jetbrains.letsPlot.livemap.mapengine.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GaugeRenderer : Renderer {
    override fun render(
        entity: EcsEntity,
        ctx: Context2d,
        renderHelper: RenderHelper) {

        val chartElement = entity.get<ChartElementComponent>()
        val gaugeSpec = entity.get<GaugeSpecComponent>()

        val radius = gaugeSpec.radius * chartElement.scalingSizeFactor
        val holeRadius = radius * gaugeSpec.holeSize
        val value = gaugeSpec.value

        ctx.translate(renderHelper.dimToScreen(entity.get<WorldOriginComponent>().origin))

        val scaledFillColor = chartElement.scaledFillColor()
        drawBand(
            ctx = ctx,
            outerRadius = radius,
            innerRadius = holeRadius,
            fromAngle = START_ANGLE_RAD,
            toAngle = END_ANGLE_RAD,
            fillColor = scaleAlpha(scaledFillColor, BACKGROUND_ALPHA),
            strokeColor = null,
            strokeWidth = 0.0,
        )

        if (value > 0.0) {
            drawBand(
                ctx = ctx,
                outerRadius = radius,
                innerRadius = holeRadius,
                fromAngle = START_ANGLE_RAD,
                toAngle = START_ANGLE_RAD - SWEEP_ANGLE_RAD * value,
                fillColor = scaledFillColor,
                strokeColor = chartElement.strokeColor?.let { chartElement.scaledStrokeColor() },
                strokeWidth = chartElement.scaledStrokeWidth(),
            )
        }
    }

    private fun drawBand(
        ctx: Context2d,
        outerRadius: Double,
        innerRadius: Double,
        fromAngle: Double,
        toAngle: Double,
        fillColor: Color,
        strokeColor: Color?,
        strokeWidth: Double,
    ) {


        val band = GaugeBand(
            innerRadius = innerRadius,
            outerRadius = outerRadius,
            fromAngle = fromAngle,
            toAngle = toAngle,
        )

        ctx.beginPath()
        ctx.moveTo(band.outerArcStart.first, band.outerArcStart.second)
        drawOuterArc(ctx, band)

        if (innerRadius > 0.0) {
            ctx.lineTo(band.innerArcEnd.first, band.innerArcEnd.second)
            drawInnerArc(ctx, band)
        } else {
            ctx.lineTo(0.0, 0.0)
        }

        ctx.closePath()

        ctx.setFillStyle(fillColor)
        ctx.fill()

        if (strokeColor != null && strokeWidth > 0.0) {
            ctx.setStrokeStyle(strokeColor)
            ctx.setLineWidth(strokeWidth)
            ctx.stroke()
        }
    }

    private fun drawOuterArc(ctx: Context2d, band: GaugeBand) {
        if (band.isDegenerate) {
            return
        }

        ctx.ellipse(
            x = 0.0,
            y = 0.0,
            radiusX = band.outerRadius,
            radiusY = band.outerRadius,
            rotation = 0.0,
            startAngle = -band.fromAngle,
            endAngle = -band.toAngle,
            anticlockwise = !band.outerSweep,
        )
    }

    private fun drawInnerArc(ctx: Context2d, band: GaugeBand) {
        if (band.innerRadius <= 0.0 || band.isDegenerate) {
            return
        }

        ctx.ellipse(
            x = 0.0,
            y = 0.0,
            radiusX = band.innerRadius,
            radiusY = band.innerRadius,
            rotation = 0.0,
            startAngle = -band.toAngle,
            endAngle = -band.fromAngle,
            anticlockwise = band.outerSweep,
        )
    }

    private data class GaugeBand(
        val innerRadius: Double,
        val outerRadius: Double,
        val fromAngle: Double,
        val toAngle: Double,
    ) {
        val outerArcStart: Pair<Double, Double>
            get() = arcPoint(outerRadius, fromAngle)

        val innerArcEnd: Pair<Double, Double>
            get() = arcPoint(innerRadius, toAngle)

        val outerSweep: Boolean
            get() = toAngle < fromAngle

        val isDegenerate: Boolean
            get() = toAngle == fromAngle

        private fun arcPoint(radius: Double, angle: Double): Pair<Double, Double> {
            return Pair(radius * cos(angle), -radius * sin(angle))
        }
    }

    private fun scaleAlpha(color: Color, factor: Double): Color {
        return color.changeAlpha((color.alpha * factor).toInt())
    }

    companion object {
        private const val BACKGROUND_ALPHA = 0.2
        private const val START_ANGLE_RAD = PI
        private const val END_ANGLE_RAD = 0.0
        private const val SWEEP_ANGLE_RAD = PI
    }
}
