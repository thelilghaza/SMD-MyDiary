package com.ras.mydiary

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ras.mydiary.databinding.FragmentManualJournalBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.graphics.scale

class ManualJournalFragment : Fragment() {
    private var _binding: FragmentManualJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private val journalsRef = database.getReference("Journals")
    private val usersRef = database.getReference("Users")
    private var selectedImageBase64: String? = null

    private val tag = "ManualJournalFragment"

    // HTTP Client with longer timeouts for image upload
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // List of predefined moods
    private val moodList = listOf(
        "angry", "sad", "happy", "neutral",
        "irritated", "excited", "nervous", "tired", "confused"
    )

    // Activity result launcher for image selection
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == androidx.appcompat.app.AppCompatActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    // Resize the bitmap to a reasonable size
                    val width = bitmap.width
                    val height = bitmap.height
                    val maxDimension = 1024

                    var resizedBitmap = bitmap
                    if (width > maxDimension || height > maxDimension) {
                        val ratio = if (width > height) {
                            maxDimension.toFloat() / width
                        } else {
                            maxDimension.toFloat() / height
                        }

                        val newWidth = (width * ratio).toInt()
                        val newHeight = (height * ratio).toInt()

                        resizedBitmap = bitmap.scale(newWidth, newHeight)

                        // Recycle original bitmap if it's different
                        if (resizedBitmap != bitmap) {
                            bitmap.recycle()
                        }
                    }

                    // Convert bitmap to Base64
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()
                    selectedImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

                    // Show image preview
                    binding.inlineImagePreview.setImageBitmap(resizedBitmap)
                    binding.inlineImagePreviewContainer.visibility = View.VISIBLE
                    binding.removeInlineImageButton.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Toast.makeText(context, "Error selecting image: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(tag, "Image selection error", e)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Setup mood dropdown
        setupMoodDropdown()

        // Set up attach image button
        binding.inlineAttachButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        // Set up remove image button
        binding.removeInlineImageButton.setOnClickListener {
            selectedImageBase64 = null
            binding.inlineImagePreview.setImageDrawable(null)
            binding.inlineImagePreviewContainer.visibility = View.GONE
            binding.removeInlineImageButton.visibility = View.GONE
        }

        // Set up save button click listener
        binding.saveEntryButton.setOnClickListener {
            saveJournalEntry()
        }
    }

