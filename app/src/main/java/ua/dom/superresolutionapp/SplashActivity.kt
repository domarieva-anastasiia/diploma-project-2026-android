package ua.dom.superresolutionapp

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = resources.getColor(R.color.gray_green, theme)
        setContentView(R.layout.activity_splash)

        val flowerImageView = findViewById<ImageView>(R.id.flowerImageView)
        val flowerAnimation = flowerImageView.drawable as? AnimationDrawable

        Handler(Looper.getMainLooper()).postDelayed({
            flowerAnimation?.start()
        }, 600)

        Handler(Looper.getMainLooper()).postDelayed({

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            finish()
        }, 3700)
    }
}