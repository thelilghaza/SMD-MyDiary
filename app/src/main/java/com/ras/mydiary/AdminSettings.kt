package com.ras.mydiary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class AdminSettings : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageBitmap: Bitmap? = null

    // UI Elements
    private lateinit var usernameHeader: TextView
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var contactEditText: EditText
    private lateinit var profileImageView: ImageView
    private lateinit var editProfileImageButton: ImageView
    private lateinit var saveButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize UI elements
        usernameHeader = findViewById(R.id.tvUsername)
        nameEditText = findViewById(R.id.etName)
        usernameEditText = findViewById(R.id.etUsername)
        contactEditText = findViewById(R.id.etContact)
        profileImageView = findViewById(R.id.profileImage)
        editProfileImageButton = findViewById(R.id.editProfileImage)
        saveButton = findViewById(R.id.saveButton)

        // Set up the toolbar with back button
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        //supportActionBar?.title = "Admin Settings"

        // Get current admin user ID
        val currentUserId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load current admin data
        loadAdminData(currentUserId)

        // Image picker launcher
        val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageBitmap = uriToBitmap(this, it)
                selectedImageBitmap?.let { bitmap ->
                    profileImageView.setImageBitmap(bitmap)
                } ?: Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle image edit click
        editProfileImageButton.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // Handle Save button click
        saveButton.setOnClickListener {
            saveAdminProfile(currentUserId)
        }
    }

    private fun loadAdminData(adminId: String) {
        database.getReference("Users").child(adminId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val admin = snapshot.getValue(User::class.java)

                    if (admin != null) {
                        // Check if user is admin
                        if (admin.role != "admin") {
                            Toast.makeText(this@AdminSettings,
                                "Unauthorized access", Toast.LENGTH_SHORT).show()
                            finish()
                            return
                        }

                        // Populate UI with admin data
                        usernameHeader.text = admin.username
                        nameEditText.setText(admin.name)
                        usernameEditText.setText(admin.username)
                        contactEditText.setText(admin.phone)

                        // Load profile image if it exists
                        if (admin.profileImageUrl.isNotEmpty()) {
                            try {
                                val decodedBytes = Base64.decode(admin.profileImageUrl, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                profileImageView.setImageBitmap(bitmap)
                                selectedImageBitmap = bitmap // Store initial image
                            } catch (e: Exception) {
                                profileImageView.setImageResource(R.drawable.icon_profile_foreground)
                            }
                        } else {
                            profileImageView.setImageResource(R.drawable.icon_profile_foreground)
                        }
                    } else {
                        Toast.makeText(this@AdminSettings,
                            "Admin data not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminSettings,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveAdminProfile(adminId: String) {
        val newName = nameEditText.text.toString().trim()
        val newUsername = usernameEditText.text.toString().trim()
        val newContact = contactEditText.text.toString().trim()

        // Validation
        if (newUsername.isEmpty()) {
            usernameEditText.error = "Username cannot be empty"
            return
        }

        if (newName.isEmpty()) {
            nameEditText.error = "Name cannot be empty"
            return
        }

        // Show loading state
        saveButton.isEnabled = false
        saveButton.text = "Saving..."

        // Prepare updated data
        val updatedAdminData = mutableMapOf<String, Any>(
            "name" to newName,
            "username" to newUsername,
            "phone" to newContact
        )

        // Add image if it was changed
        selectedImageBitmap?.let {
            val base64Image = bitmapToBase64(it)
            updatedAdminData["profileImageUrl"] = base64Image
        }

        // Update Firebase
        database.getReference("Users").child(adminId)
            .updateChildren(updatedAdminData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()

                // Update the header with new username
                usernameHeader.text = newUsername

                setResult(RESULT_OK)

                // Reset button state
                saveButton.isEnabled = true
                saveButton.text = "Save Changes"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()

                // Reset button state
                saveButton.isEnabled = true
                saveButton.text = "Save Changes"
            }
    }

    // Convert Bitmap to Base64
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream) // Adjust quality (50% here)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Convert Uri to Bitmap
    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}