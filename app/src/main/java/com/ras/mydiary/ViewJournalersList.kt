package com.ras.mydiary

import User
import android.annotation.SuppressLint
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
import com.google.firebase.database.*
import android.graphics.BitmapFactory

class ViewJournalersList : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var noUsersFoundText: TextView
    private lateinit var loadingView: View
    private lateinit var database: DatabaseReference
    private lateinit var journalersList: MutableList<User>
    private lateinit var adapter: JournalersAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journalers_list)

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView_journalers)
        searchView = findViewById(R.id.searchView_journalers)
        noUsersFoundText = findViewById(R.id.text_no_users_found)
        loadingView = findViewById(R.id.loading_view)

        // Initialize the back button in the toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Journalers"

        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        journalersList = mutableListOf()
        adapter = JournalersAdapter(journalersList)
        recyclerView.adapter = adapter

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Show loading view
        showLoading(true)

        // Load journalers data
        loadJournalers()

        // Set up search functionality
        setupSearch()
    }

    private fun loadJournalers() {
        // Query for users with role "user"
        database.orderByChild("role").equalTo("user")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    journalersList.clear()

                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        user?.let {
                            journalersList.add(it)
                        }
                    }

                    // Sort users alphabetically by username
                    journalersList.sortBy { it.username }

                    // Update the adapter
                    adapter.notifyDataSetChanged()

                    // Hide loading view
                    showLoading(false)

                    // Show or hide "No users found" text
                    if (journalersList.isEmpty()) {
                        noUsersFoundText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        noUsersFoundText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ViewJournalersList,
                        "Error loading journalers: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    noUsersFoundText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            })
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterJournalers(newText)
                return true
            }
        })
    }

    private fun filterJournalers(query: String?) {
        if (query.isNullOrEmpty()) {
            // If query is empty, show all journalers
            adapter.updateJournalers(journalersList)
        } else {
            // Filter journalers by username, name, or email containing the query
            val filteredList = journalersList.filter {
                it.username.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
            adapter.updateJournalers(filteredList)

            // Show or hide "No users found" text based on filtered results
            if (filteredList.isEmpty()) {
                noUsersFoundText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                noUsersFoundText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Adapter for displaying journalers in RecyclerView
    inner class JournalersAdapter(private var journalers: List<User>) :
        RecyclerView.Adapter<JournalersAdapter.JournalerViewHolder>() {

        fun updateJournalers(newJournalers: List<User>) {
            journalers = newJournalers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_view_journaler, parent, false)
            return JournalerViewHolder(view)
        }

        override fun onBindViewHolder(holder: JournalerViewHolder, position: Int) {
            val journaler = journalers[position]
            holder.bind(journaler)
        }

        override fun getItemCount(): Int = journalers.size

        inner class JournalerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView: MaterialCardView = itemView.findViewById(R.id.card)
            private val profileImageView: ImageView = itemView.findViewById(R.id.image_journaler)
            private val nameTextView: TextView = itemView.findViewById(R.id.journaler_name)
            private val usernameTextView: TextView = itemView.findViewById(R.id.journaler_username)
            private val emailTextView: TextView = itemView.findViewById(R.id.journaler_email)
            private val phoneTextView: TextView = itemView.findViewById(R.id.text_journaler_phone)
            private val entryCountTextView: TextView = itemView.findViewById(R.id.text_journal_entries_count)

            fun bind(user: User) {
                nameTextView.text = user.name
                usernameTextView.text = "@${user.username}"
                emailTextView.text = user.email
                phoneTextView.text = user.phone

                // For now, load a placeholder entry count (to be replaced with actual count in future)
                loadJournalEntryCount(user.uid)

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

                // Set up ripple effect and click listener for the card
                cardView.setOnClickListener {
                    // Handle journaler item click - could navigate to a detail view or show options
                    Toast.makeText(itemView.context,
                        "Selected: ${user.username}",
                        Toast.LENGTH_SHORT).show()
                }
            }

            private fun loadJournalEntryCount(userId: String) {
                // Query for journal entries by this user
                val journalsRef = FirebaseDatabase.getInstance().getReference("Journals")
                journalsRef.orderByChild("userId").equalTo(userId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val entryCount = snapshot.childrenCount
                            entryCountTextView.text = "$entryCount journal entries"
                        }

                        override fun onCancelled(error: DatabaseError) {
                            entryCountTextView.text = "-- journal entries"
                        }
                    })
            }
        }
    }
}