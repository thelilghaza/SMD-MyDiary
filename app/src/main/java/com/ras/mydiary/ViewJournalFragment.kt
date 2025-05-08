package com.ras.mydiary

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class ViewJournalFragment : Fragment() {

    private val TAG = "ViewJournalFragment"
    private val API_BASE_URL = "http://192.168.100.69/mydiary_api"

    private lateinit var journalId: String
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var journalListener: ValueEventListener? = null

    private lateinit var usernameTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var contentTextView: TextView
    private lateinit var moodTextView: TextView
    private lateinit var moodEditText: EditText
    private lateinit var timestampTextView: TextView
    private lateinit var likeButton: ImageButton
    private lateinit var likeCountTextView: TextView
    private lateinit var editButton: Button
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button
    private lateinit var imageView: ImageView

    private var isOwner = false
    private var isEditing = false
    private var isLiked = false

    // Journal entry data
    private var currentJournal: JournalEntry? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference("Journals")
        auth = FirebaseAuth.getInstance()

        // Initialize views
        usernameTextView = view.findViewById(R.id.detail_username)
        titleTextView = view.findViewById(R.id.detail_title)
        titleEditText = view.findViewById(R.id.detail_title_edit)
        contentEditText = view.findViewById(R.id.detail_content_edit)
        contentTextView = view.findViewById(R.id.detail_content)
        moodTextView = view.findViewById(R.id.detail_mood)
        moodEditText = view.findViewById(R.id.detail_mood_edit)
        timestampTextView = view.findViewById(R.id.detail_timestamp)
        likeButton = view.findViewById(R.id.like_button)
        likeCountTextView = view.findViewById(R.id.like_count)
        editButton = view.findViewById(R.id.edit_button)
        saveButton = view.findViewById(R.id.save_button)
        deleteButton = view.findViewById(R.id.delete_button)
        imageView = view.findViewById(R.id.detail_image)

        // Get journal ID from arguments
        arguments?.let {
            journalId = it.getString("journal_id", "")
            if (journalId.isNotEmpty()) {
                loadJournalDetails()
            } else {
                Toast.makeText(context, "Journal not found", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        // Set click listeners
        likeButton.setOnClickListener {
            toggleLike()
        }

        editButton.setOnClickListener {
            toggleEditMode(true)
        }

        saveButton.setOnClickListener {
            saveJournalChanges()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Remove the value event listener when fragment is destroyed
        if (journalId.isNotEmpty() && journalListener != null) {
            database.child(journalId).removeEventListener(journalListener!!)
        }
    }

    private fun loadJournalDetails() {
        // Create a new listener
        journalListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if journal still exists
                if (!snapshot.exists()) {
                    Toast.makeText(context, "Journal entry no longer exists", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                    return
                }

                val journal = snapshot.getValue(JournalEntry::class.java)
                journal?.let {
                    currentJournal = it

                    // Display journal details
                    usernameTextView.text = it.userName
                    titleTextView.text = it.title
                    titleEditText.setText(it.title)
                    contentTextView.text = it.content
                    contentEditText.setText(it.content)
                    moodTextView.text = it.mood
                    moodEditText.setText(it.mood)

                    // Load image using the fetch_image.php API
                    if (journalId.isNotEmpty()) {
                        val imageUrl = "$API_BASE_URL/fetch_image.php?entryId=$journalId"

                        try {
                            Glide.with(requireContext())
                                .load(imageUrl)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .into(imageView)

                            imageView.visibility = View.VISIBLE
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading image: ${e.message}")
                            imageView.visibility = View.GONE
                        }
                    } else {
                        imageView.visibility = View.GONE
                    }

                    // Format and display timestamp
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    timestampTextView.text = dateFormat.format(Date(it.timestamp))

                    // Check if current user is the owner of this journal
                    val currentUser = auth.currentUser
                    isOwner = currentUser != null && it.userId == currentUser.uid

                    // Show/hide edit and delete buttons based on ownership
                    editButton.visibility = if (isOwner) View.VISIBLE else View.GONE
                    deleteButton.visibility = if (isOwner) View.VISIBLE else View.GONE

                    // Check if current user has liked this journal
                    if (currentUser != null) {
                        isLiked = it.likes[currentUser.uid] == true
                        updateLikeButton()
                    }

                    // Update like count
                    val likeCount = it.likes.count { entry -> entry.value }
                    likeCountTextView.text = likeCount.toString()

                    // Initial UI setup
                    toggleEditMode(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Database error: ${error.message}")
            }
        }

        // Add the listener
        database.child(journalId).addValueEventListener(journalListener!!)
    }

    private fun toggleLike() {
        val currentUser = auth.currentUser ?: return

        val journalRef = database.child(journalId)
        val likeRef = journalRef.child("likes").child(currentUser.uid)

        if (isLiked) {
            // Unlike - update UI first for responsiveness
            isLiked = false
            updateLikeButton()

            // Update like count immediately for better UX
            val currentCount = likeCountTextView.text.toString().toIntOrNull() ?: 0
            if (currentCount > 0) {
                likeCountTextView.text = (currentCount - 1).toString()
            }

            // Then update Firebase
            likeRef.removeValue().addOnFailureListener { e ->
                // Revert UI on failure
                isLiked = true
                updateLikeButton()
                Toast.makeText(context, "Failed to unlike: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to unlike: ${e.message}")
            }
        } else {
            // Like - update UI first for responsiveness
            isLiked = true
            updateLikeButton()

            // Update like count immediately for better UX
            val currentCount = likeCountTextView.text.toString().toIntOrNull() ?: 0
            likeCountTextView.text = (currentCount + 1).toString()

            // Then update Firebase
            likeRef.setValue(true).addOnFailureListener { e ->
                // Revert UI on failure
                isLiked = false
                updateLikeButton()
                Toast.makeText(context, "Failed to like: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to like: ${e.message}")
            }
        }
    }

    private fun updateLikeButton() {
        likeButton.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun toggleEditMode(editing: Boolean) {
        isEditing = editing

        titleTextView.visibility = if (editing) View.GONE else View.VISIBLE
        titleEditText.visibility = if (editing) View.VISIBLE else View.GONE
        contentTextView.visibility = if (editing) View.GONE else View.VISIBLE
        contentEditText.visibility = if (editing) View.VISIBLE else View.GONE
        moodTextView.visibility = if (editing) View.GONE else View.VISIBLE
        moodEditText.visibility = if (editing) View.VISIBLE else View.GONE
        imageView.visibility = if (editing) View.GONE else View.VISIBLE

        editButton.visibility = if (editing) View.GONE else (if (isOwner) View.VISIBLE else View.GONE)
        saveButton.visibility = if (editing) View.VISIBLE else View.GONE
        deleteButton.visibility = if (editing) View.GONE else (if (isOwner) View.VISIBLE else View.GONE)
    }

    private fun saveJournalChanges() {
        val updatedTitle = titleEditText.text.toString().trim()
        val updatedContent = contentEditText.text.toString().trim()
        val updatedMood = moodEditText.text.toString().trim()

        if (updatedTitle.isEmpty()) {
            titleEditText.error = "Title cannot be empty"
            return
        }

        if (updatedContent.isEmpty()) {
            contentEditText.error = "Content cannot be empty"
            return
        }

        if (updatedMood.isEmpty()) {
            moodEditText.error = "Mood cannot be empty"
            return
        }

        // Show loading state
        saveButton.isEnabled = false
        saveButton.text = "Saving..."

        // Update journal in Firebase
        val updates = HashMap<String, Any>()
        updates["title"] = updatedTitle
        updates["content"] = updatedContent
        updates["mood"] = updatedMood

        database.child(journalId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Journal updated successfully", Toast.LENGTH_SHORT).show()

                // Update UI immediately for better UX
                titleTextView.text = updatedTitle
                contentTextView.text = updatedContent
                moodTextView.text = updatedMood

                toggleEditMode(false)

                // Restore button state
                saveButton.isEnabled = true
                saveButton.text = "Save"
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to update journal: ${e.message}")

                // Restore button state
                saveButton.isEnabled = true
                saveButton.text = "Save"
            }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Journal")
            .setMessage("Are you sure you want to delete this journal entry? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteJournal()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteJournal() {
        // Disable buttons to prevent further interaction
        deleteButton.isEnabled = false
        deleteButton.text = "Deleting..."
        editButton.isEnabled = false

        // Remove the event listener to prevent unwanted callbacks
        if (journalListener != null) {
            database.child(journalId).removeEventListener(journalListener!!)
            journalListener = null
        }

        // Show a loading message
        Toast.makeText(context, "Deleting entry...", Toast.LENGTH_SHORT).show()

        // First, delete the journal from Firebase
        database.child(journalId).removeValue()
            .addOnSuccessListener {
                // After successful Firebase deletion, delete the image via API
                deleteImageFromServer {
                    Toast.makeText(context, "Journal deleted successfully", Toast.LENGTH_SHORT).show()
                    // Navigate back to home fragment ONLY on successful deletion
                    navigateToHome()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete journal: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to delete journal: ${e.message}")

                // Re-enable buttons
                deleteButton.isEnabled = true
                deleteButton.text = "Delete"
                editButton.isEnabled = true

                // Reattach listener
                loadJournalDetails()
            }
    }

    private fun deleteImageFromServer(onComplete: () -> Unit) {
        // Run the network request in a background thread
        Executors.newSingleThreadExecutor().execute {
            try {
                val url = URL("$API_BASE_URL/delete_image.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Prepare JSON payload
                val jsonInputString = JSONObject().apply {
                    put("entryId", journalId)
                }.toString()

                // Send the request
                connection.outputStream.use { outputStream ->
                    outputStream.write(jsonInputString.toByteArray())
                    outputStream.flush()
                }

                // Check response code
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.optBoolean("success", false)) {
                        Log.d(TAG, "Image deleted successfully: ${jsonResponse.getString("message")}")
                    } else {
                        Log.e(TAG, "Failed to delete image: ${jsonResponse.optString("message", "Unknown error")}")
                    }
                } else {
                    Log.e(TAG, "Failed to delete image: HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image: ${e.message}")
            } finally {
                // Run onComplete callback on the main thread
                activity?.runOnUiThread {
                    onComplete()
                }
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.nav_home)
    }
}