/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.key

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focusRequester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import com.google.common.truth.Truth
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(
    ExperimentalFocus::class,
    ExperimentalKeyInput::class
)
class ShortcutsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun shortcuts_triggered() {
        val focusRequester = FocusRequester()
        var triggeredShortcut = false
        rule.setContent {
            Box(
                modifier = Modifier
                    .size(10.dp, 10.dp)
                    .focusRequester(focusRequester)
                    .focus()
                    .shortcuts {
                        on(Key.MetaLeft + Key.Enter) {
                            triggeredShortcut = true
                        }
                    }
            )
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        val firstKeyConsumed = rule.onRoot().performKeyPress(
            keyEvent(
                Key.MetaLeft, KeyEventType.KeyDown
            )
        )

        val secondKeyConsumed = rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(triggeredShortcut).isTrue()
            Truth.assertThat(firstKeyConsumed).isFalse()
            Truth.assertThat(secondKeyConsumed).isTrue()
        }
    }

    @Test
    fun shortcuts_states() {
        val focusRequester = FocusRequester()
        var triggered = 0
        var setShortcuts by mutableStateOf(true)
        rule.setContent {
            Box(
                modifier = Modifier
                    .size(10.dp, 10.dp)
                    .focusRequester(focusRequester)
                    .focus()
                    .shortcuts {
                        if (setShortcuts) {
                            on(Key.Enter) {
                                triggered += 1
                            }
                        }
                    }
            )
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(triggered).isEqualTo(1)
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyUp
            )
        )

        // Disables shortcuts
        rule.runOnIdle {
            setShortcuts = false
        }

        rule.onRoot().performKeyPress(
            keyEvent(
                Key.Enter, KeyEventType.KeyDown
            )
        )

        rule.runOnIdle {
            Truth.assertThat(triggered).isEqualTo(1)
        }
    }
}