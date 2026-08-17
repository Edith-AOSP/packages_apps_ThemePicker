package com.android.wallpaper.customization.ui.viewmodel

import android.content.Context
import android.graphics.drawable.Drawable
import com.android.customization.picker.iconpack.IconPackInfo
import com.android.customization.picker.iconpack.domain.interactor.IconPackInteractor
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class IconPackPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext private val appContext: Context,
    private val interactor: IconPackInteractor,
    @Assisted private val viewModelScope: CoroutineScope,
) {

    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): IconPackPickerViewModel
    }

    private val _overridingIconPack = MutableStateFlow<String?>(null)
    private val overridingIconPack: StateFlow<String?> = _overridingIconPack

    /** Emits the user's active icon pack override, or null if no override is active. */
    val iconPackOverride: StateFlow<String?> = _overridingIconPack

    private var savedIconPack: String? =
        runBlocking { interactor.selectedIconPack.first() ?: "" }

    val previewingIconPack: Flow<String?> =
        combine(interactor.selectedIconPack, overridingIconPack) { current, override ->
            override ?: current
        }

    val packOptions: Flow<List<OptionItemViewModel2<IconPackInfo>>> =
        combine(previewingIconPack, flowOf(interactor.installedIconPacks)) { previewingPkg, packs ->
            packs.map { pack ->
                val isPreviewed = pack.packageName == previewingPkg
                OptionItemViewModel2(
                    key = MutableStateFlow(pack.packageName.ifEmpty { "system" }),
                    payload = pack,
                    text = Text.Loaded(pack.name),
                    isSelected = MutableStateFlow(isPreviewed),
                    onClicked = if (isPreviewed) {
                        MutableStateFlow(null)
                    } else {
                        MutableStateFlow({
                            _overridingIconPack.value = pack.packageName
                        } as (() -> Unit)?)
                    },
                )
            }
        }

    val onApply: Flow<(suspend () -> Unit)?> =
        overridingIconPack.map { override ->
            if (override != null && override != savedIconPack) {
                suspend {
                    interactor.setIconPack(override)
                    savedIconPack = override
                    _overridingIconPack.value = null
                }
            } else null
        }

    /** Icon of the currently previewing icon pack, used for the Icons home entry. */
    val entryIcon: Flow<Drawable?> =
        previewingIconPack.map { pkg ->
            val defaultIcon = appContext.packageManager.getApplicationIcon(appContext.packageName)
            if (pkg.isNullOrEmpty()) {
                defaultIcon
            } else {
                interactor.installedIconPacks.firstOrNull { it.packageName == pkg }?.icon
                    ?: defaultIcon
            }
        }

    /** Display name of the currently previewing icon pack, used for the Icons home entry. */
    val previewingPackName: Flow<String> =
        previewingIconPack.map { pkg ->
            if (pkg.isNullOrEmpty()) {
                SYSTEM_ICONS_NAME
            } else {
                interactor.installedIconPacks
                    .firstOrNull { it.packageName == pkg }?.name ?: pkg
            }
        }

    fun resetPreview() {
        _overridingIconPack.value = null
    }

    companion object {
        private const val SYSTEM_ICONS_NAME = "System Icons"
    }
}
