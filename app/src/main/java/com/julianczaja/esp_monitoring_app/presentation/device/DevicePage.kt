package com.julianczaja.esp_monitoring_app.presentation.device

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.julianczaja.esp_monitoring_app.R

enum class DevicePage(
    val index: Int,
    @StringRes val titleId: Int,
    @DrawableRes val drawableId: Int,
) {
    Photos(0, R.string.photos_tab_label, R.drawable.ic_baseline_photo_24),
    Saved(1, R.string.saved_tab_label, R.drawable.ic_baseline_bookmarks_24),
    Settings(2, R.string.settings_tab_label, R.drawable.ic_baseline_settings_24)
}
