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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.bounceable
import com.android.compose.theme.PlatformTheme
import com.android.customization.model.color.ColorOption
import com.android.customization.picker.color.shared.model.ColorTuning
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.color.ui.viewmodel.ColorOptionViewModel
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.customization.picker.mode.ui.viewmodel.DarkModeViewModel
import com.android.systemui.monet.ColorScheme
import com.android.themepicker.R
import kotlin.math.ceil
import kotlin.math.roundToInt
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Top-level pages in the Colors floating sheet. */
enum class ColorSheetTab {
    WALLPAPER_COLORS,
    OTHER_COLORS,
    STYLES,
    TUNE,
    ADVANCED,
}

@Composable
fun ColorFloatingSheet(
    darkModeViewModel: DarkModeViewModel,
    colorPickerViewModel: ColorPickerViewModel,
    modifier: Modifier = Modifier,
) {
    val previewingColorOption: ColorOption? by
        colorPickerViewModel.tempPreviewingColorOption.collectAsStateWithLifecycle(
            initialValue = null
        )
    val previewingIsDarkMode: Boolean by
        darkModeViewModel.previewingIsDarkMode.collectAsStateWithLifecycle(initialValue = false)
    val screen: ColorPickerViewModel.Screen by
        colorPickerViewModel.currentScreen.collectAsStateWithLifecycle()
    val toggleIsDarkMode: () -> Unit by
        darkModeViewModel.toggleDarkMode.collectAsStateWithLifecycle(initialValue = {})
    val isDarkModeToggleEnabled: Boolean by
        darkModeViewModel.isEnabled.collectAsStateWithLifecycle(initialValue = false)
    val colorSeedOptions: List<Pair<ColorType, List<ColorOptionViewModel>>> by
        colorPickerViewModel.colorSeedOptions.collectAsStateWithLifecycle(
            initialValue = emptyList()
        )
    val previewingColorOptionKey: String? by
        colorPickerViewModel.previewingColorOptionKey.collectAsStateWithLifecycle(
            initialValue = null
        )
    val previewingStyle: Int? by
        colorPickerViewModel.previewingStyle.collectAsStateWithLifecycle(initialValue = null)
    val styleOptions = colorPickerViewModel.styleOptions.map { StyleBounceable(it) }
    val hueSliderPosition: Float by
        colorPickerViewModel.hueSliderPosition.collectAsStateWithLifecycle(
            initialValue = ColorPickerViewModel.HUE_INIT_VALUE
        )
    val previewingColorTuning: ColorTuning by
        colorPickerViewModel.previewingColorTuning.collectAsStateWithLifecycle(
            initialValue = ColorTuning.DEFAULT
        )
    val supportsColorTuning = colorPickerViewModel.supportsColorTuning

    PlatformTheme {
        val scheme =
            remember(
                previewingColorOption,
                previewingIsDarkMode,
                previewingStyle,
                previewingColorTuning,
            ) {
                previewingColorOption?.let {
                    ColorScheme(
                            it.seedColor,
                            previewingIsDarkMode,
                            previewingStyle ?: it.style,
                            previewingColorTuning.accentSaturation,
                            previewingColorTuning.backgroundSaturation,
                            previewingColorTuning.backgroundLightness,
                            previewingColorTuning.accurateShades,
                        )
                        .materialScheme
                }
            }

        ColorPreviewTheme(scheme) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { value ->
                when (value) {
                    ColorPickerViewModel.Screen.LANDING ->
                        ColorFloatingSheetLanding(
                            isDarkMode = previewingIsDarkMode,
                            toggleIsDarkMode = toggleIsDarkMode,
                            isDarkModeToggleEnabled = isDarkModeToggleEnabled,
                            colorSeedOptions = colorSeedOptions,
                            selectedColorSeedKey = previewingColorOptionKey,
                            styleOptions = styleOptions,
                            selectedStyle = previewingStyle,
                            previewingSeedColor = previewingColorOption?.seedColor,
                            onStyleClick = colorPickerViewModel::selectStyle,
                            previewingColorTuning = previewingColorTuning,
                            supportsColorTuning = supportsColorTuning,
                            onColorTuningChange = colorPickerViewModel::updateColorTuning,
                            navigateToFreeformPicker = {
                                colorPickerViewModel.setScreen(
                                    ColorPickerViewModel.Screen.FREEFORM_PICKER
                                )
                            },
                            modifier = modifier,
                        )
                    ColorPickerViewModel.Screen.VARIANT_PICKER ->
                        ColorVariantPicker(
                            styleOptions = styleOptions,
                            selectedOption = previewingStyle,
                            previewingSeedColor = previewingColorOption?.seedColor,
                            previewingIsDarkMode = previewingIsDarkMode,
                            onClick = colorPickerViewModel::onStyleOptionClick,
                            onCancel = colorPickerViewModel::cancelStyleOptionSelection,
                            onConfirm = colorPickerViewModel::confirmStyleOptionSelection,
                            navigateToLanding = {
                                colorPickerViewModel.setScreen(ColorPickerViewModel.Screen.LANDING)
                            },
                            modifier = modifier,
                        )
                    ColorPickerViewModel.Screen.FREEFORM_PICKER ->
                        FreeformColorPicker(
                            hueSliderPosition = hueSliderPosition,
                            onHueChange = colorPickerViewModel::updateHue,
                            onCancel = colorPickerViewModel::cancelFreeformColor,
                            onConfirm = colorPickerViewModel::confirmFreeformColor,
                            navigateToLanding = {
                                colorPickerViewModel.setScreen(ColorPickerViewModel.Screen.LANDING)
                            },
                            modifier = modifier,
                        )
                }
            }
        }
    }
}

