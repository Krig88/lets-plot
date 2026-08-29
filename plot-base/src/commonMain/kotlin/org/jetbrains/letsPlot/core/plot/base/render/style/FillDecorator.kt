package org.jetbrains.letsPlot.core.plot.base.render.style

import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.core.plot.base.DataPointAesthetics

fun interface FillDecorator {
    fun decorate(base: Color, aes: DataPointAesthetics): SvgFill
}
