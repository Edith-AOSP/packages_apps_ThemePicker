/*
 * Copyright (C) 2024 The Android Open Source Project
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
 *
 */
package com.android.customization.picker.color.data.repository

import android.content.theming.ThemeStyle
import com.android.customization.model.color.ColorOption
import com.android.customization.picker.color.shared.model.ColorTuning
import com.android.customization.picker.color.shared.model.ColorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracts access to application state related to functionality for selecting, picking, or setting
 * system color.
 */
interface ColorPickerRepository {
    /** List of wallpaper and preset color options on the device, categorized by Color Type */
    val colorOptions: Flow<List<Pair<ColorType, List<ColorOption>>>>

    /** The system selected color option from the generated list of color options */
    val selectedColorOption: Flow<ColorOption?>

    /** List of theme styles use to build color options, of the type [ThemeStyle] */
    val styleList: List<Int>

    /** The system selected theme style, used in the color seed and variant picker */
    val selectedStyle: Flow<Int?>

    val freeformColorHue: StateFlow<Float?>

    /** Relative palette intensity adjustments, read from the theme settings. */
    val colorTuning: Flow<ColorTuning>

    /** Whether the theme service can apply palette intensity adjustments. */
    val supportsColorTuning: Boolean

    /** Selects a color option and returns whether the operation was successful */
    suspend fun apply(colorOption: ColorOption): Boolean

    /** Selects a color option and style and returns whether the operation was successful */
    suspend fun apply(
        colorOption: ColorOption,
        @ThemeStyle.Type style: Int,
        colorTuning: ColorTuning? = null,
    ): Boolean

    fun saveFreeformColor(hue: Float)
}
