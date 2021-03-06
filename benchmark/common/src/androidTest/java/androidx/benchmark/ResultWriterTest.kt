/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@SmallTest
@RunWith(AndroidJUnit4::class)
class ResultWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val data = arrayOf(longArrayOf(100, 101, 102))
    private val names = listOf("timeNs")

    private val reportA = BenchmarkState.Report(
        testName = "MethodA",
        className = "package.Class1",
        totalRunTimeNs = 900000000,
        data = data.map { it.toList() },
        stats = data.mapIndexed { i, it -> Stats(it, names[i]) },
        repeatIterations = 100000,
        thermalThrottleSleepSeconds = 90000000,
        warmupIterations = 8000
    )
    private val reportB = BenchmarkState.Report(
        testName = "MethodB",
        className = "package.Class2",
        totalRunTimeNs = 900000000,
        data = data.map { it.toList() },
        stats = data.mapIndexed { i, it -> Stats(it, names[i]) },
        repeatIterations = 100000,
        thermalThrottleSleepSeconds = 90000000,
        warmupIterations = 8000
    )

    @Test
    fun shouldClearExistingContent() {
        val tempFile = tempFolder.newFile()

        val fakeText = "This text should not be in the final output"
        tempFile.writeText(fakeText)

        ResultWriter.writeReport(tempFile, listOf(reportA, reportB))
        assert(!tempFile.readText().startsWith(fakeText))
    }

    @Test
    fun validateJson() {
        val tempFile = tempFolder.newFile()

        val sustainedPerformanceModeInUse = IsolationActivity.sustainedPerformanceModeInUse

        ResultWriter.writeReport(tempFile, listOf(reportA, reportB))
        assertEquals(
            """
            {
                "context": {
                    "build": {
                        "device": "${Build.DEVICE}",
                        "fingerprint": "${Build.FINGERPRINT}",
                        "model": "${Build.MODEL}",
                        "version": {
                            "sdk": ${Build.VERSION.SDK_INT}
                        }
                    },
                    "cpuCoreCount": ${CpuInfo.coreDirs.size},
                    "cpuLocked": ${CpuInfo.locked},
                    "cpuMaxFreqHz": ${CpuInfo.maxFreqHz},
                    "memTotalBytes": ${MemInfo.memTotalBytes},
                    "sustainedPerformanceModeEnabled": $sustainedPerformanceModeInUse
                },
                "benchmarks": [
                    {
                        "name": "MethodA",
                        "params": {},
                        "className": "package.Class1",
                        "totalRunTimeNs": 900000000,
                        "metrics": {
                            "timeNs": {
                                "minimum": 100,
                                "maximum": 102,
                                "median": 101,
                                "runs": [
                                    100,
                                    101,
                                    102
                                ]
                            }
                        },
                        "warmupIterations": 8000,
                        "repeatIterations": 100000,
                        "thermalThrottleSleepSeconds": 90000000
                    },
                    {
                        "name": "MethodB",
                        "params": {},
                        "className": "package.Class2",
                        "totalRunTimeNs": 900000000,
                        "metrics": {
                            "timeNs": {
                                "minimum": 100,
                                "maximum": 102,
                                "median": 101,
                                "runs": [
                                    100,
                                    101,
                                    102
                                ]
                            }
                        },
                        "warmupIterations": 8000,
                        "repeatIterations": 100000,
                        "thermalThrottleSleepSeconds": 90000000
                    }
                ]
            }
            """.trimIndent(),
            tempFile.readText()
        )
    }

    @Test
    fun validateJsonWithParams() {
        val reportWithParams = BenchmarkState.Report(
            testName = "MethodWithParams[number=2,primeNumber=true]",
            className = "package.Class",
            totalRunTimeNs = 900000000,
            data = data.map { it.toList() },
            stats = data.mapIndexed { i, it -> Stats(it, names[i]) },
            repeatIterations = 100000,
            thermalThrottleSleepSeconds = 90000000,
            warmupIterations = 8000
        )

        val tempFile = tempFolder.newFile()
        ResultWriter.writeReport(tempFile, listOf(reportWithParams))
        val reportText = tempFile.readText()

        assertTrue {
            reportText.contains(
                """
                |            "name": "MethodWithParams[number=2,primeNumber=true]",
                |            "params": {
                |                "number": "2",
                |                "primeNumber": "true"
                |            },
                """.trimMargin()
            )
        }
    }

    @Test
    fun validateJsonWithInvalidParams() {
        val reportWithInvalidParams = BenchmarkState.Report(
            testName = "MethodWithParams[number=2,=true,]",
            className = "package.Class",
            totalRunTimeNs = 900000000,
            data = data.map { it.toList() },
            stats = data.mapIndexed { i, it -> Stats(it, names[i]) },
            repeatIterations = 100000,
            thermalThrottleSleepSeconds = 90000000,
            warmupIterations = 8000
        )

        val tempFile = tempFolder.newFile()
        ResultWriter.writeReport(tempFile, listOf(reportWithInvalidParams))
        val reportText = tempFile.readText()

        assertTrue {
            reportText.contains(
                """
                |            "name": "MethodWithParams[number=2,=true,]",
                |            "params": {
                |                "number": "2"
                |            },
                """.trimMargin()
            )
        }
    }
}
