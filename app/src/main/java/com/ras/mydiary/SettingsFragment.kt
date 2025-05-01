package com.ras.mydiary;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import User
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ras.mydiary.databinding.FragmentSettingsBinding
import java.io.ByteArrayOutputStream

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // UI Elements
        val doneButton: TextView = view.findViewById(R.id.doneButton)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val etName: EditText = view.findViewById(R.id.etName)
        val etUsername: EditText = view.findViewById(R.id.etUsername)
        val etContact: EditText = view.findViewById(R.id.etContact)
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val editProfileImage: ImageView = view.findViewById(R.id.editProfileImage)

        // Get current user ID
        val currentUserId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Load current user data
        database.getReference("Users").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        tvUsername.text = user.username
                        etName.setText(user.name)
                        etUsername.setText(user.username)
                        etContact.setText(user.phone)
                        // Load profile image if it exists
                        if (user.profileImageUrl.isNotEmpty()) {
                            try {
                                val decodedBytes = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                profileImage.setImageBitmap(bitmap)
                                selectedImageBitmap = bitmap // Store initial image
                            } catch (e: Exception) {
                                profileImage.setImageResource(R.drawable.icon_profile_foreground)
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.icon_profile_foreground)
                        }
                    } else {
                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        // Image picker launcher
        val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageBitmap = uriToBitmap(requireContext(), it)
                selectedImageBitmap?.let { bitmap ->
                    profileImage.setImageBitmap(bitmap)
                } ?: Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle image edit click
        editProfileImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // Handle Done button click
        doneButton.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newUsername = etUsername.text.toString().trim()
            val newContact = etContact.text.toString().trim()

            // Validation
            if (newUsername.isEmpty()) {
                etUsername.error = "Username cannot be empty"
                return@setOnClickListener
            }

            // Prepare updated data
            val updatedUserData = mutableMapOf<String, Any>(
                "name" to newName,
                "username" to newUsername,
                "contactNumber" to newContact
            )

            // Add image if it was changed
            selectedImageBitmap?.let {
                val base64Image = bitmapToBase64(it)
                updatedUserData["profileImageUrl"] = base64Image
            }

            // Update Firebase
            database.getReference("Users").child(currentUserId)
                .updateChildren(updatedUserData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}