package com.ras.mydiary

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val textView = findViewById<TextView>(R.id.registerPromptTextView)
        textView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        val usernameField = findViewById<EditText>(R.id.usernameEditText)
        val passwordField = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show a loading message while fetching user data
            Toast.makeText(this, "Checking username...", Toast.LENGTH_SHORT).show()

            // Fetch the corresponding email from the database using the username
            database.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userSnapshot = snapshot.children.firstOrNull()
                            val email = userSnapshot?.child("email")?.getValue(String::class.java)

                            if (email != null) {
                                loginWithEmail(email, password)
                            } else {
                                Toast.makeText(this@LoginActivity, "User data is incomplete", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Username not found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@LoginActivity, "Database Error", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User is logged in successfully, now fetch role
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // Fetch role from the database
                    database.child(userId).child("role").get()
                        .addOnSuccessListener { roleSnapshot ->
                            val roleValue = roleSnapshot.getValue(String::class.java)

                            // Navigate based on the role
                            when (roleValue) {
                                "user" -> {
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                                "therapist" -> {
                                    startActivity(Intent(this, MainActivity2::class.java))
                                }
                                "admin" -> {
                                    startActivity(Intent(this, AdminDashboard::class.java))
                                }
                                else -> {
                                    Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show()
                                }
                            }
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to retrieve role", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
