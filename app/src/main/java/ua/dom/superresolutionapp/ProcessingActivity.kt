package ua.dom.superresolutionapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import okhttp3.RequestBody.Companion.asRequestBody
import okio.buffer
import okio.sink
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.widget.ImageView


class ProcessingActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var selectedImageUri: Uri
    private lateinit var flowerImageView: ImageView
    private var jobId: String? = null
    private var isCancelled = false
    private var isPolling = true
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = resources.getColor(R.color.gray_green, theme)
        setContentView(R.layout.activity_processing)

        flowerImageView = findViewById(R.id.flowerImageView)
        flowerImageView.setImageResource(R.drawable.flower_animation_loop)

        val cancelButton = findViewById<Button>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            isCancelled = true
            isPolling = false
            handler.removeCallbacksAndMessages(null)

            jobId?.let {
                val request = Request.Builder()
                    .url("https://srgan-domarieva.up.railway.app/cancel/$it")
                    .post(RequestBody.create(null, ByteArray(0)))
                    .build()

                Thread {
                    client.newCall(request).execute()
                }.start()
            }
            finish()
        }

        progressBar = findViewById(R.id.progressBar)

        val uriString = intent.getStringExtra("image_uri")
        if (uriString != null) {
            selectedImageUri = Uri.parse(uriString)
            startEnhance(selectedImageUri)
            Log.d("SRGAN", "startEnhance called")
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val processingAnimation = flowerImageView.drawable as? AnimationDrawable
            processingAnimation?.start()
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)!!
        val file = File.createTempFile("upload", ".jpg", cacheDir)
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return file
    }


    private fun startEnhance(uri: Uri) {

        val file = uriToFile(uri)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("https://srgan-domarieva.up.railway.app/enhance")
            .post(requestBody)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body!!.string())

                jobId = json.getString("job_id")

                runOnUiThread {
                    startPolling()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun startPolling() {
        if (isPolling) {
            handler.postDelayed(object : Runnable {
                override fun run() {

                    if (isCancelled || jobId == null) return

                    checkStatus()

                    handler.postDelayed(this, 2000)
                }
            }, 2000)
        }
    }

    private fun checkStatus() {

        val request = Request.Builder()
            .url("https://srgan-domarieva.up.railway.app/status/$jobId")
            .get()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body!!.string())
                Log.d("SERVER_JSON", json.toString())
                val status = json.getString("status")
                val progress = json.optInt("progress", 0)

                runOnUiThread {
                    updateProgressText(status, progress)
                    if (status == "PROGRESS" && progress > 0) {
                        progressBar.progress = progress
                    }

                    if (status == "SUCCESS") {
                        progressBar.progress = 100
                        val processingAnimation = flowerImageView.drawable as? AnimationDrawable
                        processingAnimation?.stop()
                    }
                }

                if (status == "SUCCESS" && isPolling) {
                    isPolling = false
                    val downloadUrl = json.getString("download_url")
                    getResult(downloadUrl)
                }

                if (status == "FAILURE") {
                    isPolling = false

                    runOnUiThread {
                        Toast.makeText(this, "Processing failed", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun updateProgressText(status: String, progress: Int) {
        val text = findViewById<TextView>(R.id.progressText)

        text.text = when {
            status == "PROGRESS" && progress < 50 -> getString(R.string.status_progress_low)
            status == "PROGRESS" && progress >= 50 -> getString(R.string.status_progress_high)

            status == "PENDING" -> getString(R.string.status_pending)
            status == "STARTED" -> getString(R.string.status_started)
            status == "SUCCESS" -> getString(R.string.status_success)
            status == "FAILURE" -> getString(R.string.status_failure)
            status == "REVOKED" -> getString(R.string.status_revoked)

            else -> status
        }
    }

    private fun getResult(downloadUrl: String) {
        val baseUrl = "https://srgan-domarieva.up.railway.app/"
        val fullUrl = "$baseUrl$downloadUrl"

        val request = Request.Builder()
            .url(fullUrl)
            .get()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val file = File(cacheDir, "result.png")

                    response.body?.source()?.use { source ->
                        file.outputStream().use { output ->
                            source.readAll(output.sink())
                        }
                    }

                    runOnUiThread {
                        openResult(file)
                    }
                }else {
                    Log.e("GET_RESULT", "Сервер повернув помилку: ${response.code}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun openResult(file: File) {

        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("before_uri", selectedImageUri.toString())
        intent.putExtra("after_path", file.absolutePath)

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        handler.removeCallbacksAndMessages(null)
    }


}