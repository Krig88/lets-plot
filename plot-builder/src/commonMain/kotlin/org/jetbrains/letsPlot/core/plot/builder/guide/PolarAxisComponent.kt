/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.builder.guide

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.intern.math.toDegrees
import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.core.plot.base.render.style.RenderTheme
import org.jetbrains.letsPlot.core.plot.base.render.svg.Label
import org.jetbrains.letsPlot.core.plot.base.render.svg.StrokeDashArraySupport
import org.jetbrains.letsPlot.core.plot.base.render.svg.SvgComponent
import org.jetbrains.letsPlot.core.plot.base.render.svg.Text.HorizontalAnchor
import org.jetbrains.letsPlot.core.plot.base.render.svg.Text.VerticalAnchor
import org.jetbrains.letsPlot.core.plot.base.render.svg.lineString
import org.jetbrains.letsPlot.core.plot.base.theme.AxisTheme
import org.jetbrains.letsPlot.core.plot.builder.AxisUtil
import org.jetbrains.letsPlot.core.plot.builder.PolarAxisUtil
import org.jetbrains.letsPlot.core.plot.builder.layout.PlotLabelSpecFactory
import org.jetbrains.letsPlot.core.plot.builder.presentation.Style
import org.jetbrains.letsPlot.datamodel.svg.dom.*
import kotlin.math.atan2

