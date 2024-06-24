package com.julianczaja.esp_monitoring_app.domain.usecase

import com.julianczaja.esp_monitoring_app.domain.model.Photo
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import java.time.LocalDate
import javax.inject.Inject


class SelectOrDeselectAllPhotosByDateUseCase @Inject constructor() {

    /**
     * Handles the selection and deselection of photos based on the given date.
     *
     * This function performs the following actions based on the current selection state of photos:
     * - If no photos with the given date are selected, all photos with that date will be selected.
     * - If some photos with the given date are selected, all photos with that date will be selected.
     * - If all photos with the given date are selected, all photos with that date will be deselected.
     *
     * @param date The date used to filter photos for selection or deselection.
     */
    operator fun invoke(
        allSelectablePhotosWithDate: List<Selectable<Photo>>,
        allSelectedPhotos: List<Photo>,
        date: LocalDate
    ): List<Photo> {
        val shouldSelectAll = !allSelectablePhotosWithDate.all { it.isSelected }

        return when {
            shouldSelectAll -> {
                val selectedPhotosWithDifferentDate = allSelectedPhotos.filter { it.dateTime.toLocalDate() != date }
                selectedPhotosWithDifferentDate + allSelectablePhotosWithDate.map { it.item }
            }

            else -> {
                allSelectedPhotos.filter { it.dateTime.toLocalDate() != date }
            }
        }
    }
}
