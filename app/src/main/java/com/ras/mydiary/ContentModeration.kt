package com.ras.mydiary

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ContentModeration : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var noEntriesFoundText: TextView
    private lateinit var loadingView: View
    private lateinit var journalsDb: DatabaseReference
    private lateinit var usersDb: DatabaseReference

    private var allEntries = mutableListOf<JournalEntryWithUser>()
    private var displayedEntries = mutableListOf<JournalEntryWithUser>()
    private lateinit var adapter: JournalEntryAdapter

    // Current filter
    private var currentFilterMode = FilterMode.ALL

    enum class FilterMode {
        ALL,
        PUBLIC_ONLY,
        FLAGGED_ONLY, // For future implementation
        RECENT_ONLY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_moderation)

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView_entries)
        searchView = findViewById(R.id.searchView_entries)
        chipGroup = findViewById(R.id.chipGroup_filters)
        noEntriesFoundText = findViewById(R.id.text_no_entries)
        loadingView = findViewById(R.id.loading_view)

        // Initialize the toolbar with back button
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Content Moderation"

        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = JournalEntryAdapter(displayedEntries) { entry ->
            showDeleteConfirmationDialog(entry)
        }

        recyclerView.adapter = adapter

        // Initialize Firebase Database references
        journalsDb = FirebaseDatabase.getInstance().getReference("Journals")
        usersDb = FirebaseDatabase.getInstance().getReference("Users")

        // Show loading view
        showLoading(true)

        // Set up filter chips
        setupFilterChips()

        // Set up search functionality
        setupSearch()

        // Load journal entries
        loadJournalEntries()
    }

    private fun setupFilterChips() {
        // Set up filter chip group
        val allChip = findViewById<Chip>(R.id.chip_all)
        val publicChip = findViewById<Chip>(R.id.chip_public)
        val recentChip = findViewById<Chip>(R.id.chip_recent)

        allChip.isChecked = true

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                // If nothing is checked, default to ALL
                allChip.isChecked = true
                return@setOnCheckedStateChangeListener
            }

            when (checkedIds[0]) {
                R.id.chip_all -> {
                    currentFilterMode = FilterMode.ALL
                }
                R.id.chip_public -> {
                    currentFilterMode = FilterMode.PUBLIC_ONLY
                }
                R.id.chip_recent -> {
                    currentFilterMode = FilterMode.RECENT_ONLY
                }
            }

            applyFilters()
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters(newText)
                return true
            }
        })
    }

    private fun loadJournalEntries() {
        journalsDb.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allEntries.clear()

                for (journalSnapshot in snapshot.children) {
                    val journal = journalSnapshot.getValue(JournalEntry::class.java)

                    journal?.let { entry ->
                        // Fetch user information for this entry
                        usersDb.child(entry.userId).get().addOnSuccessListener { userSnapshot ->
                            val userName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown User"

                            // Create combined journal entry with user data
                            val entryWithUser = JournalEntryWithUser(
                                entry = entry,
                                userName = userName
                            )

                            allEntries.add(entryWithUser)

                            // Sort entries by timestamp (newest first)
                            allEntries.sortByDescending { it.entry.timestamp }

                            // Apply filters to update the display
                            applyFilters()
                        }.addOnFailureListener { e ->
                            Log.e("ContentModeration", "Failed to load user data: ${e.message}")
                        }
                    }
                }

                // Hide loading view
                showLoading(false)

                // If no entries were found at all
                if (snapshot.childrenCount == 0L) {
                    noEntriesFoundText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ContentModeration,
                    "Error loading journal entries: ${error.message}",
                    Toast.LENGTH_SHORT).show()
                showLoading(false)
                noEntriesFoundText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
        })
    }

    private fun applyFilters(searchQuery: String? = null) {
        // Start with all entries
        var filteredList = allEntries

        // Apply mode filter
        filteredList = when (currentFilterMode) {
            FilterMode.ALL -> filteredList
            FilterMode.PUBLIC_ONLY -> {
                // Since we don't have direct isPublic field, we can check if the entry
                // is marked as public in some other way. For now, let's return all entries
                // or alternatively use a different field if available in your JournalEntry model
                filteredList
            }

            FilterMode.RECENT_ONLY -> {
                // Filter entries from the last 7 days
                val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                filteredList.filter { it.entry.timestamp >= oneWeekAgo }
            }

            FilterMode.FLAGGED_ONLY -> {
                // For future implementation
                filteredList
            }
        } as MutableList<JournalEntryWithUser>

        // Apply search filter if provided
        if (!searchQuery.isNullOrBlank()) {
            val query = searchQuery.trim().lowercase()
            filteredList = filteredList.filter {
                it.userName.lowercase().contains(query) ||
                        it.entry.content.lowercase().contains(query) ||
                        it.entry.title.lowercase().contains(query) ||
                        it.entry.mood.lowercase().contains(query)
            } as MutableList<JournalEntryWithUser>
        }

        // Update displayed entries
        displayedEntries.clear()
        displayedEntries.addAll(filteredList)
        adapter.notifyDataSetChanged()

        // Show or hide the "no entries found" message
        if (displayedEntries.isEmpty()) {
            noEntriesFoundText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noEntriesFoundText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showDeleteConfirmationDialog(entry: JournalEntryWithUser) {
        AlertDialog.Builder(this)
            .setTitle("Delete Journal Entry")
            .setMessage("Are you sure you want to delete this journal entry? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteJournalEntry(entry.entry.id)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteJournalEntry(entryId: String) {
        journalsDb.child(entryId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this,
                    "Journal entry has been deleted",
                    Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Deletion failed: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Data class to combine journal entry with user data
    data class JournalEntryWithUser(
        val entry: JournalEntry,
        val userName: String
    )

    // Adapter for displaying journal entries in RecyclerView
    inner class JournalEntryAdapter(
        private val entries: List<JournalEntryWithUser>,
        private val onDeleteClick: (JournalEntryWithUser) -> Unit
    ) : RecyclerView.Adapter<JournalEntryAdapter.JournalEntryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalEntryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_content_moderation, parent, false)
            return JournalEntryViewHolder(view)
        }

        override fun onBindViewHolder(holder: JournalEntryViewHolder, position: Int) {
            val entryWithUser = entries[position]
            holder.bind(entryWithUser)
        }

        override fun getItemCount(): Int = entries.size

        inner class JournalEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView: MaterialCardView = itemView.findViewById(R.id.card_journal)
            private val nameTextView: TextView = itemView.findViewById(R.id.text_user_name)
            private val titleTextView: TextView = itemView.findViewById(R.id.text_journal_title)
            private val contentTextView: TextView = itemView.findViewById(R.id.text_journal_content)
            private val moodTextView: TextView = itemView.findViewById(R.id.text_journal_mood)
            private val timestampTextView: TextView = itemView.findViewById(R.id.text_journal_timestamp)
            private val isPublicIcon: ImageView = itemView.findViewById(R.id.icon_public)
            private val deleteButton: View = itemView.findViewById(R.id.button_delete_journal)

            fun bind(entryWithUser: JournalEntryWithUser) {
                val entry = entryWithUser.entry

                nameTextView.text = entryWithUser.userName
                titleTextView.text = entry.title
                contentTextView.text = entry.content
                moodTextView.text = entry.mood

                // Format timestamp
                val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                timestampTextView.text = dateFormat.format(Date(entry.timestamp))

                // After looking at your JournalEntry model, I see you have isPublic field
                // But if your model differs, we need to adjust this
                // If entry doesn't have isPublic field, this needs to be changed
                try {
                    // Use reflection to check if the field exists
                    val field = entry.javaClass.getDeclaredField("isPublic")
                    field.isAccessible = true
                    val isPublic = field.getBoolean(entry)
                    isPublicIcon.visibility = if (isPublic) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    // If field doesn't exist, always hide the icon
                    isPublicIcon.visibility = View.GONE
                }

                // Set click listener for delete button
                deleteButton.setOnClickListener {
                    onDeleteClick(entryWithUser)
                }

                // Expand card to show full content on click
                cardView.setOnClickListener {
                    // Toggle between the max lines constraint for content
                    if (contentTextView.maxLines == 3) {
                        contentTextView.maxLines = Integer.MAX_VALUE
                    } else {
                        contentTextView.maxLines = 3
                    }
                }
            }
        }
    }
}