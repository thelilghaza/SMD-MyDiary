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

class ViewTherapistsList : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var noUsersFoundText: TextView
    private lateinit var loadingView: View
    private lateinit var database: DatabaseReference
    private lateinit var therapistsList: MutableList<User>
    private lateinit var adapter: TherapistsAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_therapists_list)

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView_therapists)
        searchView = findViewById(R.id.searchView_therapists)
        noUsersFoundText = findViewById(R.id.text_no_users_found)
        loadingView = findViewById(R.id.loading_view)

        // Initialize the back button in the toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Therapists"

        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        therapistsList = mutableListOf()
        adapter = TherapistsAdapter(therapistsList)
        recyclerView.adapter = adapter

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Show loading view
        showLoading(true)

        // Load therapists data
        loadTherapists()

        // Set up search functionality
        setupSearch()
    }

    private fun loadTherapists() {
        // Query for users with role "therapist"
        database.orderByChild("role").equalTo("therapist")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    therapistsList.clear()

                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        user?.let {
                            therapistsList.add(it)
                        }
                    }

                    // Sort users alphabetically by username
                    therapistsList.sortBy { it.username }

                    // Update the adapter
                    adapter.notifyDataSetChanged()

                    // Hide loading view
                    showLoading(false)

                    // Show or hide "No users found" text
                    if (therapistsList.isEmpty()) {
                        noUsersFoundText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        noUsersFoundText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ViewTherapistsList,
                        "Error loading therapists: ${error.message}",
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
                filterTherapists(newText)
                return true
            }
        })
    }

    private fun filterTherapists(query: String?) {
        if (query.isNullOrEmpty()) {
            // If query is empty, show all therapists
            adapter.updateTherapists(therapistsList)
        } else {
            // Filter therapists by username, name, or email containing the query
            val filteredList = therapistsList.filter {
                it.username.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
            adapter.updateTherapists(filteredList)

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

    // Adapter for displaying therapists in RecyclerView
    inner class TherapistsAdapter(private var therapists: List<User>) :
        RecyclerView.Adapter<TherapistsAdapter.TherapistViewHolder>() {

        fun updateTherapists(newTherapists: List<User>) {
            therapists = newTherapists
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TherapistViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_view_therapist, parent, false)
            return TherapistViewHolder(view)
        }

        override fun onBindViewHolder(holder: TherapistViewHolder, position: Int) {
            val therapist = therapists[position]
            holder.bind(therapist)
        }

        override fun getItemCount(): Int = therapists.size

        inner class TherapistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView: MaterialCardView = itemView.findViewById(R.id.card_therapist)
            private val profileImageView: ImageView = itemView.findViewById(R.id.image_therapist_profile)
            private val nameTextView: TextView = itemView.findViewById(R.id.text_therapist_name)
            private val usernameTextView: TextView = itemView.findViewById(R.id.text_therapist_username)
            private val emailTextView: TextView = itemView.findViewById(R.id.text_therapist_email)
            private val phoneTextView: TextView = itemView.findViewById(R.id.text_therapist_phone)
            private val clientCountTextView: TextView = itemView.findViewById(R.id.text_clients_count)
            private val specializationTextView: TextView = itemView.findViewById(R.id.text_therapist_specialization)

            fun bind(user: User) {
                nameTextView.text = user.name
                usernameTextView.text = "@${user.username}"
                emailTextView.text = user.email
                phoneTextView.text = user.phone

                // For now, we'll set a placeholder specialization
                // In a real app, this would come from the user data
                specializationTextView.text = "General Therapy"

                // Load client count (users who have had sessions with this therapist)
                loadClientCount(user.uid)

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
                    // Handle therapist item click - could navigate to a detail view or show options
                    Toast.makeText(itemView.context,
                        "Selected: ${user.username}",
                        Toast.LENGTH_SHORT).show()
                }
            }

            private fun loadClientCount(therapistId: String) {
                // In a real app, you would query a "Sessions" or similar node
                // For now, we'll use a random number between 5-20 for demonstration
                val clientCount = (5..20).random()
                clientCountTextView.text = "$clientCount active clients"

                // Alternatively, you could implement actual client counting like this:
                /*
                val sessionsRef = FirebaseDatabase.getInstance().getReference("Sessions")
                sessionsRef.orderByChild("therapistId").equalTo(therapistId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // Count unique client IDs
                            val uniqueClientIds = mutableSetOf<String>()
                            for (sessionSnapshot in snapshot.children) {
                                val clientId = sessionSnapshot.child("clientId").getValue(String::class.java)
                                if (!clientId.isNullOrEmpty()) {
                                    uniqueClientIds.add(clientId)
                                }
                            }

                            val clientCount = uniqueClientIds.size
                            clientCountTextView.text = "$clientCount active clients"
                        }

                        override fun onCancelled(error: DatabaseError) {
                            clientCountTextView.text = "-- active clients"
                        }
                    })
                */
            }
        }
    }
}