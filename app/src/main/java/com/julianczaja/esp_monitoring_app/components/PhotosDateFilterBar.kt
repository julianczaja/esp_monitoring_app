package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.data.utils.toMonthDayString
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate


@Composable
fun PhotosDateFilterBar(
    modifier: Modifier = Modifier,
    dates: ImmutableList<Selectable<LocalDate>>,
    highlightedDate: LocalDate? = null,
    onDateClicked: (Selectable<LocalDate>) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            items(dates) { selectableDate ->
                Card(
                    shape = RoundedCornerShape(MaterialTheme.shape.photoDateFilterCorners),
                    colors = CardDefaults.cardColors().copy(
                        containerColor = when (selectableDate.item) {
                            highlightedDate -> MaterialTheme.colorScheme.primary.copy(alpha = .5f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    onClick = { onDateClicked(selectableDate) },
                    border = BorderStroke(
                        width = if (selectableDate.isSelected) 2.dp else 1.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = selectableDate.item.toMonthDayString(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
        HorizontalDivider()
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun PhotosDateFilterBarPreview() {
    AppBackground(Modifier.height(100.dp)) {
        PhotosDateFilterBar(
            dates = persistentListOf(
                Selectable(LocalDate.of(2024, 4, 25), true),
                Selectable(LocalDate.of(2024, 4, 10), true),
                Selectable(LocalDate.of(2024, 1, 20), false),
            ),
            highlightedDate = LocalDate.of(2024, 4, 25),
            onDateClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotosDateFilterBarOverflowPreview() {
    AppBackground {
        PhotosDateFilterBar(
            dates = persistentListOf(
                Selectable(LocalDate.of(2024, 4, 25), true),
                Selectable(LocalDate.of(2024, 4, 14), true),
                Selectable(LocalDate.of(2024, 4, 13), false),
                Selectable(LocalDate.of(2024, 4, 12), false),
                Selectable(LocalDate.of(2024, 4, 11), false),
                Selectable(LocalDate.of(2024, 4, 10), false),
                Selectable(LocalDate.of(2024, 1, 20), false),
            ),
            highlightedDate = LocalDate.of(2024, 4, 25),
            onDateClicked = {}
        )
    }
}
//endregion
