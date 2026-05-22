package ua.dom.superresolutionapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var galleryButton: Button
    private lateinit var previewImage: ImageView
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var selectedImageUri: Uri? = null
    private lateinit var cameraButton: Button
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var photoUri: Uri? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    val url = "https://srgan-domarieva.up.railway.app/enhance"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = resources.getColor(R.color.gray_green, theme)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        galleryButton = findViewById(R.id.galleryButton)
        previewImage = findViewById(R.id.previewImage)

        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {
                selectedImageUri = uri
                previewImage.setImageURI(uri)
            }

        }

        galleryButton.setOnClickListener {

            galleryLauncher.launch("image/*")

        }

        val historyButton = findViewById<Button>(R.id.historyButton)
        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)

        }

        val languageButton = findViewById<ImageView>(R.id.languageSelector)
        languageButton.setOnClickListener { view ->
            showLanguageMenu(view)
        }

        val enhanceButton = findViewById<Button>(R.id.enhanceButton)
        enhanceButton.setOnClickListener {
            val intent = Intent(this, ProcessingActivity::class.java)
            intent.putExtra("image_uri", selectedImageUri.toString())
            startActivityForResult(intent, 1)
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Дозвіл на камеру відхилено", Toast.LENGTH_SHORT).show()
            }

        }

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success && photoUri != null) {
                selectedImageUri = photoUri
                previewImage.setImageURI(photoUri)
            }
        }

        cameraButton = findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }



        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == 1 && resultCode == RESULT_OK) {
                val path = data?.getStringExtra("result_path")
                val bitmap = BitmapFactory.decodeFile(path)

                findViewById<ImageView>(R.id.previewImage).setImageBitmap(bitmap)
            }
        }


    }

    private fun createImageFile(): File {
        val storageDir = getExternalFilesDir("Pictures")
        return File.createTempFile(
            "photo_",
            ".jpg",
            storageDir
        )
    }

    private fun openCamera() {
        val file = createImageFile()

        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        photoUri?.let { uri ->
            cameraLauncher.launch(uri)
        }
    }

    private fun changeLanguage(languageCode: String) {
        // languageCode може бути "uk" або "en"
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    private fun showLanguageMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.language_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.lang_uk -> {
                    changeLanguage("uk")
                    true
                }
                R.id.lang_en -> {
                    changeLanguage("en")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}

