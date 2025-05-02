package com.ras.mydiary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.ras.mydiary.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalAdapter: JournalAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Keep a list of all journal entries to filter for search
    private val allJournalEntries = mutableListOf<JournalEntry>()

    // Current filter mode
    private var currentFilterMode = FilterMode.ALL_ENTRIES

    // Search query
    private var currentSearchQuery = ""

    // Enum for filter modes
    private enum class FilterMode {
        ALL_ENTRIES,
        MY_ENTRIES,
        MOOD_ANALYSIS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference("Journals")
        auth = FirebaseAuth.getInstance()

        // Initialize RecyclerView
        binding.journalRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set up search functionality
        setupSearch()

        // Set up tab layout
        setupTabLayout()

        // Set up SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadJournalEntries()
        }

        // Set up the color scheme for the SwipeRefreshLayout
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Load dummy data initially (for quick UI rendering)
        loadDummyData()

        // Load real data from Firebase
        loadJournalEntries()
    }

    private fun setupSearch() {
        val searchEditText = binding.searchEditText
        val clearSearchButton = binding.clearSearchButton

        // Setup search input
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch(searchEditText.text.toString())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        // Text change listener for real-time search and clear button visibility
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearSearchButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                performSearch(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear search button
        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            performSearch("")
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentFilterMode = FilterMode.ALL_ENTRIES
                        applyFiltersAndSearch()
                    }
                    1 -> {
                        currentFilterMode = FilterMode.MY_ENTRIES
                        applyFiltersAndSearch()
                    }
                    2 -> {
                        currentFilterMode = FilterMode.MOOD_ANALYSIS
                        // TODO: Implement mood analysis view
                        // For now, just filter by entries with a mood
                        applyFiltersAndSearch()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadDummyData() {
        // Dummy data matching the updated JournalEntry structure
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: ""

        val dummyEntries = listOf(
            JournalEntry(
                id = "1",
                userId = userId,
                userName = "Raza",
                content = "Today I felt hopeful.",
                mood = "Hopeful",
                timestamp = System.currentTimeMillis(),
                likes = mapOf(),
                isPublic = true
            ),
            JournalEntry(
                id = "2",
                userId = userId,
                userName = "Sana",
                content = "It was a tough day, but I managed.",
                mood = "Tired",
                timestamp = System.currentTimeMillis() - 3600000,
                likes = mapOf(),
                isPublic = true
            ),
            JournalEntry(
                id = "3",
                userId = userId,
                userName = "Abdulrehman",
                content = "Had a peaceful walk in the evening.",
                mood = "Peaceful",
                timestamp = System.currentTimeMillis() - 7200000,
                likes = mapOf(),
                isPublic = true
            )
        )

        allJournalEntries.clear()
        allJournalEntries.addAll(dummyEntries)

        journalAdapter = JournalAdapter(dummyEntries)
        binding.journalRecyclerView.adapter = journalAdapter
    }

    private fun loadJournalEntries() {
        binding.swipeRefreshLayout.isRefreshing = true

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val journalList = mutableListOf<JournalEntry>()
                val currentUserId = auth.currentUser?.uid

                for (journalSnapshot in snapshot.children) {
                    val journal = journalSnapshot.getValue(JournalEntry::class.java)

                    // Only add entries that are either:
                    // 1. Public entries from anyone
                    // 2. Private entries that belong to the current user
                    journal?.let {
                        if (it.isPublic || it.userId == currentUserId) {
                            journalList.add(it)
                        }
                    }
                }

                // Sort by timestamp (newest first)
                journalList.sortByDescending { it.timestamp }

                // Save all entries for filtering
                allJournalEntries.clear()
                allJournalEntries.addAll(journalList)

                // Apply filters and search
                applyFiltersAndSearch()

                // Stop refresh animation
                binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                binding.swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun performSearch(query: String) {
        currentSearchQuery = query.trim().lowercase()
        applyFiltersAndSearch()
    }

    private fun applyFiltersAndSearch() {
        // Start with all entries
        var filteredList = allJournalEntries.toMutableList()

        // Apply tab filter
        when (currentFilterMode) {
            FilterMode.ALL_ENTRIES -> {
                // No additional filtering needed
            }
            FilterMode.MY_ENTRIES -> {
                val currentUserId = auth.currentUser?.uid ?: ""
                filteredList = filteredList.filter { it.userId == currentUserId }.toMutableList()
            }
            FilterMode.MOOD_ANALYSIS -> {
                // For now, just show all entries (will be replaced with actual mood analysis)
                // This is just a placeholder for future mood analysis functionality
            }
        }

        // Apply search filter if there's a query
        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.userName.lowercase().contains(currentSearchQuery) ||
                        it.mood.lowercase().contains(currentSearchQuery) ||
                        it.content.lowercase().contains(currentSearchQuery)
            }.toMutableList()
        }

        // Update adapter
        journalAdapter = JournalAdapter(filteredList)
        binding.journalRecyclerView.adapter = journalAdapter

        // Show empty state if no entries
        if (filteredList.isEmpty()) {
            binding.emptyStateTextView.visibility = View.VISIBLE
        } else {
            binding.emptyStateTextView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}