    private fun setupMoodDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            moodList.map { it.replaceFirstChar { char -> char.uppercase(Locale.ROOT) } }
        )

        (binding.entryMood as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    @SuppressLint("SetTextI18n")
    private fun saveJournalEntry() {
        val title = binding.entryTitle.text.toString().trim()
        val mood = binding.entryMood.text.toString().trim().lowercase(Locale.getDefault())
        val content = binding.entryContent.text.toString().trim()
        val isPublic = binding.privacySwitch.isChecked

        // Basic validation
        if (title.isEmpty()) {
            binding.entryTitle.error = "Title is required"
            return
        }

        if (mood.isEmpty()) {
            binding.entryMood.error = "Mood is required"
            return
        }

        if (mood !in moodList) {
            binding.entryMood.error = "Please select a valid mood"
            return
        }

        if (content.isEmpty()) {
            binding.entryContent.error = "Content is required"
            return
        }

        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to save entries", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable save button to prevent multiple submissions
        binding.saveEntryButton.isEnabled = false
        binding.saveEntryButton.text = "Saving..."

        // Get the user's data from Firebase database
        getUsernameFromDatabase(currentUser.uid) { userName ->
            // Generate a unique key for this entry
            val entryId = journalsRef.push().key

            if (entryId == null) {
                Toast.makeText(context, "Error creating entry", Toast.LENGTH_SHORT).show()
                binding.saveEntryButton.isEnabled = true
                binding.saveEntryButton.text = "Save Entry"
                return@getUsernameFromDatabase
            }

            // Get user data
            val userId = currentUser.uid

            // Upload image if exists
            if (selectedImageBase64 != null) {
                uploadImage(entryId, selectedImageBase64!!) { imageUrl ->
                    saveJournalToFirebase(entryId, userId, userName, title, content, mood, isPublic, imageUrl)
                }
            } else {
                saveJournalToFirebase(entryId, userId, userName, title, content, mood, isPublic, "")
            }
        }
    }

    private fun uploadImage(entryId: String, imageBase64: String, callback: (String) -> Unit) {
        val json = JSONObject().apply {
            put("entryId", entryId)
            put("imageBase64", imageBase64)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://192.168.155.103/mydiary_api/upload_image.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e(tag, "Upload failed", e)

                    // Even if image upload fails, proceed with saving the entry to Firebase without image
                    Log.d(tag, "Continuing to save entry without image")
                    val title = binding.entryTitle.text.toString().trim()
                    val mood = binding.entryMood.text.toString().trim().lowercase(Locale.getDefault())
                    val content = binding.entryContent.text.toString().trim()
                    val isPublic = binding.privacySwitch.isChecked
                    saveJournalToFirebase(entryId, auth.currentUser?.uid ?: "",
                        auth.currentUser?.displayName ?: "Anonymous", title, content, mood, isPublic, "")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                requireActivity().runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            Log.d(tag, "Response: $responseBody")
                            val jsonResponse = JSONObject(responseBody)
                            if (jsonResponse.has("error")) {
                                Toast.makeText(context, "Upload error: ${jsonResponse.getString("error")}", Toast.LENGTH_LONG).show()
                                Log.e(tag, "Server error: ${jsonResponse.getString("error")}")

                                // Even if server returns error, proceed with saving entry
                                val title = binding.entryTitle.text.toString().trim()
                                val mood = binding.entryMood.text.toString().trim().lowercase(Locale.getDefault())
                                val content = binding.entryContent.text.toString().trim()
                                val isPublic = binding.privacySwitch.isChecked
                                saveJournalToFirebase(entryId, auth.currentUser?.uid ?: "",
                                    auth.currentUser?.displayName ?: "Anonymous", title, content, mood, isPublic, "")
                            } else {
                                val imageUrl = jsonResponse.getString("imageUrl")
                                callback(imageUrl)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error processing response: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e(tag, "Response parsing error", e)

                            // Continue with saving entry
                            val title = binding.entryTitle.text.toString().trim()
                            val mood = binding.entryMood.text.toString().trim().lowercase(Locale.getDefault())
                            val content = binding.entryContent.text.toString().trim()
                            val isPublic = binding.privacySwitch.isChecked
                            saveJournalToFirebase(entryId, auth.currentUser?.uid ?: "",
                                auth.currentUser?.displayName ?: "Anonymous", title, content, mood, isPublic, "")
                        }
                    } else {
                        Toast.makeText(context, "Image upload failed: HTTP ${response.code}", Toast.LENGTH_LONG).show()
                        Log.e(tag, "Upload failed: HTTP ${response.code}, Body: $responseBody")

                        // Continue with saving entry
                        val title = binding.entryTitle.text.toString().trim()
                        val mood = binding.entryMood.text.toString().trim().lowercase(Locale.getDefault())
                        val content = binding.entryContent.text.toString().trim()
                        val isPublic = binding.privacySwitch.isChecked
                        saveJournalToFirebase(entryId, auth.currentUser?.uid ?: "",
                            auth.currentUser?.displayName ?: "Anonymous", title, content, mood, isPublic, "")
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun saveJournalToFirebase(
        entryId: String,
        userId: String,
        userName: String,
        title: String,
        content: String,
        mood: String,
        isPublic: Boolean,
        imageUrl: String
    ) {
        // Create journal entry object
        val journalEntry = JournalEntry(
            id = entryId,
            userId = userId,
            userName = userName,
            title = title,
            content = content,
            mood = mood,
            timestamp = System.currentTimeMillis(),
            likes = mapOf(),
            public = isPublic,
            imageUrl = imageUrl
        )

        // Save to Firebase Realtime Database
        journalsRef.child(entryId).setValue(journalEntry)
            .addOnSuccessListener {
                Toast.makeText(context, "Journal entry saved successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_home)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving entry: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(tag, "Firebase save error", e)
                binding.saveEntryButton.isEnabled = true
                binding.saveEntryButton.text = "Save Entry"
            }
    }

    private fun getUsernameFromDatabase(userId: String, callback: (String) -> Unit) {
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)

                if (!username.isNullOrEmpty()) {
                    callback(username)
                } else {
                    val name = snapshot.child("name").getValue(String::class.java)

                    if (!name.isNullOrEmpty()) {
                        callback(name)
                    } else {
                        val email = auth.currentUser?.email?.substringBefore('@') ?: "Anonymous"
                        callback(email)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                val fallbackName = auth.currentUser?.email?.substringBefore('@') ?: "Anonymous"
                callback(fallbackName)
                Log.e(tag, "Username fetch cancelled", error.toException())
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}