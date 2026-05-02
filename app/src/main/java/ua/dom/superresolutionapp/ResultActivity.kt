package ua.dom.superresolutionapp

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ResultActivity : AppCompatActivity() {

    private lateinit var beforeImage: ImageView
    private lateinit var afterImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = resources.getColor(R.color.gray_green, theme)
        setContentView(R.layout.activity_result)

        beforeImage = findViewById(R.id.beforeImage)
        afterImage = findViewById(R.id.afterImage)

        val beforeUri = Uri.parse(intent.getStringExtra("before_uri"))
        val afterPath = intent.getStringExtra("after_path")

//        val beforeBitmap = BitmapFactory.decodeStream(
//            contentResolver.openInputStream(beforeUri)
//        )
//
//        val afterBitmap = BitmapFactory.decodeFile(afterPath)

        val originalBefore = BitmapFactory.decodeStream(
            contentResolver.openInputStream(beforeUri)
        )

        val afterBitmap = BitmapFactory.decodeFile(afterPath)

        val beforeBitmap = Bitmap.createScaledBitmap(
            originalBefore,
            afterBitmap.width,
            afterBitmap.height,
            true
        )

        beforeImage.setImageBitmap(beforeBitmap)
        afterImage.setImageBitmap(afterBitmap)

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveImage(afterBitmap)
        }

        findViewById<Button>(R.id.homeButton).setOnClickListener {
            finish()
        }

        val container = findViewById<FrameLayout>(R.id.sliderContainer)
        val sliderLine = findViewById<View>(R.id.sliderLine)

        container.post {
            val width = container.width
            var sliderPosition = width / 2

            updateSlider(beforeImage, sliderLine, sliderPosition)

            container.setOnTouchListener { _, event ->
                sliderPosition = event.x.toInt().coerceIn(0, width)
                updateSlider(beforeImage, sliderLine, sliderPosition)
                true
            }
        }
    }

    private fun saveImage(bitmap: Bitmap) {
        val filename = "SR_${System.currentTimeMillis()}.png"

        val fos: OutputStream

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SRGAN")
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = resolver.openOutputStream(imageUri!!)!!
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        Toast.makeText(this, "Завантажено", Toast.LENGTH_SHORT).show()
    }

    fun updateSlider(beforeImage: ImageView, sliderLine: View, position: Int) {
        beforeImage.clipBounds = Rect(0, 0, position, beforeImage.height)
        sliderLine.x = position.toFloat()
    }


}