class PolarAxisComponent(
    private val length: Double,
    private val orientation: Orientation,
    private val breaksData: PolarAxisUtil.PolarBreaksData,
    private val labelAdjustments: AxisComponent.TickLabelAdjustments = AxisComponent.TickLabelAdjustments(orientation),
    private val axisTheme: AxisTheme,
    private val hideAxisBreaks: Boolean = false,
    private val renderTheme: RenderTheme = RenderTheme.DEFAULT,
) : SvgComponent() {

    private companion object {
        private const val TICK_MARK_AMPLITUDE_SCALE = 0.3
        private const val TICK_MARK_SEGMENT_SCALE = 0.2
    }
    init {
        rootGroup.pointerEvents().set(SvgGraphicsElement.PointerEvents.NONE)
    }

    override fun buildComponent() {
        buildAxis()
    }

    private fun buildAxis() {
        val rootElement = rootGroup

        // Ticks and labels
        if (!hideAxisBreaks && (axisTheme.showLabels() || axisTheme.showTickMarks())) {
            val tickLabelBaseOffset = AxisUtil.tickLabelBaseOffset(axisTheme, orientation)

            for ((i, v) in breaksData.majorBreaks.withIndex()) {
                val label = breaksData.majorLabels[i % breaksData.majorLabels.size]
                val labelOffset = tickLabelBaseOffset.add(labelAdjustments.additionalOffset(i))

                val (tickLabel, tickMark) = buildTick(label, labelOffset, axisTheme, v, breaksData.center)

                tickMark?.let { rootElement.children().add(it) }
                tickLabel?.let { rootElement.children().add(it.rootGroup) }
            }
        }

        // Axis line
        if (!hideAxisBreaks && axisTheme.showLine()) {
            val stylizer = renderTheme.paths
            if (orientation.isHorizontal) {
                val points = stylizer?.apply(breaksData.axisLine) ?: breaksData.axisLine
                val axisLine = SvgPathElement().apply {
                    d().set(SvgPathDataBuilder().lineString(points).build())
                    strokeWidth().set(axisTheme.lineWidth())
                    strokeColor().set(axisTheme.lineColor())
                    StrokeDashArraySupport.apply(this, axisTheme.lineWidth(), axisTheme.lineType())
                    fillColor().set(Color.TRANSPARENT)
                }
                rootElement.children().add(axisLine)
            } else {
                val axisLine: SvgNode = if (stylizer != null) {
                    val styled = stylizer.apply(
                        listOf(
                            DoubleVector(0.0, breaksData.axisLine[0].y),
                            DoubleVector(0.0, breaksData.axisLine[1].y)
                        )
                    )
                    SvgPathElement().apply {
                        d().set(SvgPathDataBuilder().lineString(styled).build())
                        fillColor().set(Color.TRANSPARENT)
                        strokeWidth().set(axisTheme.lineWidth())
                        strokeColor().set(axisTheme.lineColor())
                        StrokeDashArraySupport.apply(this, axisTheme.lineWidth(), axisTheme.lineType())
                    }
                } else {
                    SvgLineElement().apply {
                        y1().set(breaksData.axisLine[0].y)
                        y2().set(breaksData.axisLine[1].y)
                        strokeWidth().set(axisTheme.lineWidth())
                        strokeColor().set(axisTheme.lineColor())
                        StrokeDashArraySupport.apply(this, axisTheme.lineWidth(), axisTheme.lineType())
                    }
                }
                rootElement.children().add(axisLine)
            }
        }
    }

    private fun buildTick(
        label: String,
        labelOffset: DoubleVector,
        axisTheme: AxisTheme,
        breakCoord: DoubleVector,
        center: DoubleVector
    ): Pair<Label?, SvgNode?> {

        val tickMark: SvgNode? = if (axisTheme.showTickMarks()) {
            val markLength = axisTheme.tickMarkLength()
            val stylizer = renderTheme.paths
            if (stylizer != null) {
                val tickPoints: List<DoubleVector> = when (orientation) {
                    Orientation.LEFT -> listOf(
                        DoubleVector(0.0, breakCoord.y),
                        DoubleVector(-markLength, breakCoord.y)
                    )

                    Orientation.BOTTOM -> {
                        val tickMarkVector = breakCoord.mul(1 + markLength / breakCoord.length())
                        listOf(
                            breakCoord.add(center),
                            tickMarkVector.add(center)
                        )
                    }

                    Orientation.RIGHT -> error("Unsupported orientation $orientation")
                    Orientation.TOP -> error("Unsupported orientation $orientation")
                }
                val styled = stylizer.apply(
                    tickPoints,
                    amplitudeScale = TICK_MARK_AMPLITUDE_SCALE,
                    segmentLengthScale = TICK_MARK_SEGMENT_SCALE,
                )
                SvgPathElement().apply {
                    d().set(SvgPathDataBuilder().lineString(styled).build())
                    fillColor().set(Color.TRANSPARENT)
                    strokeWidth().set(axisTheme.tickMarkWidth())
                    strokeColor().set(axisTheme.tickMarkColor())
                    StrokeDashArraySupport.apply(this, axisTheme.tickMarkWidth(), axisTheme.tickMarkLineType())
                }
            } else {
                val tickMark = SvgLineElement()
                tickMark.strokeWidth().set(axisTheme.tickMarkWidth())
                tickMark.strokeColor().set(axisTheme.tickMarkColor())
                StrokeDashArraySupport.apply(tickMark, axisTheme.tickMarkWidth(), axisTheme.tickMarkLineType())

                when (orientation) {
                    Orientation.LEFT -> {
                        tickMark.x2().set(-markLength)
                        tickMark.y2().set(0.0)
                        SvgUtils.transformTranslate(tickMark, 0.0, breakCoord.y)
                    }

                    Orientation.BOTTOM -> {
                        val tickMarkVector = breakCoord.mul(1 + markLength / breakCoord.length())
                        tickMark.x2().set(tickMarkVector.add(center).x)
                        tickMark.y2().set(tickMarkVector.add(center).y)

                        tickMark.x1().set(breakCoord.add(center).x)
                        tickMark.y1().set(breakCoord.add(center).y)
                    }

                    Orientation.RIGHT -> error("Unsupported orientation $orientation")
                    Orientation.TOP -> error("Unsupported orientation $orientation")
                }
                tickMark
            }
        } else {
            null
        }

        val tickLabel = if (axisTheme.showLabels()) {
            val tickLabel = Label(label, markdown = axisTheme.titleStyle().markdown)
            tickLabel.addClassName("${Style.AXIS_TEXT}-${axisTheme.axis}")

            when (orientation.isHorizontal) {
                false -> {
                    // Vertical axis is always on the left side of the plot for now - ignore breakCoord.x
                    tickLabel.moveTo(labelOffset.x, labelOffset.y + breakCoord.y)
                    tickLabel.setHorizontalAnchor(HorizontalAnchor.RIGHT)
                    tickLabel.setVerticalAnchor(VerticalAnchor.CENTER)
                }

                true -> {
                    val pos = breakCoord.mul(1 + labelOffset.length() / breakCoord.length())
                    tickLabel.moveTo(pos.add(center))
                    val degrees = toDegrees(atan2(pos.y, pos.x))
                    val (hAnchor, vAnchor) = when (degrees) {
                        in -5.0..5.0 -> HorizontalAnchor.LEFT to VerticalAnchor.CENTER
                        in 85.0..95.0 -> HorizontalAnchor.MIDDLE to VerticalAnchor.TOP
                        in -95.0..-85.0 -> HorizontalAnchor.MIDDLE to VerticalAnchor.BOTTOM
                        in 175.0..185.0 -> HorizontalAnchor.RIGHT to VerticalAnchor.CENTER
                        in -185.0..-175.0 -> HorizontalAnchor.RIGHT to VerticalAnchor.CENTER

                        in 0.0..90.0 -> HorizontalAnchor.LEFT to VerticalAnchor.TOP
                        in 90.0..180.0 -> HorizontalAnchor.RIGHT to VerticalAnchor.TOP
                        in -180.0..-90.0 -> HorizontalAnchor.RIGHT to VerticalAnchor.BOTTOM
                        in -90.0..0.0 -> HorizontalAnchor.LEFT to VerticalAnchor.BOTTOM
                        else -> HorizontalAnchor.MIDDLE to VerticalAnchor.CENTER
                    }

                    tickLabel.setHorizontalAnchor(hAnchor)
                    tickLabel.setVerticalAnchor(vAnchor)
                }
            }

            val tickHeight = PlotLabelSpecFactory.axisTick(axisTheme).height()
            tickLabel.setFontSize(tickHeight)
            tickLabel.setLineHeight(tickHeight)
            tickLabel.rotate(labelAdjustments.rotationDegree)
            tickLabel
        } else {
            null
        }

        return tickLabel to tickMark
    }

}
