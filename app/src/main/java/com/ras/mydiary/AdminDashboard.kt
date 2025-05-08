package com.ras.mydiary

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Base64
import android.graphics.BitmapFactory
import android.widget.Toast
import com.bumptech.glide.Glide

public class AdminDashboard : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var drawerLayout: DrawerLayout

    // UI elements
    private lateinit var journalersCountText: TextView
    private lateinit var therapistsCountText: TextView
    private lateinit var recentFeedbackText: TextView
    private lateinit var moodTrendText: TextView
    private lateinit var viewJournalersButton: Button
    private lateinit var viewTherapistsButton: Button

    companion object {
        private const val ADMIN_SETTINGS_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference()

        // Set up toolbar and navigation drawer
        setupToolbarAndDrawer()

        // Initialize UI elements
        initializeUIElements()

        // Load data from Firebase
        loadUserStatistics()
        loadRecentFeedback()
        loadMoodTrends()

        loadAdminDataInNavHeader()

        // Set click listeners for buttons
        setButtonListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADMIN_SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            // Reload admin data in navigation header
            loadAdminDataInNavHeader()
        }
    }

    private fun setupToolbarAndDrawer() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val menuButton = findViewById<ImageButton>(R.id.hamburger_icon)
        menuButton.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END)
            } else {
                drawerLayout.closeDrawer(GravityCompat.END)
            }
        }
    }

    private fun initializeUIElements() {
        // Find and initialize all UI elements
        journalersCountText = findViewById(R.id.text_journalers_count)
        therapistsCountText = findViewById(R.id.text_therapists_count)
        recentFeedbackText = findViewById(R.id.text_recent_feedback)
        moodTrendText = findViewById(R.id.text_mood_trend)
        viewJournalersButton = findViewById(R.id.view_journalers_button)
        viewTherapistsButton = findViewById(R.id.view_therapists_button)
    }

    private fun loadUserStatistics() {
        // Query for user counts by role
        val usersRef = database.child("Users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var journalersCount = 0
                var therapistsCount = 0

                for (userSnapshot in snapshot.children) {
                    val role = userSnapshot.child("role").getValue(String::class.java)
                    when (role) {
                        "user" -> journalersCount++
                        "therapist" -> therapistsCount++
                    }
                }

                // Update UI with counts
                journalersCountText.text = "Journalers: $journalersCount"
                therapistsCountText.text = "Therapists: $therapistsCount"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                journalersCountText.text = "Journalers: --"
                therapistsCountText.text = "Therapists: --"
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun loadRecentFeedback() {
        // For now, show placeholder data since the Feedback node may not exist yet
        recentFeedbackText.text = "• User1: App is very helpful (Rating: 5/5)\n\n" +
                "• User2: Would like more journal prompts (Rating: 4/5)\n\n" +
                "• User3: Love the mood tracking feature (Rating: 5/5)"

        // In a real implementation, you would fetch from the Feedback node:
        /*
        val feedbackRef = database.child("Feedback")
        feedbackRef.orderByChild("timestamp").limitToLast(3)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val feedbackBuilder = StringBuilder()

                        for (feedbackSnapshot in snapshot.children) {
                            val userId = feedbackSnapshot.child("userId").getValue(String::class.java) ?: "Unknown"
                            val message = feedbackSnapshot.child("message").getValue(String::class.java) ?: "No message"
                            val rating = feedbackSnapshot.child("rating").getValue(Int::class.java) ?: 0

                            feedbackBuilder.append("• User $userId: $message (Rating: $rating/5)\n\n")
                        }

                        if (feedbackBuilder.isNotEmpty()) {
                            recentFeedbackText.text = feedbackBuilder.toString()
                        } else {
                            recentFeedbackText.text = "No recent feedback"
                        }
                    } else {
                        recentFeedbackText.text = "No feedback available"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    recentFeedbackText.text = "Error loading feedback"
                }
            })
        */
    }

    private fun loadMoodTrends() {
        // Query for mood trends across all journal entries
        val journalsRef = database.child("Journals")

        journalsRef.orderByChild("timestamp").startAt(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000.0)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Map to store mood counts
                    val moodCounts = mutableMapOf<String, Int>()
                    var totalEntries = 0

                    for (journalSnapshot in snapshot.children) {
                        val mood = journalSnapshot.child("mood").getValue(String::class.java)
                        if (!mood.isNullOrEmpty()) {
                            moodCounts[mood] = moodCounts.getOrDefault(mood, 0) + 1
                            totalEntries++
                        }
                    }

                    // Sort moods by frequency (descending)
                    val sortedMoods = moodCounts.entries.sortedByDescending { it.value }

                    // Build the mood trend text
                    val moodTrendBuilder = StringBuilder()

                    if (sortedMoods.isNotEmpty() && totalEntries > 0) {
                        // Display top 3 moods
                        val topMoods = sortedMoods.take(3)
                        for (moodEntry in topMoods) {
                            val percentage = (moodEntry.value.toFloat() / totalEntries * 100).toInt()
                            moodTrendBuilder.append("${moodEntry.key}: $percentage%\n")
                        }
                    } else {
                        // If no data found, show placeholder data
                        moodTrendBuilder.append("Happy: 45%\n")
                        moodTrendBuilder.append("Peaceful: 30%\n")
                        moodTrendBuilder.append("Anxious: 15%\n")
                    }

                    // Update UI with mood trends
                    moodTrendText.text = moodTrendBuilder.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    // If error, show placeholder data
                    moodTrendText.text = "Happy: 45%\nPeaceful: 30%\nAnxious: 15%"
                }
            })
    }

    private fun setButtonListeners() {
        // For now, just show Toast messages - will implement actual navigation later
        viewJournalersButton.setOnClickListener {

             val intent = Intent(this, ViewJournalersList::class.java)
             startActivity(intent)
        }

        viewTherapistsButton.setOnClickListener {
            // You can uncomment this when TherapistsListActivity is ready
             val intent = Intent(this, ViewTherapistsList::class.java)
             startActivity(intent)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks
        when (item.itemId) {
            R.id.nav_admin_dashboard -> {
                // Already on dashboard, do nothing
            }
            R.id.nav_users_management -> {
                // Navigate to user management (implementation pending)
                val intent = Intent(this, UserManagement::class.java)
                startActivity(intent)
            }
            R.id.nav_content_moderation -> {
                // Navigate to content moderation (implementation pending)
                val intent = Intent(this, ContentModeration::class.java)
                startActivity(intent)
            }
            R.id.nav_analytics -> {
                // Navigate to analytics (implementation pending)
                Toast.makeText(this, "Analytics coming soon", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_admin_settings -> {
                // Navigate to admin settings (implementation pending)
                val intent = Intent(this, AdminSettings::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                // Logout functionality
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.END)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    // Add this method to your AdminActivity.kt class
    private fun loadAdminDataInNavHeader() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView_profile)
        val usernameTextView = headerView.findViewById<TextView>(R.id.textView_username)
        val emailTextView = headerView.findViewById<TextView>(R.id.textView_email)

        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val database = FirebaseDatabase.getInstance()
            val uid = user.uid

            val userRef = database.getReference("Users").child(uid)
            userRef.get().addOnSuccessListener { dataSnapshot ->
                val name = dataSnapshot.child("name").getValue(String::class.java)
                val username = dataSnapshot.child("username").getValue(String::class.java)
                val profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String::class.java)

                // Set username and email
                usernameTextView.text = name ?: username ?: "Admin"
                emailTextView.text = user.email ?: "admin@example.com"

                // Load the profile image using Glide or from Base64
                if (!profileImageUrl.isNullOrEmpty()) {
                    try {
                        // Assuming profileImageUrl is Base64
                        val decodedBytes = Base64.decode(profileImageUrl, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        profileImageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        // Fallback to a default image if decoding fails
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.icon_profile_foreground)
                            .circleCrop()
                            .into(profileImageView)
                    }
                } else {
                    // Use default profile image if no URL is available
                    profileImageView.setImageResource(R.drawable.icon_profile_foreground)
                }
            }.addOnFailureListener {
                // Handle failure
                profileImageView.setImageResource(R.drawable.icon_profile_foreground)
                usernameTextView.text = "Admin"
                emailTextView.text = user.email ?: "admin@example.com"
            }
        }
    }
}