package com.ras.mydiary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.ras.mydiary.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalAdapter: JournalAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val allJournalEntries = mutableListOf<JournalEntry>()
    private var currentFilterMode = FilterMode.ALL_ENTRIES
    private var currentSearchQuery = ""

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

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Journals")

        binding.journalRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        journalAdapter = JournalAdapter(emptyList())
        binding.journalRecyclerView.adapter = journalAdapter

        setupSearch()
        setupTabLayout()

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadJournalEntries()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        loadJournalEntries()

        requireActivity().supportFragmentManager.addOnBackStackChangedListener {
            val fragment = requireActivity().supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (fragment !is MoodAnalysisFragment) {
                binding.journalContentContainer.visibility = View.VISIBLE
            }
        }
    }


    private fun setupSearch() {
        val searchEditText = binding.searchEditText
        val clearSearchButton = binding.clearSearchButton

        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch(searchEditText.text.toString())

                // Hide keyboard
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)

                return@setOnEditorActionListener true
            }
            false
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearSearchButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                performSearch(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

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
                        showJournalList()

                        removeMoodAnalysisFragment()
                    }
                    1 -> {
                        currentFilterMode = FilterMode.MY_ENTRIES
                        applyFiltersAndSearch()
                        showJournalList()

                        removeMoodAnalysisFragment()
                    }
                    2 -> {
                        currentFilterMode = FilterMode.MOOD_ANALYSIS
                        openMoodAnalysisFragment()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun removeMoodAnalysisFragment() {
        val fragmentManager = requireActivity().supportFragmentManager
        val moodFragment = fragmentManager.findFragmentById(R.id.fragmentContainer)
        if (moodFragment is MoodAnalysisFragment) {
            fragmentManager.popBackStack()
        }
    }


    private fun showJournalList() {
        binding.journalContentContainer.visibility = View.VISIBLE
    }

    private fun openMoodAnalysisFragment() {
        try {
            val moodAnalysisFragment = MoodAnalysisFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, moodAnalysisFragment)
            transaction.addToBackStack(null)
            transaction.commit()

            binding.journalContentContainer.visibility = View.GONE
        } catch (e: Exception) {
            println("Error navigating to MoodAnalysisFragment: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadJournalEntries() {
        binding.swipeRefreshLayout.isRefreshing = true

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val journalList = mutableListOf<JournalEntry>()
                val currentUserId = auth.currentUser?.uid

                for (journalSnapshot in snapshot.children) {
                    val journal = journalSnapshot.getValue(JournalEntry::class.java)
                    if (journal != null && (journal.public || journal.userId == currentUserId)) {
                        journalList.add(journal)
                    }
                }

                journalList.sortByDescending { it.timestamp }

                allJournalEntries.clear()
                allJournalEntries.addAll(journalList)

                applyFiltersAndSearch()
                binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun performSearch(query: String) {
        currentSearchQuery = query.trim().lowercase()
        applyFiltersAndSearch()
    }

    @OptIn(UnstableApi::class)
    private fun applyFiltersAndSearch() {
        _binding?.let { binding ->

            var filteredList = allJournalEntries.toMutableList()

            when (currentFilterMode) {
                FilterMode.ALL_ENTRIES -> { /* No filter */ }
                FilterMode.MY_ENTRIES -> {
                    val currentUserId = auth.currentUser?.uid ?: ""
                    filteredList = filteredList.filter { it.userId == currentUserId }.toMutableList()
                }
                FilterMode.MOOD_ANALYSIS -> { /* Navigation already handled */ }
            }

            if (currentSearchQuery.isNotEmpty()) {
                filteredList = filteredList.filter {
                    it.userName.lowercase().contains(currentSearchQuery) ||
                            it.mood.lowercase().contains(currentSearchQuery) ||
                            it.content.lowercase().contains(currentSearchQuery)
                }.toMutableList()
            }

            journalAdapter.updateData(filteredList)

            binding.emptyStateTextView.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE

        } ?: Log.w("HomeFragment", "applyFiltersAndSearch() called after view destroyed")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}