@Composable
fun ColorFloatingSheetLanding(
    isDarkMode: Boolean,
    toggleIsDarkMode: () -> Unit,
    isDarkModeToggleEnabled: Boolean,
    colorSeedOptions: List<Pair<ColorType, List<ColorOptionViewModel>>>,
    selectedColorSeedKey: String?,
    styleOptions: List<StyleBounceable>,
    selectedStyle: Int?,
    previewingSeedColor: Int?,
    onStyleClick: (Int) -> Unit,
    previewingColorTuning: ColorTuning,
    supportsColorTuning: Boolean,
    onColorTuningChange: (ColorTuning) -> Unit,
    navigateToFreeformPicker: () -> Unit,
    modifier: Modifier,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    val lazyListState: LazyListState = rememberLazyListState()
    var selectedTab by remember { mutableStateOf(ColorSheetTab.WALLPAPER_COLORS) }
    val filteredColorSeedOptions =
        when (selectedTab) {
            ColorSheetTab.WALLPAPER_COLORS ->
                colorSeedOptions.filter { it.first == ColorType.WALLPAPER_COLOR }
            ColorSheetTab.OTHER_COLORS ->
                colorSeedOptions.filter {
                    it.first == ColorType.PRESET_COLOR || it.first == ColorType.FREEFORM_COLOR
                }
            ColorSheetTab.STYLES, ColorSheetTab.TUNE, ColorSheetTab.ADVANCED -> emptyList()
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 16.dp)
                .clip(shape = RoundedCornerShape(28.dp))
                .drawBehind { drawRect(colorScheme.surfaceBright) }
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.system_colors_title),
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = toggleIsDarkMode,
                        enabled = isDarkModeToggleEnabled,
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = colorScheme.surfaceDim,
                                contentColor = colorScheme.onSurface,
                            ),
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter =
                                painterResource(
                                    if (isDarkMode) {
                                        R.drawable.ic_light_mode_24
                                    } else {
                                        R.drawable.ic_dark_mode_24
                                    }
                                ),
                            contentDescription =
                                stringResource(
                                    if (isDarkMode) {
                                        R.string.switch_to_light_theme
                                    } else {
                                        R.string.switch_to_dark_theme
                                    }
                                ),
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = navigateToFreeformPicker,
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = colorScheme.surfaceDim,
                                contentColor = colorScheme.onSurface,
                            ),
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.ic_palette_filled_24px),
                            contentDescription = "",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab != ColorSheetTab.ADVANCED) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    text =
                        stringResource(
                            when (selectedTab) {
                                ColorSheetTab.WALLPAPER_COLORS ->
                                    R.string.wallpaper_color_subheader
                                ColorSheetTab.OTHER_COLORS -> R.string.preset_color_subheader
                                ColorSheetTab.STYLES -> R.string.color_style_subheader
                                ColorSheetTab.TUNE -> R.string.color_tune_subheader
                                ColorSheetTab.ADVANCED -> R.string.color_advanced_subheader
                            }
                        ),
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (selectedTab == ColorSheetTab.STYLES) {
                StyleRow(
                    styleOptions = styleOptions,
                    selectedStyle = selectedStyle,
                    previewingSeedColor = previewingSeedColor,
                    previewingColorTuning = previewingColorTuning,
                    previewingIsDarkMode = isDarkMode,
                    onStyleClick = onStyleClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (selectedTab == ColorSheetTab.TUNE) {
                ColorTuningPanel(
                    colorTuning = previewingColorTuning,
                    onColorTuningChange = onColorTuningChange,
                )
            } else if (selectedTab == ColorSheetTab.ADVANCED) {
                ColorAdvancedPanel(
                    colorTuning = previewingColorTuning,
                    onColorTuningChange = onColorTuningChange,
                )
            } else {
                // TODO (b/441279631): implement scroll to selected color option
                LazyRow(
                    state = lazyListState,
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier =
                        Modifier.semantics { testTagsAsResourceId = true }
                            .testTag(
                                "com.google.android.apps.wallpaper:id/colors_horizontal_list"
                            ),
                ) {
                    filteredColorSeedOptions.forEachIndexed {
                        colorTypeIdx,
                        colorTypeToOptions ->
                        val colorList = colorTypeToOptions.second
                        if (colorTypeIdx != 0 && colorList.isNotEmpty()) {
                            item { OptionListGroupDivider() }
                        }
                        itemsIndexed(colorList) { idx, option ->
                            ColorSeedOption(
                                isDarkMode = isDarkMode,
                                optionItem = option,
                                isSelected = option.key == selectedColorSeedKey,
                                modifier =
                                    Modifier.size(
                                            dimensionResource(
                                                R.dimen.floating_sheet_color_option_size
                                            )
                                        )
                                        .bounceable(
                                            bounceable = colorList[idx],
                                            previousBounceable =
                                                if (idx > 0) colorList[idx - 1] else null,
                                            nextBounceable =
                                                if (idx < colorList.lastIndex) colorList[idx + 1]
                                                else null,
                                            orientation = Horizontal,
                                        ),
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    ColorSheetTabRow(
        selectedTab = selectedTab,
        showTune = supportsColorTuning,
        onTabSelected = { selectedTab = it },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    }
}

@Composable
private fun ColorSheetTabRow(
    selectedTab: ColorSheetTab,
    showTune: Boolean,
    onTabSelected: (ColorSheetTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = LocalAnimatedColorScheme.current
    val tabs =
        buildList {
            add(
                ColorToolbarTab(
                    tab = ColorSheetTab.WALLPAPER_COLORS,
                    labelRes = R.string.wallpaper_color_tab,
                    iconRes = com.android.wallpaper.R.drawable.ic_baseline_wallpaper_24,
                )
            )
            add(
                ColorToolbarTab(
                    tab = ColorSheetTab.OTHER_COLORS,
                    labelRes = R.string.preset_color_tab_2,
                    iconRes = R.drawable.ic_colors,
                )
            )
            add(
                ColorToolbarTab(
                    tab = ColorSheetTab.STYLES,
                    labelRes = R.string.color_style_tab,
                    iconRes = R.drawable.ic_palette_filled_24px,
                )
            )
            if (showTune) {
                add(
                    ColorToolbarTab(
                        tab = ColorSheetTab.TUNE,
                        labelRes = R.string.color_tune_tab,
                        iconRes = com.android.wallpaper.R.drawable.ic_tune_black_24dp,
                    )
                )
                add(
                    ColorToolbarTab(
                        tab = ColorSheetTab.ADVANCED,
                        labelRes = R.string.color_advanced_tab,
                        iconRes = R.drawable.ic_settings,
                    )
                )
            }
        }
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val scrollState = rememberScrollState()
        Row(
            modifier =
                Modifier.clip(RoundedCornerShape(50))
                    .background(colorScheme.surfaceBright)
                    .widthIn(max = maxWidth)
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
        tabs.forEach { item ->
            val selected = selectedTab == item.tab
            val isCompact = tabs.size >= 3
            val showIcon = selected || isCompact
            val showText = selected || !isCompact
            val containerColor by
                animateColorAsState(
                    targetValue =
                        if (selected) colorScheme.secondaryContainer else Color.Transparent,
                    animationSpec = tween(durationMillis = 220),
                    label = "colorSheetTabContainer",
                )
            val contentColor by
                animateColorAsState(
                    targetValue =
                        if (selected) colorScheme.onSecondaryContainer
                        else colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 220),
                    label = "colorSheetTabContent",
                )
            Row(
                modifier =
                    Modifier.heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(containerColor)
                        .clickable { onTabSelected(item.tab) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = showIcon,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription =
                            if (showText) null else stringResource(item.labelRes),
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                AnimatedVisibility(
                    visible = showText,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Text(
                        modifier = Modifier.padding(start = if (showIcon) 8.dp else 0.dp),
                        text = stringResource(item.labelRes),
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ColorAdvancedPanel(
    colorTuning: ColorTuning,
    onColorTuningChange: (ColorTuning) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val json =
                            Settings.Secure.getString(
                                context.contentResolver,
                                ColorTuning.SETTING,
                            ) ?: "{}"
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(json.toByteArray())
                        }
                    }
                }
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val text =
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                                it.readText()
                            }
                        if (
                            !text.isNullOrEmpty() &&
                                runCatching { JSONObject(text) }.isSuccess
                        ) {
                            Settings.Secure.putString(
                                context.contentResolver,
                                ColorTuning.SETTING,
                                text,
                            )
                        }
                    }
                }
            }
        }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        AdvancedSwitch(
            title = stringResource(R.string.color_accurate_shades_title),
            description = stringResource(R.string.color_accurate_shades_description),
            checked = colorTuning.accurateShades,
            onCheckedChange = {
                onColorTuningChange(colorTuning.withAccurateShades(it))
            },
        )
        AdvancedSwitch(
            title = stringResource(R.string.color_tinted_text_title),
            description = stringResource(R.string.color_tinted_text_description),
            checked = colorTuning.tintedText,
            onCheckedChange = {
                onColorTuningChange(colorTuning.withTintedText(it))
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = { exportLauncher.launch("colors.json") },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.color_advanced_backup))
            }
            FilledTonalButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.color_advanced_restore))
            }
        }
    }
}

