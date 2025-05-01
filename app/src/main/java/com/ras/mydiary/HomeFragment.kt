package com.ras.mydiary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ras.mydiary.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalAdapter: JournalAdapter

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

        // Dummy data
        val dummyEntries = listOf(
            JournalEntry("1", "Raza", "Today I felt hopeful.", "Hopeful", System.currentTimeMillis()),
            JournalEntry("2", "Sana", "It was a tough day, but I managed.", "Tired", System.currentTimeMillis() - 3600000),
            JournalEntry("3", "Abdulrehman", "Had a peaceful walk in the evening.", "Peaceful", System.currentTimeMillis() - 7200000)
        )

        journalAdapter = JournalAdapter(dummyEntries)
        binding.journalRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.journalRecyclerView.adapter = journalAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}