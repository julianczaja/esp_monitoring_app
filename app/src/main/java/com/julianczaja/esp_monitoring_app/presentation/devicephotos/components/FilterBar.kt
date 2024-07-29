package com.julianczaja.esp_monitoring_app.presentation.devicephotos.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.components.AppBackground
import com.julianczaja.esp_monitoring_app.data.utils.toMonthDayString
import com.julianczaja.esp_monitoring_app.domain.model.Day
import com.julianczaja.esp_monitoring_app.domain.model.PhotosFilterMode
import com.julianczaja.esp_monitoring_app.presentation.theme.shape
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import java.time.LocalDate

private const val FILTER_BAR_HEIGHT_DP = 50
private const val ANIMATION_LENGTH_MS = 300

@Composable
fun FilterBar(
    modifier: Modifier = Modifier,
    days: ImmutableList<Day>,
    filterMode: PhotosFilterMode,
    currentDayIndex: Int = 0,
    onDayClicked: (Day) -> Unit,
    onFilterModeClicked: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = filterMode.ordinal,
        pageCount = { PhotosFilterMode.entries.size }
    )
    val lazyRowState = rememberLazyListState()

    LaunchedEffect(key1 = filterMode) {
        pagerState.animateScrollToPage(filterMode.ordinal)
    }

    LaunchedEffect(key1 = currentDayIndex) {
        if (days.isNotEmpty()) {
            delay(ANIMATION_LENGTH_MS.toLong())
            lazyRowState.animateScrollToItem(currentDayIndex, -50)
        }
    }

    Column {
        Row(
            modifier = modifier.height(FILTER_BAR_HEIGHT_DP.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                state = lazyRowState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                itemsIndexed(days) { index, day ->
                    val padding by animateDpAsState(
                        targetValue = when {
                            index == currentDayIndex -> MaterialTheme.spacing.medium
                            else -> 0.dp
                        },
                        label = "weight",
                        animationSpec = tween(ANIMATION_LENGTH_MS, easing = LinearEasing)
                    )
                    val color by animateColorAsState(
                        targetValue = when {
                            index == currentDayIndex -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        label = "color",
                        animationSpec = tween(2 * ANIMATION_LENGTH_MS, easing = LinearEasing)
                    )
                    Card(
                        shape = RoundedCornerShape(MaterialTheme.shape.photoDateFilterCorners),
                        colors = CardDefaults.cardColors().copy(containerColor = color),
                        onClick = { onDayClicked(day) },
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = padding),
                                text = day.date.toMonthDayString(),
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
            currentDayIndex = 1,
            days = persistentListOf(
                Day(1L, LocalDate.of(2024, 4, 25)),
                Day(1L, LocalDate.of(2024, 4, 10)),
                Day(1L, LocalDate.of(2024, 1, 20)),
            ),
            filterMode = PhotosFilterMode.ALL,
            onDayClicked = {},
            onFilterModeClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun FilterBarOverflowPreview() {
    AppBackground(Modifier.height((FILTER_BAR_HEIGHT_DP + 20).dp)) {
        FilterBar(
            currentDayIndex = 1,
            days = persistentListOf(
                Day(1L, LocalDate.of(2024, 4, 25)),
                Day(1L, LocalDate.of(2024, 4, 14)),
                Day(1L, LocalDate.of(2024, 4, 13)),
                Day(1L, LocalDate.of(2024, 4, 12)),
                Day(1L, LocalDate.of(2024, 4, 11)),
                Day(1L, LocalDate.of(2024, 4, 10)),
                Day(1L, LocalDate.of(2024, 1, 20)),
            ),
            filterMode = PhotosFilterMode.SAVED_ONLY,
            onDayClicked = {},
            onFilterModeClicked = {}
        )
    }
}
//endregion
