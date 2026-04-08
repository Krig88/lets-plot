/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.livemap.api

import org.jetbrains.letsPlot.commons.intern.spatial.LonLat
import org.jetbrains.letsPlot.commons.intern.typedGeometry.Vec
import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.livemap.chart.ChartElementComponent
import org.jetbrains.letsPlot.livemap.chart.GaugeSpecComponent
import org.jetbrains.letsPlot.livemap.chart.IndexComponent
import org.jetbrains.letsPlot.livemap.chart.LocatorComponent
import org.jetbrains.letsPlot.livemap.chart.gauge.GaugeLocator
import org.jetbrains.letsPlot.livemap.chart.gauge.GaugeRenderer
import org.jetbrains.letsPlot.livemap.core.ecs.EcsEntity
import org.jetbrains.letsPlot.livemap.core.ecs.addComponents
import org.jetbrains.letsPlot.livemap.core.layers.LayerKind
import org.jetbrains.letsPlot.livemap.mapengine.LayerEntitiesComponent
import org.jetbrains.letsPlot.livemap.mapengine.MapProjection
import org.jetbrains.letsPlot.livemap.mapengine.RenderableComponent
import org.jetbrains.letsPlot.livemap.mapengine.placement.ScreenDimensionComponent
import org.jetbrains.letsPlot.livemap.mapengine.placement.WorldOriginComponent

@LiveMapDsl
class GaugeLayerBuilder(
    val factory: FeatureEntityFactory,
    val mapProjection: MapProjection,
)

fun FeatureLayerBuilder.gauges(block: GaugeLayerBuilder.() -> Unit) {
    val layerEntity = myComponentManager
        .createEntity("map_layer_gauge")
        .addComponents {
            +layerManager.addLayer("geom_gauge", LayerKind.FEATURES)
            +LayerEntitiesComponent()
        }

    GaugeLayerBuilder(
        FeatureEntityFactory(layerEntity, panningPointsMaxCount = 100),
        mapProjection,
    ).apply(block)
}

fun GaugeLayerBuilder.gauge(block: GaugeEntityBuilder.() -> Unit) {
    GaugeEntityBuilder(factory)
        .apply(block)
        .build()
}

@LiveMapDsl
class GaugeEntityBuilder(
    private val myFactory: FeatureEntityFactory,
) {
    var sizeScalingRange: ClosedRange<Int>? = null
    var alphaScalingEnabled: Boolean = false

    var layerIndex: Int? = null
    var index: Int = 0
    var point: Vec<LonLat> = LonLat.ZERO_VEC

    var radius: Double = 0.0
    var holeSize: Double = 0.0
    var value: Double = 0.0

    var fillColor: Color = Color.WHITE
    var strokeColor: Color = Color.TRANSPARENT
    var strokeWidth: Double = 0.0

    fun build(): EcsEntity {
        return myFactory.createStaticFeatureWithLocation("map_ent_s_gauge", point)
            .run {
                myFactory.incrementLayerPointsTotalCount(1)
                setInitializer { worldPoint ->
                    if (layerIndex != null) {
                        +IndexComponent(layerIndex!!, index)
                    }
                    +LocatorComponent(GaugeLocator)
                    +RenderableComponent().apply {
                        renderer = GaugeRenderer()
                    }
                    +ChartElementComponent().apply {
                        sizeScalingRange = this@GaugeEntityBuilder.sizeScalingRange
                        alphaScalingEnabled = this@GaugeEntityBuilder.alphaScalingEnabled
                        fillColor = this@GaugeEntityBuilder.fillColor
                        strokeColor = this@GaugeEntityBuilder.strokeColor
                        strokeWidth = this@GaugeEntityBuilder.strokeWidth
                    }
                    +GaugeSpecComponent().apply {
                        radius = this@GaugeEntityBuilder.radius
                        holeSize = this@GaugeEntityBuilder.holeSize
                        value = this@GaugeEntityBuilder.value
                    }
                    +WorldOriginComponent(worldPoint)
                    +ScreenDimensionComponent()
                }
            }
    }
}
