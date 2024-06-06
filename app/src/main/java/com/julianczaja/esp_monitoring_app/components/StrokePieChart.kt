package com.julianczaja.esp_monitoring_app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.julianczaja.esp_monitoring_app.data.utils.getClampedPercent


@Composable
fun StrokePieChart(
    modifier: Modifier = Modifier,
    value: Float,
    maxValue: Float,
    text: String? = null,
    strokeWidthDp: Int = 20,
    baseColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.inversePrimary
) {
    val fillPercent = getClampedPercent(value, maxValue)
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onBackground
    val textStyle = MaterialTheme.typography.bodyMedium.copy(textColor, textAlign = TextAlign.Center)
    val textLayoutResult = remember(fillPercent, text) {
        text?.let {
            textMeasurer.measure(text = text, style = textStyle)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val strokeWidth = strokeWidthDp.dp.toPx()

        drawArc(
            color = backgroundColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(width - strokeWidth, width - strokeWidth),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = baseColor,
            startAngle = -90f,
            sweepAngle = fillPercent * 3.6f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(width - strokeWidth, width - strokeWidth),
            style = Stroke(strokeWidth, cap = StrokeCap.Butt)
        )
        if (!text.isNullOrEmpty() && textLayoutResult != null) {
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = textStyle,
                topLeft = Offset(
                    x = center.x - textLayoutResult.size.width / 2,
                    y = center.y - textLayoutResult.size.height / 2,
                )
            )
        }
    }
}

//region Preview
@PreviewLightDark
@Composable
private fun StrokePieChartWithTextPreview() {
    AppBackground {
        StrokePieChart(
            modifier = Modifier.size(200.dp),
            value = 64.5f,
            maxValue = 100.0f,
            text = "Used 64.5%"
        )
    }
}

@PreviewLightDark
@Composable
private fun StrokePieChartWithoutTextPreview() {
    AppBackground {
        StrokePieChart(
            modifier = Modifier.size(200.dp),
            value = 14.5f,
            maxValue = 100.0f,
        )
    }
}
//endregion