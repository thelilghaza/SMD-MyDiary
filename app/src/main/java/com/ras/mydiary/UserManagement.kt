package com.ras.mydiary

import User
import android.app.AlertDialog
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.graphics.BitmapFactory

class UserManagement : AppCompatActivity() {

    private lateinit var journalersRecyclerView: RecyclerView
    private lateinit var therapistsRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var journalersEmptyText: TextView
    private lateinit var therapistsEmptyText: TextView
    private lateinit var loadingView: View
    private lateinit var usersDatabase: DatabaseReference
    private lateinit var journalsDatabase: DatabaseReference

    private lateinit var journalersList: MutableList<User>
    private lateinit var therapistsList: MutableList<User>
    private lateinit var journalersAdapter: UserAdapter
    private lateinit var therapistsAdapter: UserAdapter

    // Track ongoing deletions
    private var isDeletingUser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        // Initialize UI components
        journalersRecyclerView = findViewById(R.id.recyclerView_journalers)
        therapistsRecyclerView = findViewById(R.id.recyclerView_therapists)
        searchView = findViewById(R.id.searchView_users)
        journalersEmptyText = findViewById(R.id.text_no_journalers)
        therapistsEmptyText = findViewById(R.id.text_no_therapists)
        loadingView = findViewById(R.id.loading_view)

        // Initialize the toolbar with back button
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "User Management"

        // Set up the RecyclerViews
        journalersRecyclerView.layoutManager = LinearLayoutManager(this)
        therapistsRecyclerView.layoutManager = LinearLayoutManager(this)

        journalersList = mutableListOf()
        therapistsList = mutableListOf()

        journalersAdapter = UserAdapter(journalersList, UserType.JOURNALER) { user ->
            showDeleteConfirmationDialog(user)
        }

        therapistsAdapter = UserAdapter(therapistsList, UserType.THERAPIST) { user ->
            showDeleteConfirmationDialog(user)
        }

        journalersRecyclerView.adapter = journalersAdapter
        therapistsRecyclerView.adapter = therapistsAdapter

        // Initialize Firebase Database references
        usersDatabase = FirebaseDatabase.getInstance().getReference("Users")
        journalsDatabase = FirebaseDatabase.getInstance().getReference("Journals")

        // Show loading view
        showLoading(true)

        // Load users data
        loadUsers()