@Composable
private fun AdvancedSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = description,
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = {
                Icon(
                    painter =
                        painterResource(
                            if (checked) {
                                com.android.wallpaper.R.drawable.ic_check_wallpaper
                            } else {
                                com.android.wallpaper.R.drawable.ic_close
                            }
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )
    }
}

private enum class TuningSection {
    ACCENT,
    BACKGROUND,
}

@Composable
private fun ColorTuningPanel(
    colorTuning: ColorTuning,
    onColorTuningChange: (ColorTuning) -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by remember { mutableStateOf(TuningSection.ACCENT) }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TuningSectionTab(
                text = stringResource(R.string.color_tuning_accent),
                selected = section == TuningSection.ACCENT,
                onClick = { section = TuningSection.ACCENT },
                modifier = Modifier.weight(1f),
            )
            TuningSectionTab(
                text = stringResource(R.string.color_tuning_background),
                selected = section == TuningSection.BACKGROUND,
                onClick = { section = TuningSection.BACKGROUND },
                modifier = Modifier.weight(1f),
            )
        }
        when (section) {
            TuningSection.ACCENT ->
                TuningRow(
                    title = stringResource(R.string.color_tuning_saturation),
                    value = colorTuning.accentSaturation,
                    onValueChange = {
                        onColorTuningChange(colorTuning.withAccentSaturation(it))
                    },
                )
            TuningSection.BACKGROUND -> {
                TuningRow(
                    title = stringResource(R.string.color_tuning_saturation),
                    value = colorTuning.backgroundSaturation,
                    onValueChange = {
                        onColorTuningChange(colorTuning.withBackgroundSaturation(it))
                    },
                )
                TuningRow(
                    title = stringResource(R.string.color_tuning_lightness),
                    value = colorTuning.backgroundLightness,
                    onValueChange = {
                        onColorTuningChange(colorTuning.withBackgroundLightness(it))
                    },
                )
            }
        }
    }
}

