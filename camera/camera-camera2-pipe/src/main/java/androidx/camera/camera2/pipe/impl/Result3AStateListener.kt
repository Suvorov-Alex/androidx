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

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.CaptureResult
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.Status3A
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * Given a map of keys and a list of acceptable values for each key, this checks if the given
 * [CaptureResult] has all of those keys and for every key the value for that key is one of the
 * acceptable values.
 *
 * This update method can be called multiple times as we get newer [CaptureResult]s from the camera
 * device. This class also exposes a [Deferred] to query the status of desired state.
 */
class Result3AStateListener(
    private val exitConditionForKeys: Map<CaptureResult.Key<*>, List<Any>>,
    private val frameLimit: Int? = null,
    private val timeLimitNs: Long? = null
) {

    init {
        require(exitConditionForKeys.isNotEmpty()) { "Exit condition map for keys is empty." }
    }

    private val deferred = CompletableDeferred<Result3A>()

    @Volatile private var frameNumberOfFirstUpdate: FrameNumber? = null
    @Volatile private var timestampOfFirstUpdateNs: Long? = null
    @GuardedBy("this")
    private var initialRequestNumber: RequestNumber? = null

    fun onRequestSequenceCreated(requestNumber: RequestNumber) {
        synchronized(this) {
            if (initialRequestNumber == null) {
                initialRequestNumber = requestNumber
            }
        }
    }

    fun update(requestNumber: RequestNumber, frameMetadata: FrameMetadata): Boolean {
        // Save some compute if the task is already complete or has been canceled.
        if (deferred.isCompleted || deferred.isCancelled) {
            return true
        }

        // Ignore the update if the update is from a previously submitted request.
        synchronized(this) {
            val initialRequestNumber = initialRequestNumber
            if (initialRequestNumber == null || requestNumber.value < initialRequestNumber.value) {
                return false
            }
        }

        val currentTimestampNs: Long? = frameMetadata.get(CaptureResult.SENSOR_TIMESTAMP)
        val currentFrameNumber = frameMetadata.frameNumber

        if (currentTimestampNs != null && timestampOfFirstUpdateNs == null) {
            timestampOfFirstUpdateNs = currentTimestampNs
        }

        val timestampOfFirstUpdateNs = timestampOfFirstUpdateNs
        if (timeLimitNs != null &&
            timestampOfFirstUpdateNs != null &&
            currentTimestampNs != null &&
            currentTimestampNs - timestampOfFirstUpdateNs > timeLimitNs
        ) {
            deferred.complete(
                Result3A(frameMetadata.frameNumber, Status3A.TIME_LIMIT_REACHED)
            )
            return true
        }

        if (frameNumberOfFirstUpdate == null) {
            frameNumberOfFirstUpdate = currentFrameNumber
        }

        val frameNumberOfFirstUpdate = frameNumberOfFirstUpdate
        if (frameNumberOfFirstUpdate != null && frameLimit != null &&
            currentFrameNumber.value - frameNumberOfFirstUpdate.value > frameLimit
        ) {
            deferred.complete(
                Result3A(frameMetadata.frameNumber, Status3A.FRAME_LIMIT_REACHED)
            )
            return true
        }

        for ((k, v) in exitConditionForKeys) {
            val valueInCaptureResult = frameMetadata.get(k)
            if (!v.contains(valueInCaptureResult)) {
                return false
            }
        }
        deferred.complete(Result3A(frameMetadata.frameNumber, Status3A.OK))
        return true
    }

    fun getDeferredResult(): Deferred<Result3A> {
        return deferred
    }
}
