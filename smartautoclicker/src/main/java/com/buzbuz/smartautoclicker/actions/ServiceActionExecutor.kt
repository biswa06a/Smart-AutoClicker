/*
 * Copyright (C) 2024 Kevin Buzeau
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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.AndroidRuntimeException
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.Dumpable
import kotlinx.coroutines.delay
import java.io.PrintWriter
import org.tensorflow.lite.Interpreter
import android.graphics.Bitmap
import android.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.FileInputStream
import java.nio.channels.FileChannel

class ServiceActionExecutor(private val service: AccessibilityService) {

    private val gestureExecutor: GestureExecutor = GestureExecutor()

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

    /** AI-based function to detect and classify an image. */
    fun detectImage(image: Bitmap): Int {
        val inputBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3).order(ByteOrder.nativeOrder())
        val outputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())

        // Resize & normalize image
        val scaledBitmap = Bitmap.createScaledBitmap(image, 224, 224, true)
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = scaledBitmap.getPixel(x, y)
                inputBuffer.putFloat(Color.red(pixel) / 255.0f)
                inputBuffer.putFloat(Color.green(pixel) / 255.0f)
                inputBuffer.putFloat(Color.blue(pixel) / 255.0f)
            }
        }

        // Run AI model
        tflite.run(inputBuffer, outputBuffer)
        return outputBuffer.int  // Predicted label
    }

    /** Executes a gesture based on AI detection. */
    fun performClickWithAI(image: Bitmap) {
        val detectedClass = detectImage(image)

        if (detectedClass == 1) { // If AI detects the target, perform click
            performClick(500, 500)  // Change coordinates to match your game
        }
    }

    fun reset() {
        gestureExecutor.reset()
    }

    suspend fun safeDispatchGesture(gestureDescription: GestureDescription) {
        if (!gestureExecutor.dispatchGesture(service, gestureDescription)) {
            Log.w(TAG, "System did not execute the gesture")
            delay(500)
        }
    }

    fun safeStartActivity(intent: Intent) {
        try {
            service.startActivity(intent)
        } catch (anfe: ActivityNotFoundException) {
            Log.w(TAG, "Can't start activity, it is not found.")
        } catch (arex: AndroidRuntimeException) {
            Log.w(TAG, "Can't start activity, Intent is invalid.")
        } catch (iaex: IllegalArgumentException) {
            Log.w(TAG, "Can't start activity, Intent contains illegal arguments.")
        } catch (secEx: SecurityException) {
            Log.w(TAG, "Can't start activity due to security restrictions.")
        } catch (npe: NullPointerException) {
            Log.w(TAG, "Can't start activity, Intent is null.")
        }
    }

    fun safeSendBroadcast(intent: Intent) {
        try {
            service.sendBroadcast(intent)
        } catch (iaex: IllegalArgumentException) {
            Log.w(TAG, "Can't send broadcast, Intent is invalid.")
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        gestureExecutor.dump(writer, prefix)
    }
}

private const val TAG = "ServiceActionExecutor"