@Composable
private fun TuningSectionTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    val cornerRadius by
        animateDpAsState(
            targetValue = if (selected) 12.dp else 20.dp,
            animationSpec = tween(durationMillis = 300),
            label = "tuningSectionCorner",
        )
    val containerColor by
        animateColorAsState(
            targetValue =
                if (selected) colorScheme.secondaryContainer else colorScheme.surfaceDim,
            animationSpec = tween(durationMillis = 220),
            label = "tuningSectionContainer",
        )
    Box(
        modifier =
            modifier
                .height(40.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(containerColor)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color =
                if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
            style =
                if (selected) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            maxLines = 1,
        )
    }
}

@Composable
private fun TuningRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(100.dp)) {
            Text(
                text = title,
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = ColorTuning.MIN.toFloat()..ColorTuning.MAX.toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        Text(
            text = formatTuningValue(value),
            color = colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(36.dp),
        )
        IconButton(
            modifier = Modifier.size(40.dp),
            enabled = value != ColorTuning.NEUTRAL,
            onClick = {
                onValueChange(ColorTuning.NEUTRAL)
                scope.launch {
                    rotation.snapTo(0f)
                    rotation.animateTo(360f, animationSpec = tween(durationMillis = 350))
                }
            },
        ) {
            Icon(
                modifier = Modifier.size(18.dp).rotate(rotation.value),
                painter = painterResource(com.android.wallpaper.R.drawable.ic_refresh_18px),
                contentDescription = stringResource(R.string.color_tuning_reset),
            )
        }
    }
}

