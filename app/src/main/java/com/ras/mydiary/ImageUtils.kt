package com.ras.mydiary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Utility class for handling image operations in the app
 */
object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Convert a Uri to a Bitmap
     */
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Uri to Bitmap: ${e.message}")
            null
        }
    }

    /**
     * Resize a bitmap to a maximum dimension while maintaining aspect ratio
     * @param bitmap The bitmap to resize
     * @param maxDimension The maximum width or height
     * @return The resized bitmap
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Don't resize if already smaller than max dimension
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Convert a Bitmap to a Base64 string
     * @param bitmap The bitmap to convert
     * @param quality JPEG quality (0-100)
     * @return Base64 encoded string
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Convert a Base64 string to a Bitmap
     */
    fun base64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrEmpty()) return null

        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Base64 to Bitmap: ${e.message}")
            null
        }
    }

    /**
     * Optimize image from Uri for upload - resizes and encodes to Base64
     */
    fun optimizeImageForUpload(context: Context, uri: Uri): String? {
        return try {
            val originalBitmap = uriToBitmap(context, uri) ?: return null
            val resizedBitmap = resizeBitmap(originalBitmap, 1024)

            // Recycle the original bitmap if different from resized
            if (originalBitmap != resizedBitmap) {
                originalBitmap.recycle()
            }

            // Convert to Base64
            val base64 = bitmapToBase64(resizedBitmap, 85)

            // Recycle the bitmap
            resizedBitmap.recycle()

            base64
        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing image: ${e.message}")
            null
        }
    }
}