        // Set up search functionality
        setupSearch()
    }

    private fun loadUsers() {
        // Load all users, then separate into journalers and therapists
        usersDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isDeletingUser) {
                    // Skip processing if we're in the middle of a deletion to avoid UI flicker
                    return
                }

                journalersList.clear()
                therapistsList.clear()

                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        when (it.role) {
                            "user" -> journalersList.add(it)
                            "therapist" -> therapistsList.add(it)
                        }
                    }
                }

                // Sort users alphabetically by username
                journalersList.sortBy { it.username }
                therapistsList.sortBy { it.username }

                // Update the adapters
                journalersAdapter.notifyDataSetChanged()
                therapistsAdapter.notifyDataSetChanged()

                // Hide loading view
                showLoading(false)

                // Show or hide empty state texts
                journalersEmptyText.visibility = if (journalersList.isEmpty()) View.VISIBLE else View.GONE
                therapistsEmptyText.visibility = if (therapistsList.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserManagement,
                    "Error loading users: ${error.message}",
                    Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        })
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText)
                return true
            }
        })
    }

    private fun filterUsers(query: String?) {
        if (query.isNullOrEmpty()) {
            // If query is empty, show all users
            journalersAdapter.updateUsers(journalersList)
            therapistsAdapter.updateUsers(therapistsList)
        } else {
            // Filter journalers
            val filteredJournalers = journalersList.filter {
                it.username.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
            journalersAdapter.updateUsers(filteredJournalers)

            // Filter therapists
            val filteredTherapists = therapistsList.filter {
                it.username.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
            therapistsAdapter.updateUsers(filteredTherapists)

            // Show or hide empty state texts
            journalersEmptyText.visibility = if (filteredJournalers.isEmpty()) View.VISIBLE else View.GONE
            therapistsEmptyText.visibility = if (filteredTherapists.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        journalersRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        therapistsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showDeleteConfirmationDialog(user: User) {
        val message = if (user.role == "user") {
            "Are you sure you want to delete ${user.name}'s account? This will also delete all their journal entries. This action cannot be undone."
        } else {
            "Are you sure you want to delete ${user.name}'s account? This action cannot be undone."
        }

        AlertDialog.Builder(this)
            .setTitle("Delete User Account")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                deleteUserAccount(user)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteUserAccount(user: User) {
        // Prevent multiple deletions at once and avoid UI flicker
        if (isDeletingUser) {
            return
        }

        isDeletingUser = true
        showProgressDialog("Deleting user account...")

        when (user.role) {
            "user" -> {
                // For journalers, first delete all their journal entries
                deleteAllJournalEntries(user.uid) { success ->
                    if (success) {
                        // Then delete the user data
                        deleteUserData(user)
                    } else {
                        // If there was an error, stop the deletion process
                        dismissProgressDialog()
                        Toast.makeText(this,
                            "Failed to delete journal entries. User deletion cancelled.",
                            Toast.LENGTH_LONG).show()
                        isDeletingUser = false
                    }
                }
            }
            else -> {
                // For non-journalers, just delete the user data
                deleteUserData(user)
            }
        }
    }

    private fun deleteAllJournalEntries(userId: String, callback: (Boolean) -> Unit) {
        // Query for all journal entries by this user
        journalsDatabase.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // No entries to delete
                        callback(true)
                        return
                    }

                    val totalEntries = snapshot.childrenCount
                    var deletedEntries = 0
                    var errorOccurred = false

                    for (journalSnapshot in snapshot.children) {
                        val entryId = journalSnapshot.key ?: continue

                        journalsDatabase.child(entryId).removeValue()
                            .addOnSuccessListener {
                                deletedEntries++

                                // Check if all entries have been processed
                                if (deletedEntries == totalEntries.toInt()) {
                                    callback(!errorOccurred)
                                }
                            }
                            .addOnFailureListener { e ->
                                errorOccurred = true
                                deletedEntries++

                                // Log the error
                                Toast.makeText(this@UserManagement,
                                    "Error deleting entry: ${e.message}",
                                    Toast.LENGTH_SHORT).show()

                                // Check if all entries have been processed
                                if (deletedEntries == totalEntries.toInt()) {
                                    callback(!errorOccurred)
                                }
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserManagement,
                        "Failed to delete journal entries: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            })
    }

    private fun deleteUserData(user: User) {
        // Delete user data from Firebase Realtime Database
        usersDatabase.child(user.uid).removeValue()
            .addOnSuccessListener {
                // If database delete is successful
                dismissProgressDialog()

                Toast.makeText(this,
                    "User ${user.username} has been deleted",
                    Toast.LENGTH_SHORT).show()

                // Reset flag to allow future deletions
                isDeletingUser = false

                // In a real implementation, you would also delete the auth user
                // This requires Firebase Admin SDK or Firebase Functions
                /*
                // Using Firebase Functions
                val functions = FirebaseFunctions.getInstance()
                val data = hashMapOf("uid" to user.uid)

                functions.getHttpsCallable("deleteUser")
                    .call(data)
                    .addOnSuccessListener {
                        Toast.makeText(this,
                            "User authentication record also deleted",
                            Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                            "Auth deletion failed: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                */
            }
            .addOnFailureListener { e ->
                dismissProgressDialog()

                Toast.makeText(this,
                    "User deletion failed: ${e.message}",
                    Toast.LENGTH_SHORT).show()

                // Reset flag to allow future deletions
                isDeletingUser = false
            }
    }

    // Progress dialog for deletion process
    private var progressDialog: AlertDialog? = null

    private fun showProgressDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.entries_delete, null)
        dialogView.findViewById<TextView>(R.id.text_progress_message).text = message

        builder.setView(dialogView)
        builder.setCancelable(false)

        progressDialog = builder.create()
        progressDialog?.show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Enum to distinguish between types of users
    enum class UserType {
        JOURNALER,
        THERAPIST
    }

    // Adapter for displaying users in RecyclerView
    inner class UserAdapter(
        private var users: List<User>,
        private val userType: UserType,
        private val onDeleteClick: (User) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

        fun updateUsers(newUsers: List<User>) {
            users = newUsers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_management, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]
            holder.bind(user)
        }

        override fun getItemCount(): Int = users.size

        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView: MaterialCardView = itemView.findViewById(R.id.card_user)
            private val profileImageView: ImageView = itemView.findViewById(R.id.image_user_profile)
            private val nameTextView: TextView = itemView.findViewById(R.id.text_user_name)
            private val usernameTextView: TextView = itemView.findViewById(R.id.text_user_username)
            private val emailTextView: TextView = itemView.findViewById(R.id.text_user_email)
            private val deleteButton: View = itemView.findViewById(R.id.button_delete_user)

            fun bind(user: User) {
                nameTextView.text = user.name
                usernameTextView.text = "@${user.username}"
                emailTextView.text = user.email

                // Set different card background colors based on user type
                when (userType) {
                    UserType.JOURNALER -> cardView.setCardBackgroundColor(resources.getColor(R.color.journaler_card_bg, null))
                    UserType.THERAPIST -> cardView.setCardBackgroundColor(resources.getColor(R.color.therapist_card_bg, null))
                }

                // Load profile image
                if (user.profileImageUrl.isNotEmpty()) {
                    try {
                        // Try to decode from Base64 if that's how the image is stored
                        val decodedBytes = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        profileImageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        // If Base64 fails, try loading as URL
                        Glide.with(itemView.context)
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.icon_profile_foreground)
                            .circleCrop()
                            .into(profileImageView)
                    }
                } else {
                    // Default image if no profile image
                    profileImageView.setImageResource(R.drawable.icon_profile_foreground)
                }

                // Set click listener for delete button
                deleteButton.setOnClickListener {
                    onDeleteClick(user)
                }
            }
        }
    }
}