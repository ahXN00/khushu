package com.kaizen.khushu.ui.util

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * A utility that creates a morphing [Shape] between two [RoundedPolygon]s.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberMorphShape(
    start: RoundedPolygon,
    end: RoundedPolygon,
    progress: Float
): Shape {
    val morph = remember(start, end) { Morph(start, end) }
    
    return remember(morph, progress) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                // normalized path from morph (usually centered at 0.5, 0.5 or 0,0)
                val path = morph.toPath(progress).asComposePath()
                
                val matrix = androidx.compose.ui.graphics.Matrix()
                // Scale the 1x1 normalized polygon to the actual size
                matrix.scale(size.width, size.height)
                path.transform(matrix)
                
                return Outline.Generic(path)
            }
        }
    }
}
