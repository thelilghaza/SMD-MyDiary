package com.ras.mydiary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ras.mydiary.databinding.FragmentNewEntryBinding
import java.util.Locale

class NewEntryFragment : Fragment() {
    private var _binding: FragmentNewEntryBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private val journalsRef = database.getReference("Journals")
    private val usersRef = database.getReference("Users")

    // List of predefined moods
    private val moodList = listOf(
        "angry", "sad", "happy", "neutral",
        "irritated", "excited", "nervous", "tired", "confused"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Setup mood dropdown
        setupMoodDropdown()

        // Set up save button click listener
        binding.saveEntryButton.setOnClickListener {
            saveJournalEntry()
        }
    }

    private fun setupMoodDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            moodList.map { it.capitalize(Locale.ROOT) }
        )

        (binding.entryMood as? AutoCompleteTextView)?.setAdapter(adapter)
    }

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

            // Create journal entry object
            val journalEntry = JournalEntry(
                id = entryId,
                userId = userId,
                userName = userName,
                title = title,
                content = content,
                mood = mood,
                timestamp = System.currentTimeMillis(),
                likes = mapOf(), // Initialize with empty likes map
                isPublic = isPublic
            )

            // Save to Firebase Realtime Database
            journalsRef.child(entryId).setValue(journalEntry)
                .addOnSuccessListener {
                    Toast.makeText(context, "Journal entry saved successfully", Toast.LENGTH_SHORT).show()

                    // Navigate back to home fragment
                    findNavController().navigate(R.id.nav_home)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.saveEntryButton.isEnabled = true
                    binding.saveEntryButton.text = "Save Entry"
                }
        }
    }

    private fun getUsernameFromDatabase(userId: String, callback: (String) -> Unit) {
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // First try to get username from the database
                val username = snapshot.child("username").getValue(String::class.java)

                if (!username.isNullOrEmpty()) {
                    callback(username)
                } else {
                    // Try to get name as fallback
                    val name = snapshot.child("name").getValue(String::class.java)

                    if (!name.isNullOrEmpty()) {
                        callback(name)
                    } else {
                        // Use email or a default name as last resort
                        val email = auth.currentUser?.email?.substringBefore('@') ?: "Anonymous"
                        callback(email)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // If there's an error, use a fallback username
                val fallbackName = auth.currentUser?.email?.substringBefore('@') ?: "Anonymous"
                callback(fallbackName)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}