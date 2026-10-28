/*
 * Copyright (C) 2026 The Android Open Source Project
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.customization.picker.color.shared.model.ColorTuning
import com.android.customization.picker.color.ui.compose.Shader.CUSTOM_SHADER
import com.android.systemui.monet.ColorScheme
import com.android.themepicker.R
import com.google.ux.material.libmonet.dynamiccolor.DynamicScheme
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors

/** A style preview card used by both the Styles tab and the style drill-down picker. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StyleOptionCard(
    @ThemeStyle.Type style: Int,
    selected: Boolean,
    previewingSeedColor: Int?,
    previewingColorTuning: ColorTuning,
    previewingIsDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = LocalAnimatedColorScheme.current
    val animatedAlpha: Float by animateFloatAsState(if (selected) 1f else 0.25f)
    val scheme =
        remember(previewingSeedColor, previewingIsDarkMode, style, previewingColorTuning) {
            previewingSeedColor?.let {
                ColorScheme(
                        it,
                        previewingIsDarkMode,
                        style,
                        previewingColorTuning.accentSaturation,
                        previewingColorTuning.backgroundSaturation,
                        previewingColorTuning.backgroundLightness,
                    )
                    .materialScheme
            }
        }
    Column(modifier = modifier) {
        ColorOption(
            modifier = Modifier.fillMaxWidth().height(68.dp),
            isSelected = selected,
            onClick = onClick,
        ) {
            val colors = scheme?.let { styleColors(it, style) }
            val shader = RuntimeShader(CUSTOM_SHADER)
            val shaderBrush = ShaderBrush(shader)
            shader.setFloatUniform("resolution", size.width, size.height)
            onDrawBehind {
                colors?.let {
                    shader.setColorUniform(
                        "backgroundColor",
                        android.graphics.Color.valueOf(it.background),
                    )
                    shader.setColorUniform(
                        "squareColor",
                        android.graphics.Color.valueOf(it.midLayer),
                    )
                    shader.setColorUniform(
                        "circleColor",
                        android.graphics.Color.valueOf(it.topLayer),
                    )
                    drawRect(brush = shaderBrush)
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = animatedAlpha },
            text = stringResource(styleLabelRes(style)),
            style =
                if (selected) {
                    MaterialTheme.typography.labelSmallEmphasized
                } else {
                    MaterialTheme.typography.labelSmall
                },
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun styleLabelRes(@ThemeStyle.Type style: Int): Int =
    when (style) {
        ThemeStyle.SPRITZ -> R.string.color_style_neutral
        ThemeStyle.MONOCHROMATIC -> R.string.color_style_monochrome
        ThemeStyle.TONAL_SPOT -> R.string.color_style_tonal_spot
        ThemeStyle.VIBRANT -> R.string.color_style_vibrant
        ThemeStyle.EXPRESSIVE -> R.string.color_style_expressive
        ThemeStyle.RAINBOW -> R.string.color_style_rainbow
        ThemeStyle.FRUIT_SALAD -> R.string.color_style_fruit_salad
        ThemeStyle.CONTENT -> R.string.color_style_content
        else -> R.string.color_style_cmf
    }

private fun styleColors(scheme: DynamicScheme, @ThemeStyle.Type style: Int): VariantColors {
    val materialColors = MaterialDynamicColors()
    return if (style == ThemeStyle.SPRITZ) {
        VariantColors(
            background = materialColors.secondaryFixed().getArgb(scheme),
            midLayer = materialColors.secondaryFixed().getArgb(scheme),
            topLayer = materialColors.primaryFixed().getArgb(scheme),
        )
    } else {
        VariantColors(
            background = materialColors.tertiaryFixed().getArgb(scheme),
            midLayer = materialColors.secondaryFixed().getArgb(scheme),
            topLayer = materialColors.primaryFixed().getArgb(scheme),
        )
    }
}
