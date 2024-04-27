package com.julianczaja.esp_monitoring_app.presentation.devicesavedphotos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.julianczaja.esp_monitoring_app.R


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerScope.DeviceSavedScreen() {
    DeviceSavedScreenContent()
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerScope.DeviceSavedScreenContent() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(id = R.string.work_in_progress),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
