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

package androidx.compose.runtime.livedata.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.lifecycle.LiveData
import androidx.compose.foundation.Text
import androidx.compose.runtime.livedata.observeAsState

@Sampled
@Composable
fun LiveDataSample(liveData: LiveData<String>) {
    val value: String? by liveData.observeAsState()
    Text("Value is $value")
}

@Sampled
@Composable
fun LiveDataWithInitialSample(liveData: LiveData<String>) {
    val value: String by liveData.observeAsState("initial")
    Text("Value is $value")
}