package com.julianczaja.esp_monitoring_app.presentation.devicephotos.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.data.utils.toMonthDayString
import com.julianczaja.esp_monitoring_app.domain.model.PhotosFilterMode
import com.julianczaja.esp_monitoring_app.domain.model.Selectable
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import java.time.LocalDate

private const val FILTER_BAR_HEIGHT_DP = 50

@Composable
fun FilterBar(
    modifier: Modifier = Modifier,
    dates: List<Selectable<LocalDate>>,
    highlightedDate: LocalDate? = null,
    filterMode: PhotosFilterMode,
    onDateClicked: (Selectable<LocalDate>) -> Unit,
    onFilterModeClicked: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = filterMode.ordinal,
        pageCount = { PhotosFilterMode.entries.size }
    )

    LaunchedEffect(key1 = filterMode) {
        pagerState.animateScrollToPage(filterMode.ordinal)
    }

    Column {
        Row(
            modifier = modifier.height(FILTER_BAR_HEIGHT_DP.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
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
            VerticalDivider(Modifier.fillMaxHeight())
            HorizontalPager(
                modifier = Modifier
                    .clickable { onFilterModeClicked() }
                    .width(100.dp),
                state = pagerState,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = filterMode.labelId),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun FilterBarPreview() {
    AppBackground(Modifier.height((FILTER_BAR_HEIGHT_DP + 20).dp)) {
        FilterBar(
            dates = listOf(
                Selectable(LocalDate.of(2024, 4, 25), true),
                Selectable(LocalDate.of(2024, 4, 10), true),
                Selectable(LocalDate.of(2024, 1, 20), false),
            ),
            highlightedDate = LocalDate.of(2024, 4, 25),
            filterMode = PhotosFilterMode.ALL,
            onDateClicked = {},
            onFilterModeClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun FilterBarOverflowPreview() {
    AppBackground(Modifier.height((FILTER_BAR_HEIGHT_DP + 20).dp)) {
        FilterBar(
            dates = listOf(
                Selectable(LocalDate.of(2024, 4, 25), true),
                Selectable(LocalDate.of(2024, 4, 14), true),
                Selectable(LocalDate.of(2024, 4, 13), false),
                Selectable(LocalDate.of(2024, 4, 12), false),
                Selectable(LocalDate.of(2024, 4, 11), false),
                Selectable(LocalDate.of(2024, 4, 10), false),
                Selectable(LocalDate.of(2024, 1, 20), false),
            ),
            highlightedDate = LocalDate.of(2024, 4, 25),
            filterMode = PhotosFilterMode.SAVED_ONLY,
            onDateClicked = {},
            onFilterModeClicked = {}
        )
    }
}
//endregion
