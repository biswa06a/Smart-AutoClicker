package com.buzbuz.smartautoclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.*
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class SmartAutoClickerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    /** Loads TensorFlow Lite model for AI-based image recognition. */
    private val tflite: Interpreter by lazy {
        val assetManager = assets
        val model = assetManager.openFd("model.tflite")
        val inputStream = FileInputStream(model.fileDescriptor)
        val buffer = inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            model.startOffset,
            model.declaredLength
        )
        Interpreter(buffer)
    }

    /** Captures a screenshot of the current screen. */
    private fun captureScreen(): Bitmap? {
        val imageReader = ImageReader.newInstance(1080, 1920, PixelFormat.RGBA_8888, 2)
        val surface = imageReader.surface
        val image = imageReader.acquireLatestImage() ?: return null

        val planes = image.planes
        val buffer = planes[0].buffer
        val width = image.width
        val height = image.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        return bitmap
    }

    /** AI-based function to detect and classify an image before executing a gesture. */
    private fun detectImage(image: Bitmap): Int {
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

    /** Executes an auto-click when the AI detects the target. */
    private fun performClick() {
        val screenshot = captureScreen() ?: return
        val detectedClass = detectImage(screenshot)

        if (detectedClass == 1) { // If AI detects the target
            clickOnPosition(500, 500) // Adjust coordinates as needed
        }
    }

    /** Handles accessibility events (triggered automatically). */
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        handler.postDelayed({ performClick() }, 1000) // Runs every second
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted")
    }

    private fun clickOnPosition(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, null, null)
    }

    companion object {
        private const val TAG = "SmartAutoClickerService"
    }
}
