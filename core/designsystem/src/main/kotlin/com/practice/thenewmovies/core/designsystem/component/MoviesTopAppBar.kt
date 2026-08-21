/*
 * Copyright 2026 TheNewMovies
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
package com.practice.thenewmovies.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.thenewmovies.core.designsystem.icon.MoviesIcons
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

@Composable
fun MoviesTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: Int? = null,
    onActionClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(MoviesIcons.Back),
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onBackClick),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        if (actionIcon != null) {
            Icon(
                painter = painterResource(actionIcon),
                contentDescription = "Action",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onActionClick),
            )
        } else {
            Spacer(modifier = Modifier.size(36.dp))
        }
    }
}

@Preview
@Composable
private fun MoviesTopAppBarPreview() {
    MoviesTheme {
        MoviesTopAppBar(title = "Movie Title", onBackClick = {})
    }
}
