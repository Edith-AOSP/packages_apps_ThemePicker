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
package com.android.customization.picker.color.shared.model

import android.provider.Settings
import org.json.JSONException
import org.json.JSONObject

/**
 * Relative intensity adjustments for the generated palette.
 *
 * Values are percentages with [NEUTRAL] as unchanged. They live in the same secure setting as the
 * seed and style, so the theme service can apply them whenever it regenerates an overlay.
 */
data class ColorTuning(
    val accentSaturation: Int = NEUTRAL,
    val backgroundSaturation: Int = NEUTRAL,
    val backgroundLightness: Int = NEUTRAL,
    val accurateShades: Boolean = true,
    val tintedText: Boolean = true,
) {
    val isNeutral: Boolean
        get() =
            accentSaturation == NEUTRAL &&
                backgroundSaturation == NEUTRAL &&
                backgroundLightness == NEUTRAL &&
                accurateShades &&
                tintedText

    fun withAccentSaturation(value: Int) = copy(accentSaturation = value.coerceIn(MIN, MAX))

    fun withBackgroundSaturation(value: Int) =
        copy(backgroundSaturation = value.coerceIn(MIN, MAX))

    fun withBackgroundLightness(value: Int) =
        copy(backgroundLightness = value.coerceIn(MIN, MAX))

    fun withAccurateShades(value: Boolean) = copy(accurateShades = value)

    fun withTintedText(value: Boolean) = copy(tintedText = value)

    companion object {
        const val NEUTRAL = 100
        const val MIN = 0
        const val MAX = 200

        const val KEY_ACCENT_SATURATION = "android.theme.customization.accent_saturation"
        const val KEY_BACKGROUND_SATURATION =
            "android.theme.customization.background_saturation"
        const val KEY_BACKGROUND_LIGHTNESS = "android.theme.customization.background_lightness"
        const val KEY_ACCURATE_SHADES = "android.theme.customization.accurate_shades"
        const val KEY_TINTED_TEXT = "android.theme.customization.tinted_text"

        val DEFAULT = ColorTuning()

        fun fromJson(jsonString: String?): ColorTuning {
            if (jsonString.isNullOrEmpty()) return DEFAULT
            return try {
                fromJson(JSONObject(jsonString))
            } catch (e: JSONException) {
                DEFAULT
            }
        }

        fun fromJson(json: JSONObject) =
            ColorTuning(
                accentSaturation =
                    json.optInt(KEY_ACCENT_SATURATION, NEUTRAL).coerceIn(MIN, MAX),
                backgroundSaturation =
                    json.optInt(KEY_BACKGROUND_SATURATION, NEUTRAL).coerceIn(MIN, MAX),
                backgroundLightness =
                    json.optInt(KEY_BACKGROUND_LIGHTNESS, NEUTRAL).coerceIn(MIN, MAX),
                accurateShades = json.optInt(KEY_ACCURATE_SHADES, 1) != 0,
                tintedText = json.optInt(KEY_TINTED_TEXT, 1) != 0,
            )

        /**
         * Merges the tuning values into [jsonString], preserving every other key. The theme
         * service and the wallpaper picker share this setting, so unrelated fields must survive.
         */
        fun writeTo(jsonString: String?, tuning: ColorTuning): String {
            val json =
                try {
                    if (jsonString.isNullOrEmpty()) JSONObject() else JSONObject(jsonString)
                } catch (e: JSONException) {
                    JSONObject()
                }
            json.put(KEY_ACCENT_SATURATION, tuning.accentSaturation)
            json.put(KEY_BACKGROUND_SATURATION, tuning.backgroundSaturation)
            json.put(KEY_BACKGROUND_LIGHTNESS, tuning.backgroundLightness)
            json.put(KEY_ACCURATE_SHADES, if (tuning.accurateShades) 1 else 0)
            json.put(KEY_TINTED_TEXT, if (tuning.tintedText) 1 else 0)
            return json.toString()
        }

        val SETTING = Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES
    }
}
