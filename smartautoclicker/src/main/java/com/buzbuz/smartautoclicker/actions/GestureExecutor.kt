/*
 * Copyright (C) 2025 Kevin Buzeau
 *
 * This program is free software: you can redistribute 
 * it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will 
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.buzbuz.smartautoclicker.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulation
import kotlinx.coroutines.suspendCoroutine
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

internal class GestureExecutor : Dumpable {

    private var resultCallback: GestureResultCallback? = null
    private var currentContinuation: Continuation<Boolean>? = null
    private var completedGestures: Long = 0L
    private var cancelledGestures: Long = 0L
    private var errorGestures: Long = 0L

    /** Loads TensorFlow Lite model for AI-based image recognition. */
    private val tflite: Interpreter by lazy {
        val assetManager = service.assets
        val model = assetManager.openFd("model.tflite")
        val inputStream = FileInputStream(model.fileDescriptor)
        val buffer = inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            model.startOffset,
            model.declaredLength
        )
        Interpreter(buffer)
    }

    /** AI-based function to detect and classify an image before executing a gesture. */
    fun detectImage(image: Bitmap): Int {
        val inputBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3).order(ByteOrder.nativeOrder())
        val outputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(image, 224, 224, true)
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = scaledBitmap.getPixel(x, y)
                inputBuffer.putFloat(Color.red(pixel) / 255.0f)
                inputBuffer.putFloat(Color.green(pixel) / 255.0f)
                inputBuffer.putFloat(Color.blue(pixel) / 255.0f)
            }
        }

        tflite.run(inputBuffer, outputBuffer)
        return outputBuffer.int  // Predicted label
    }

    /** Executes a gesture based on AI detection. */
    suspend fun dispatchGesture(service: AccessibilityService, gesture: GestureDescription, image: Bitmap): Boolean {
        if (currentContinuation != null) {
            Log.w(TAG, "Previous gesture result is not finished. Resetting executor.")
            resultCallback = null
            currentContinuation = null
        }

        resultCallback = resultCallback ?: newGestureResultCallback()

        return suspendCoroutine { continuation ->
            currentContinuation = continuation

            // Run AI model to detect if a gesture should be executed
            val detectedClass = detectImage(image)
            if (detectedClass == 1) { // If AI detects the target
                try {
                    service.dispatchGesture(gesture, resultCallback, null)
                } catch (rEx: RuntimeException) {
                    Log.w(TAG, "System is not responsive, the gesture execution failed.")
                    errorGestures++
                    resumeExecution(gestureError = true)
                }
            } else {
                Log.w(TAG, "AI did not detect the target, skipping gesture.")
                resumeExecution(gestureError = true)
            }
        }
    }

    private fun resumeExecution(gestureError: Boolean) {
        currentContinuation?.let { continuation ->
            currentContinuation = null
            try {
                continuation.resume(!gestureError)
            } catch (isEx: IllegalStateException) {
                Log.w(TAG, "Continuation has already been resumed.")
            }
        } ?: Log.w(TAG, "Can't resume continuation. Did it complete?")
    }

    private fun newGestureResultCallback() = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) {
            completedGestures++
            resumeExecution(gestureError = false)
        }

        override fun onCancelled(gestureDescription: GestureDescription) {
            cancelledGestures++
            resumeExecution(gestureError = false)
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = "${prefix.addDumpTabulation()}"

        writer.apply {
            append(prefix).println("* GestureExecutor:")
            append(contentPrefix).append("Completed=$completedGestures\n")
            append(contentPrefix).append("Cancelled=$cancelledGestures\n")
            append(contentPrefix).append("Error=$errorGestures\n")
        }
    }
}

private const val TAG = "GestureExecutor"
