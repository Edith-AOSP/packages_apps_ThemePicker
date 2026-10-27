/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.customization.picker.color.ui.compose

import android.content.theming.ThemeStyle
import android.graphics.RuntimeShader
import androidx.annotation.ColorInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.compose.animation.bounceable
import com.android.customization.picker.color.ui.compose.Shader.CUSTOM_SHADER
import com.android.systemui.monet.ColorScheme
import com.android.themepicker.R
import com.android.wallpaper.picker.option.ui.compose.OptionBounceable
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

object Shader {
    @Language("AGSL")
    val CUSTOM_SHADER =
        """
            uniform float2 resolution;
            layout(color) uniform half4 backgroundColor;
            layout(color) uniform half4 circleColor;
            layout(color) uniform half4 squareColor;

            const float BLUR_PERCENT = 0.5;
            const float BOX_SIZE_PERCENT = 0.5;
            const float CIRCLE_RAD_PERCENT = 0.42;
            const float CIRCLE_OPACITY = 0.9;

            float sdCircle( float2 p, float r ) {
              return length(p) - r;
            }

            float sdBox( float2 p, float2 b )
            {
                float2 d = abs(p)-b;
                return length(max(d,0.0)) + min(max(d.x,d.y),0.0);
            }

            half4 main(float2 p)
            {
              float blurDist = resolution.y * BLUR_PERCENT;
              half4 color = backgroundColor;
              // Add box
              float2 sideLengths = resolution.xy * BOX_SIZE_PERCENT;
              float dBox = sdBox(p.xy, sideLengths) - blurDist / 2.0;
              if (dBox < 0.0) {
                float weight = min((dBox / blurDist) * -1.0, 1.0);
                color = mix(color, squareColor, smoothstep(0.0, 1.0, weight));
              }
              // Add circle
              float radius = resolution.y * CIRCLE_RAD_PERCENT;
              float2 center = p.xy - resolution.xy * (1 - CIRCLE_RAD_PERCENT);
              float dCircle = sdCircle(center, radius) - blurDist / 2.0;
              if (dCircle < 0.0) {
                float weight = min((dCircle / blurDist) * -1.0, 1.0) * CIRCLE_OPACITY;
                color = mix (color, circleColor, smoothstep(0.0, 1.0, weight));
              }
              return color;
            }
        """
            .trimIndent()
}

/** UI for selecting a color variant (aka theme style). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorVariantPicker(
    styleOptions: List<StyleBounceable>,
    selectedOption: Int?,
    previewingSeedColor: Int?,
    previewingIsDarkMode: Boolean,
    onClick: (Int) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    navigateToLanding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    DrillDownFloatingSheet(
        onCancel = onCancel,
        onConfirm = onConfirm,
        navigateToLanding = navigateToLanding,
        modifier = modifier,
    ) {
        LazyRow(
            modifier = Modifier.padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(styleOptions) { idx, option ->
                val isSelected = option.style == selectedOption
                val animatedAlpha: Float by animateFloatAsState(if (isSelected) 1f else 0.25f)
                val scheme =
                    remember(previewingSeedColor, previewingIsDarkMode) {
                        previewingSeedColor?.let {
                            ColorScheme(previewingSeedColor, previewingIsDarkMode, option.style)
                                .materialScheme
                        }
                    }
                Column(modifier = Modifier.width(68.dp)) {
                    ColorOption(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(68.dp)
                                .bounceable(
                                    bounceable = styleOptions[idx],
                                    previousBounceable =
                                        if (idx > 0) styleOptions[idx - 1] else null,
                                    nextBounceable =
                                        if (idx < styleOptions.lastIndex) styleOptions[idx + 1]
                                        else null,
                                    orientation = Horizontal,
                                ),
                        isSelected = isSelected,
                        onClick = {
                            onClick(option.style)
                            coroutineScope.launch { option.clickBounceAnimate() }
                        },
                    ) {
                        val materialColors = MaterialDynamicColors()
                        val colors =
                            scheme?.let {
                                when (option.style) {
                                    ThemeStyle.SPRITZ ->
                                        VariantColors(
                                            background =
                                                materialColors.secondaryFixed().getArgb(scheme),
                                            midLayer =
                                                materialColors.secondaryFixed().getArgb(scheme),
                                            topLayer = materialColors.primaryFixed().getArgb(scheme),
                                        )

                                    else ->
                                        VariantColors(
                                            background =
                                                materialColors.tertiaryFixed().getArgb(scheme),
                                            midLayer =
                                                materialColors.secondaryFixed().getArgb(scheme),
                                            topLayer = materialColors.primaryFixed().getArgb(scheme),
                                        )
                                }
                            }
                        val shader = RuntimeShader(CUSTOM_SHADER)
                        val shaderBrush = ShaderBrush(shader)
                        shader.setFloatUniform("resolution", size.width, size.height)
                        onDrawBehind {
                            colors?.let {
                                shader.setColorUniform(
                                    "backgroundColor",
                                    android.graphics.Color.valueOf(colors.background),
                                )
                                shader.setColorUniform(
                                    "squareColor",
                                    android.graphics.Color.valueOf(colors.midLayer),
                                )
                                shader.setColorUniform(
                                    "circleColor",
                                    android.graphics.Color.valueOf(colors.topLayer),
                                )
                                drawRect(brush = shaderBrush)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = animatedAlpha },
                        text = stringResource(styleLabelRes(option.style)),
                        style =
                            if (isSelected) {
                                MaterialTheme.typography.labelSmallEmphasized
                            } else {
                                MaterialTheme.typography.labelSmall
                            },
                        color = { colorScheme.onSurface },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

data class StyleBounceable(@ThemeStyle.Type val style: Int) : OptionBounceable()

data class VariantColors(
    @ColorInt val background: Int,
    @ColorInt val midLayer: Int,
    @ColorInt val topLayer: Int,
)
