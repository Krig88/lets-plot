package org.jetbrains.letsPlot.core.plot.base.render.style

object PrimitiveStyles {
    // Intentionally hardcoded while style selection is not wired through the pipeline.
    val compiledStyle: CompiledStyle = XkcdCompiledStyle

    var roughness: Double
        get() = XkcdCompiledStyle.roughness
        set(value) {
            XkcdCompiledStyle.roughness = value
        }
}