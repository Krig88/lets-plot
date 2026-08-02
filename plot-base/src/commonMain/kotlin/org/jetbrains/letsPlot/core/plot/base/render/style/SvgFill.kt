package org.jetbrains.letsPlot.core.plot.base.render.style

import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgElement

sealed interface SvgFill {
    data class Solid(val color: Color) : SvgFill

    data class Pattern(val id: String, val def: SvgElement, val fallbackColor: Color) : SvgFill
}
