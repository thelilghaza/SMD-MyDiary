package com.ras.mydiary

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.graphics.drawable.GradientDrawable

class MoodAnalysisFragment : Fragment() {

    private lateinit var entriesLineChart: LineChart
    private lateinit var moodBarContainer: LinearLayout
    private lateinit var timeRangeButton: MaterialButton
    private lateinit var moodFilterSpinner: Spinner

    private val database = FirebaseDatabase.getInstance().reference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Updated mood types
    private val allMoodTypes = listOf("angry", "sad", "happy", "neutral", "irritated", "excited", "nervous", "tired", "confused")

    // Currently selected mood for bar graph
    private var selectedMood = "happy"

    // Current time range filter
    private var currentTimeRange = TimeRange.WEEKLY

    // Fixed mood bar color
    private val moodBarColor = Color.parseColor("#9EB5DB")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mood_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        entriesLineChart = view.findViewById(R.id.entriesLineChart)
        moodBarContainer = view.findViewById(R.id.moodBarContainer)
        timeRangeButton = view.findViewById(R.id.timeRangeButton)
        moodFilterSpinner = view.findViewById(R.id.moodFilterSpinner)

        // Give the chart some time to properly initialize its layout
        view.post {
            setupLineChart()
            setupTimeRangeButton()
            setupMoodFilterSpinner()
            loadJournalEntries()
        }
    }

    private fun setupLineChart() {
        entriesLineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            // Significantly increase horizontal margins to prevent cutoff
            setExtraOffsets(30f, 20f, 30f, 20f)

            // Disable auto-scaling that might cause the chart to be cut off
            setScaleEnabled(false)

            // Add margin to ensure content is fully visible
            minOffset = 30f

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = 45f // Tilting the x-axis labels by 45 degrees
                setAvoidFirstLastClipping(true) // Prevent clipping of first/last labels
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                spaceTop = 20f // Add extra space at the top
            }

            axisRight.isEnabled = false
        }
    }

    private fun setupTimeRangeButton() {
        timeRangeButton.setOnClickListener {
            currentTimeRange = when(currentTimeRange) {
                TimeRange.WEEKLY -> TimeRange.MONTHLY
                TimeRange.MONTHLY -> TimeRange.YEARLY
                TimeRange.YEARLY -> TimeRange.WEEKLY
            }

            timeRangeButton.text = when(currentTimeRange) {
                TimeRange.WEEKLY -> "Weekly"
                TimeRange.MONTHLY -> "Monthly"
                TimeRange.YEARLY -> "Yearly"
            }

            loadJournalEntries()
        }
    }

    private fun setupMoodFilterSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            allMoodTypes.map { it.capitalize(Locale.getDefault()) }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        moodFilterSpinner.adapter = adapter
        moodFilterSpinner.setSelection(allMoodTypes.indexOf(selectedMood))

        moodFilterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMood = allMoodTypes[position]
                loadJournalEntries()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun loadJournalEntries() {
        // Calculate date range based on selected time range
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.apply {
            when(currentTimeRange) {
                TimeRange.WEEKLY -> add(Calendar.DAY_OF_YEAR, -7)
                TimeRange.MONTHLY -> add(Calendar.MONTH, -1)
                TimeRange.YEARLY -> add(Calendar.YEAR, -1)
            }
        }
        val startTime = calendar.timeInMillis

        // Query entries from Realtime Database
        database.child("Journals")
            .orderByChild("userId")
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = ArrayList<JournalEntry>()

                    for (entrySnapshot in snapshot.children) {
                        val entry = entrySnapshot.getValue(JournalEntry::class.java)
                        entry?.let {
                            if (it.timestamp >= startTime && it.timestamp <= endTime) {
                                entries.add(it)
                            }
                        }
                    }

                    updateLineChart(entries)
                    updateMoodBars(entries)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    println("Error getting entries: ${error.message}")
                }
            })
    }

    private fun updateLineChart(entries: List<JournalEntry>) {
        val chartEntries = ArrayList<Entry>()
        val moodLabels = ArrayList<String>()

        // Count frequency of each mood type
        val moodFrequency = mutableMapOf<String, Int>()

        // Initialize all mood types with 0 count
        allMoodTypes.forEach { mood ->
            moodFrequency[mood] = 0
        }

        // Count frequencies
        for (entry in entries) {
            val mood = entry.mood.lowercase(Locale.getDefault())
            moodFrequency[mood] = (moodFrequency[mood] ?: 0) + 1
        }

        // Create chart data points
        moodFrequency.entries.forEachIndexed { index, (mood, count) ->
            chartEntries.add(Entry(index.toFloat(), count.toFloat()))
            moodLabels.add(mood.capitalize(Locale.getDefault()))
        }

        // Set up line dataset
        val dataSet = LineDataSet(chartEntries, "Mood Frequency").apply {
            color = Color.parseColor("#3F51B5")
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(Color.parseColor("#3F51B5"))
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER

            // Add extra spacing to prevent drawing outside bounds
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0f) value.toInt().toString() else ""
                }
            }
        }

        // Set chart data
        entriesLineChart.data = LineData(dataSet)

        // Set x-axis labels
        entriesLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(moodLabels)

        // Extra space for rotated labels
        entriesLineChart.extraBottomOffset = 30f

        // Make sure all data points are visible
        entriesLineChart.fitScreen()

        // Apply padding to ensure content is visible
        val labelCount = moodLabels.size
        if (labelCount > 0) {
            entriesLineChart.setVisibleXRangeMaximum((labelCount + 0.5f))
        }

        entriesLineChart.invalidate()
    }

    private fun updateMoodBars(entries: List<JournalEntry>) {
        moodBarContainer.removeAllViews()

        // Get days of week for weekly view
        val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")

        // Filter entries by selected mood
        val filteredEntries = entries.filter { it.mood.lowercase(Locale.getDefault()) == selectedMood.lowercase(
            Locale.getDefault()
        ) }

        // Count entries by day of week
        val entriesCountByDay = mutableMapOf<Int, Int>()

        for (entry in filteredEntries) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = entry.timestamp
            }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Map Calendar.DAY_OF_WEEK to 0-6 index (Monday-Sunday)
            val dayIndex = when(dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> -1 // Should never happen
            }

            if (dayIndex >= 0) {
                entriesCountByDay[dayIndex] = (entriesCountByDay[dayIndex] ?: 0) + 1
            }
        }

        // Find maximum count for scaling
        val maxCount = entriesCountByDay.values.maxOrNull() ?: 1

        // Create mood bars
        for (i in 0 until 7) {
            val barLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
            }

            val count = entriesCountByDay[i] ?: 0

            val barHeight = if (count > 0 && maxCount > 0) {
                (count.toFloat() / maxCount.toFloat() * 100f).toInt()
            } else {
                0
            }

            val emptySpace = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    (100 - barHeight).toFloat()
                )
            }

            // Create rounded corner drawable
            val roundedCorner = GradientDrawable()
            roundedCorner.shape = GradientDrawable.RECTANGLE
            roundedCorner.cornerRadius = 16f // Rounded corners
            roundedCorner.setColor(moodBarColor) // Use the single blue color for all bars

            val bar = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    barHeight.toFloat()
                )
                background = roundedCorner
            }

            barLayout.addView(emptySpace)
            barLayout.addView(bar)
            moodBarContainer.addView(barLayout)
        }
    }

    private fun getMoodColor(mood: String): Int {
        // Always return the same blue color for all moods
        return moodBarColor
    }

    enum class TimeRange {
        WEEKLY, MONTHLY, YEARLY
    }
}