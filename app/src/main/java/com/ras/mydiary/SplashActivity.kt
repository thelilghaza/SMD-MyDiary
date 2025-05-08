package com.ras.mydiary

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val animationView = findViewById<LottieAnimationView>(R.id.splashAnimation)
        animationView.setAnimation(R.raw.splash_animation)
        animationView.loop(true)
        animationView.playAnimation()

        checkUserAuthentication()
    }

    private fun checkUserAuthentication() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance().getReference("Users")
            database.child(currentUser.uid).child("role").get()
                .addOnSuccessListener { roleSnapshot ->
                    val role = roleSnapshot.getValue(String::class.java)
                    val intent = when (role) {
                        "user" -> Intent(this, MainActivity::class.java)
                        "therapist" -> Intent(this, MainActivity2::class.java)
                        "admin" -> Intent(this, AdminDashboard::class.java)
                        else -> Intent(this, LoginActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
