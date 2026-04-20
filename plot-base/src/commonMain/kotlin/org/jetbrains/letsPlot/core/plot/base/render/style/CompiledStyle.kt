package org.jetbrains.letsPlot.core.plot.base.render.style

import org.jetbrains.letsPlot.commons.geometry.DoubleRectangle
import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgPathData
import org.jetbrains.letsPlot.datamodel.svg.dom.slim.SvgSlimShape

interface CompiledStyle {
    fun styleLineString(points: List<DoubleVector>, closePath: Boolean = false): StyledPrimitive<List<DoubleVector>>

    fun stylePath(points: List<DoubleVector>, closePath: Boolean = false): StyledPrimitive<SvgPathData>
    fun stylePath(pathData: SvgPathData): StyledPrimitive<SvgPathData>
    fun styleRect(rect: DoubleRectangle): StyledPrimitive<SvgPathData>
    fun styleCircle(center: DoubleVector, radius: Double): StyledPrimitive<SvgPathData>

    fun styleSlimPath(pathData: Any): StyledPrimitive<SvgSlimShape>
    fun styleSlimRect(rect: DoubleRectangle): StyledPrimitive<SvgSlimShape>
    fun styleSlimCircle(center: DoubleVector, radius: Double): StyledPrimitive<SvgSlimShape>
    fun styleSlimLine(start: DoubleVector, end: DoubleVector): StyledPrimitive<SvgSlimShape>
}