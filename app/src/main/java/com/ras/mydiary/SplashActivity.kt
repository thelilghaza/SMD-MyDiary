package com.ras.mydiary

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Find the LottieAnimationView
        val animationView = findViewById<LottieAnimationView>(R.id.splashAnimation)
        animationView.setAnimation(R.raw.splash_animation) // Load JSON animation
        animationView.playAnimation() // Start Animation

        // Delay and check authentication before proceeding
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAuthentication()
        }, 1000) // 1-second delay
    }

    private fun checkUserAuthentication() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is logged in, navigate to FeedActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User not logged in, navigate to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish() // Close SplashActivity
    }
}