private fun formatTuningValue(value: Int): String =
    if (value % 100 == 0) {
        "${value / 100}x"
    } else {
        String.format(java.util.Locale.US, "%.1fx", value / 100f)
    }

private data class ColorToolbarTab(
    val tab: ColorSheetTab,
    val labelRes: Int,
    val iconRes: Int,
)

@Composable
private fun StyleRow(
    styleOptions: List<StyleBounceable>,
    selectedStyle: Int?,
    previewingSeedColor: Int?,
    previewingColorTuning: ColorTuning,
    previewingIsDarkMode: Boolean,
    onStyleClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(styleOptions) { index, option ->
            StyleOptionCard(
                style = option.style,
                selected = option.style == selectedStyle,
                previewingSeedColor = previewingSeedColor,
                previewingColorTuning = previewingColorTuning,
                previewingIsDarkMode = previewingIsDarkMode,
                onClick = {
                    onStyleClick(option.style)
                    coroutineScope.launch { option.clickBounceAnimate() }
                },
                modifier =
                    Modifier.width(76.dp)
                        .bounceable(
                            bounceable = option,
                            previousBounceable = styleOptions.getOrNull(index - 1),
                            nextBounceable = styleOptions.getOrNull(index + 1),
                            orientation = Horizontal,
                        ),
            )
        }
    }
}

@Composable
fun OptionListGroupDivider(modifier: Modifier = Modifier) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    Box(
        modifier =
            modifier
                .width(12.dp)
                .height(28.dp)
                .padding(horizontal = 5.dp)
                .drawBehind {
                    drawRoundRect(
                        color = colorScheme.onSurfaceVariant,
                        cornerRadius = CornerRadius(x = 1.dp.toPx(), y = 1.dp.toPx()),
                    )
                }
                .semantics { hideFromAccessibility() }
    )
}

@Composable
fun ColorSeedOption(
    isDarkMode: Boolean,
    optionItem: ColorOptionViewModel,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val colorIcon: ColorOptionIconViewModel = optionItem.icon

    Box(modifier = modifier) {
        ColorOption(
            isSelected = isSelected,
            onClick = {
                if (!isSelected) {
                    optionItem.onClick?.invoke()
                    coroutineScope.launch { optionItem.clickBounceAnimate() }
                }
            },
        ) {
            // Round up width to prevent empty pixels between quadrants in bounce
            // animation.
            val quadrantSize = Size(ceil(size.width / 2f), size.height / 2f)
            onDrawBehind {
                colorIcon?.let {
                    drawRect(
                        color =
                            if (isDarkMode) {
                                Color(it.darkThemeColor0)
                            } else {
                                Color(it.lightThemeColor0)
                            },
                        size = quadrantSize,
                    )
                    drawRect(
                        color =
                            if (isDarkMode) {
                                Color(it.darkThemeColor1)
                            } else {
                                Color(it.lightThemeColor1)
                            },
                        topLeft = Offset(x = size.width / 2f, y = 0f),
                        size = quadrantSize,
                    )
                    drawRect(
                        color =
                            if (isDarkMode) {
                                Color(it.darkThemeColor2)
                            } else {
                                Color(it.lightThemeColor2)
                            },
                        topLeft = Offset(x = 0f, y = size.height / 2f),
                        size = quadrantSize,
                    )
                    drawRect(
                        color =
                            if (isDarkMode) {
                                Color(it.darkThemeColor3)
                            } else {
                                Color(it.lightThemeColor3)
                            },
                        topLeft = Offset(x = size.width / 2f, y = size.height / 2f),
                        size = quadrantSize,
                    )
                }
            }
        }

    }
}
