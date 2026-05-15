/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.geom.util

import org.jetbrains.letsPlot.commons.geometry.DoubleRectangle
import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.intern.typedGeometry.algorithms.AdaptiveResampler
import org.jetbrains.letsPlot.commons.intern.typedGeometry.algorithms.AdaptiveResampler.Companion.resample
import org.jetbrains.letsPlot.commons.intern.typedGeometry.algorithms.XkcdStyler
import org.jetbrains.letsPlot.core.commons.geometry.PolylineSimplifier
import org.jetbrains.letsPlot.core.plot.base.*
import org.jetbrains.letsPlot.core.plot.base.render.svg.lineString
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgNode
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgPathDataBuilder
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgPathElement
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgRectElement
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgShape
import org.jetbrains.letsPlot.datamodel.svg.dom.slim.SvgSlimElements
import org.jetbrains.letsPlot.datamodel.svg.dom.slim.SvgSlimGroup

class RectanglesHelper(
    private val myAesthetics: Aesthetics,
    pos: PositionAdjustment,
    coord: CoordinateSystem,
    ctx: GeomContext,
    private val geometryFactory: (DataPointAesthetics) -> DoubleRectangle?
) : GeomHelper(pos, coord, ctx) {
    private fun rectangleCorners(rect: DoubleRectangle): List<DoubleVector> {
        // Closed polyline for a rectangle; reused across branches to avoid duplicating point order logic.
        return listOf(
            DoubleVector(rect.left, rect.top),
            DoubleVector(rect.right, rect.top),
            DoubleVector(rect.right, rect.bottom),
            DoubleVector(rect.left, rect.bottom),
            DoubleVector(rect.left, rect.top)
        )
    }

    private fun stylize(points: List<DoubleVector>): List<DoubleVector> {
        return XkcdStyler.stylize(points)
    }

    private fun createRectPath(points: List<DoubleVector>): SvgPathElement {
        return SvgPathElement().apply {
            d().set(SvgPathDataBuilder().lineString(points).build())
        }
    }

    private fun createRectangleSvgNode(rect: DoubleRectangle): SvgNode {
        // Use a stylized polyline path, because SvgRectElement cannot express hand-drawn edges
        return createRectPath(stylize(rectangleCorners(rect)))
    }

    // TODO: Replace with SvgRectHelper
    fun createNonLinearRectangles(handler: (DataPointAesthetics, SvgNode, List<DoubleVector>) -> Unit) {
        myAesthetics.dataPoints().forEach { p ->
            geometryFactory(p)?.let { rect ->
                val polyRect = stylize(
                    resample(
                        precision = AdaptiveResampler.PIXEL_PRECISION,
                        points = rectangleCorners(rect)
                    ) { toClient(it, p) }
                )

                val svgPoly = createRectPath(polyRect)

                decorate(svgPoly, p)
                handler(p, svgPoly, polyRect)
            }
        }
    }

    fun createRectangles(handler: (DataPointAesthetics, SvgNode, DoubleRectangle) -> Unit) {
        myAesthetics.dataPoints().forEach { p ->
            geometryFactory(p)?.let { rect ->
                val clientRect = toClient(rect, p) ?: return@let
                val svgNode = createRectangleSvgNode(clientRect)
                decorate(svgNode as SvgShape, p)
                handler(p, svgNode, clientRect)
            }
        }
    }

    fun createRectangles(): MutableList<SvgNode> {
        val result = ArrayList<SvgNode>()

        for (index in 0 until myAesthetics.dataPointCount()) {
            val p = myAesthetics.dataPointAt(index)
            val clientRect = geometryFactory(p) ?: continue

            val svgNode = createRectangleSvgNode(clientRect)
            decorate(svgNode as SvgShape, p)
            result.add(svgNode)
        }

        return result
    }

    fun createSvgRectHelper(): SvgRectHelper {
        return SvgRectHelper()
    }

    inner class SvgRectHelper {
        private var onGeometry: (DataPointAesthetics, DoubleRectangle?, List<DoubleVector>?) -> Unit = { _, _, _ -> }
        private var myResamplingEnabled = false
        private var myResamplingPrecision = AdaptiveResampler.PIXEL_PRECISION

        fun setResamplingEnabled(b: Boolean) {
            myResamplingEnabled = b
        }

        fun setResamplingPrecision(precision: Double) {
            myResamplingPrecision = precision
        }

        fun onGeometry(handler: (DataPointAesthetics, DoubleRectangle?, List<DoubleVector>?) -> Unit) {
            onGeometry = handler
        }

        fun createSlimRectangles(): SvgSlimGroup {
            val pointCount = myAesthetics.dataPointCount()
            val group = SvgSlimElements.g(pointCount)

            for (index in 0 until pointCount) {
                val p = myAesthetics.dataPointAt(index)
                val rect = geometryFactory(p) ?: continue

                val polyRect = if (myResamplingEnabled) {
                    resample(
                        precision = myResamplingPrecision,
                        points = rectangleCorners(rect)
                    ) { toClient(it, p) }
                } else {
                    val clientRect = toClient(rect, p) ?: continue
                    rectangleCorners(clientRect)
                }

                val styledPolyRect = stylize(polyRect)

                // Resampling of a tiny rectangle still can produce a very small polygon - simplify it.
                val simplified = PolylineSimplifier.douglasPeucker(styledPolyRect).setWeightLimit(PolylineSimplifier.DOUGLAS_PEUCKER_PIXEL_THRESHOLD).points.let {
                    if (it.size != 1) {
                        println("RectanglesHelper: expected a single path, but got ${it.size}")
                    }

                    it.firstOrNull() ?: emptyList()
                }

                onGeometry(p, null, simplified)

                val slimShape = SvgSlimElements.path(SvgPathDataBuilder().lineString(simplified).build())
                decorateSlimShape(slimShape, p)
                slimShape.appendTo(group)
            }
            return group
        }
    }
}
