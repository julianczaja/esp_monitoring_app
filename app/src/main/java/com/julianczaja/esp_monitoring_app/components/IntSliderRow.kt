package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.julianczaja.esp_monitoring_app.presentation.theme.spacing
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntSliderRow(
    label: String,
    value: Int,
    steps: Int,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var sliderPosition by remember { mutableFloatStateOf(value.toFloat()) }

    LaunchedEffect(key1 = value) {
        sliderPosition = value.toFloat()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large, Alignment.CenterHorizontally)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(.3f)
        )
        Slider(
            modifier = Modifier.weight(.6f),
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { onValueChange(sliderPosition.roundToInt()) },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            interactionSource = interactionSource,
            thumb = {
                Label(
                    interactionSource = interactionSource,
                    label = {
                        PlainTooltip {
                            Text(
                                text = sliderPosition.roundToInt().toString(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                ) {
                    SliderDefaults.Thumb(interactionSource = interactionSource)
                }
            }
        )
        Text(
            text = value.toString(),
            modifier = Modifier.weight(.1f),
            textAlign = TextAlign.Center
        )
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun IntSliderRowPreview() {
    AppBackground {
        Column {
            IntSliderRow(
                label = "Label",
                value = 1,
                steps = 3,
                enabled = true,
                valueRange = -2f..2f,
                onValueChange = {}
            )
            IntSliderRow(
                label = "Label",
                value = 31,
                steps = 51,
                enabled = false,
                valueRange = 0f..50f,
                onValueChange = {}
            )
        }
    }
}
//endregion
