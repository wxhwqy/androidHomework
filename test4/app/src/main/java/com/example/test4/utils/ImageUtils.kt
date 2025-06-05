package com.example.test4.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

class ImageUtils(private val context: Context) {
    
    companion object {
        private const val IMAGE_DIRECTORY = "note_images"
        private const val MAX_IMAGE_SIZE = 1024 * 1024
        private const val JPEG_QUALITY = 85
    }
    
    fun saveImageToInternalStorage(imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                val imageDir = File(context.filesDir, IMAGE_DIRECTORY)
                if (!imageDir.exists()) {
                    imageDir.mkdirs()
                }
                
                val fileName = "img_${UUID.randomUUID()}.jpg"
                val imageFile = File(imageDir, fileName)
                
                val bitmap = BitmapFactory.decodeStream(stream)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                }
                
                bitmap.recycle()
                imageFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun deleteImageFile(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
} 