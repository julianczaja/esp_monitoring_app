package com.julianczaja.esp_monitoring_app.domain.model

import androidx.annotation.StringRes
import com.julianczaja.esp_monitoring_app.R

enum class PhotosFilterMode(@StringRes val labelId: Int) {
    SAVED_ONLY(R.string.filter_saved_only_label),
    SERVER_ONLY(R.string.filter_server_only_label),
    ALL(R.string.filter_all_label)
}
