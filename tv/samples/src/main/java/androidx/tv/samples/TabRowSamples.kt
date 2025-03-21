/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.tv.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material.LocalContentColor
import androidx.tv.material.Tab
import androidx.tv.material.TabDefaults
import androidx.tv.material.TabRow
import androidx.tv.material.TabRowDefaults
import kotlin.time.Duration.Companion.microseconds
import kotlinx.coroutines.delay

/**
 * Tab row with a Pill indicator
 */
@Composable
@Sampled
fun PillIndicatorTabRow() {
  val tabs = listOf("Tab 1", "Tab 2", "Tab 3")
  var selectedTabIndex by remember { mutableStateOf(0) }

  TabRow(
    selectedTabIndex = selectedTabIndex,
    separator = { Spacer(modifier = Modifier.width(12.dp)) },
  ) {
    tabs.forEachIndexed { index, tab ->
      Tab(
        selected = index == selectedTabIndex,
        onFocus = { selectedTabIndex = index },
      ) {
        Text(
          text = tab,
          fontSize = 12.sp,
          color = LocalContentColor.current,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }
    }
  }
}

/**
 * Tab row with an Underlined indicator
 */
@Composable
@Sampled
fun UnderlinedIndicatorTabRow() {
  val tabs = listOf("Tab 1", "Tab 2", "Tab 3")
  var selectedTabIndex by remember { mutableStateOf(0) }

  TabRow(
    selectedTabIndex = selectedTabIndex,
    separator = { Spacer(modifier = Modifier.width(12.dp)) },
    indicator = { tabPositions ->
      TabRowDefaults.UnderlinedIndicator(
        currentTabPosition = tabPositions[selectedTabIndex]
      )
    }
  ) {
    tabs.forEachIndexed { index, tab ->
      Tab(
        selected = index == selectedTabIndex,
        onFocus = { selectedTabIndex = index },
        colors = TabDefaults.underlinedIndicatorTabColors(),
      ) {
        Text(
          text = tab,
          fontSize = 12.sp,
          color = LocalContentColor.current,
          modifier = Modifier.padding(bottom = 4.dp)
        )
      }
    }
  }
}

/**
 * Tab row with delay between tab changes
 */
@Composable
@Sampled
fun TabRowWithDebounce() {
  val tabs = listOf("Tab 1", "Tab 2", "Tab 3")
  var selectedTabIndex by remember { mutableStateOf(0) }

  // This index will be used to show a panel
  var tabPanelIndex by remember { mutableStateOf(selectedTabIndex) }

  // Change the tab-panel only after some delay
  LaunchedEffect(selectedTabIndex) {
    delay(250.microseconds)
    tabPanelIndex = selectedTabIndex
  }

  TabRow(
    selectedTabIndex = selectedTabIndex,
    separator = { Spacer(modifier = Modifier.width(12.dp)) },
  ) {
    tabs.forEachIndexed { index, tab ->
      Tab(
        selected = index == selectedTabIndex,
        onFocus = { selectedTabIndex = index },
      ) {
        Text(
          text = tab,
          fontSize = 12.sp,
          color = LocalContentColor.current,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }
    }
  }
}

/**
 * Tab changes onClick instead of onFocus
 */
@Composable
@Sampled
fun OnClickNavigation() {
  val bgColors = listOf(
    Color(0x6a, 0x16, 0x16),
    Color(0x6a, 0x40, 0x16),
    Color(0x6a, 0x6a, 0x16),
    Color(0x40, 0x6a, 0x16),
  )

  var focusedTabIndex by remember { mutableStateOf(0) }
  var activeTabIndex by remember { mutableStateOf(focusedTabIndex) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColors[activeTabIndex])
  ) {
    TabRow(
      selectedTabIndex = focusedTabIndex,
      indicator = { tabPositions ->
        // FocusedTab's indicator
        TabRowDefaults.PillIndicator(
          currentTabPosition = tabPositions[focusedTabIndex],
          activeColor = Color.Blue.copy(alpha = 0.4f),
          inactiveColor = Color.Transparent,
        )

        // SelectedTab's indicator
        TabRowDefaults.PillIndicator(
          currentTabPosition = tabPositions[activeTabIndex]
        )
      }
    ) {
      repeat(bgColors.size) {
        Tab(
          selected = activeTabIndex == it,
          onFocus = { focusedTabIndex = it },
          onClick = {
            focusedTabIndex = it
            activeTabIndex = it
          }
        ) {
          Text(
            text = "Tab ${it + 1}",
            fontSize = 12.sp,
            color = LocalContentColor.current,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
        }
      }
    }
  }
}
