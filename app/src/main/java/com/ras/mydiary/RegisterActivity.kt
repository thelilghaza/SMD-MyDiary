package com.ras.mydiary

import User
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.onesignal.OneSignal

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val textView = findViewById<TextView>(R.id.loginPromptTextView)
        textView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        val nameField = findViewById<EditText>(R.id.nameRegisterEditText)
        val usernameField = findViewById<EditText>(R.id.usernameRegisterEditText)
        val emailField = findViewById<EditText>(R.id.emailRegisterEditText)
        val phoneField = findViewById<EditText>(R.id.phoneRegisterEditText)
        val passwordField = findViewById<EditText>(R.id.passwordRegisterEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        val roleRadioGroup = findViewById<RadioGroup>(R.id.roleRadioGroup)

        registerButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val phone = phoneField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val name = nameField.text.toString().trim()

            val selectedRoleId = roleRadioGroup.checkedRadioButtonId
            val role = when (selectedRoleId) {
                R.id.roleUserRadio -> "user"
                R.id.roleTherapistRadio -> "therapist"
                else -> ""
            }

            // Validate fields
            if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                        val oneSignalId = OneSignal.User.onesignalId ?: ""

                        val appUser = User(
                            uid = userId,
                            username = username,
                            name = name,
                            email = email,
                            phone = phone,
                            role = role,
                            oneSignalId = oneSignalId,
                            profileImageUrl = ""
                        )

                        database.child(userId).setValue(appUser)
                            .addOnSuccessListener {
                                database.child(userId).child("role").get()
                                    .addOnSuccessListener { roleSnapshot ->
                                        val roleValue = roleSnapshot.getValue(String::class.java)

                                        when (roleValue) {
                                            "user" -> startActivity(Intent(this, MainActivity::class.java))
                                            "therapist" -> startActivity(Intent(this, MainActivity::class.java))
                                            else -> Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show()
                                        }

                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Failed to retrieve role", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}