/*
 * Copyright 2023 Blocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.merxury.blocker.core.designsystem.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun BlockerErrorAlertDialog(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState(0)
    AlertDialog(
        modifier = modifier,
        title = {
            Text(text = title)
        },
        text = {
            Text(
                modifier = Modifier.verticalScroll(scrollState),
                text = text,
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            BlockerTextButton(
                onClick = onDismissRequest,
            ) {
                Text(
                    text = stringResource(id = android.R.string.ok),
                )
            }
        },
    )
}

@Composable
fun BlockerWarningAlertDialog(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState(0)
    AlertDialog(
        modifier = modifier,
        title = {
            Text(
                modifier = Modifier,
                text = title,
            )
        },
        text = {
            Text(
                modifier = Modifier.verticalScroll(scrollState),
                text = text,
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            BlockerTextButton(
                onClick = {
                    onConfirmRequest()
                    onDismissRequest()
                },
            ) {
                Text(
                    text = stringResource(id = android.R.string.ok),
                )
            }
        },
        dismissButton = {
            BlockerTextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(id = android.R.string.cancel),
                )
            }
        },
    )
}
