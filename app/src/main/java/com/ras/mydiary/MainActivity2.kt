package com.ras.mydiary

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ras.mydiary.databinding.ActivityMain2Binding

class MainActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMain2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView_feed)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Dummy client data
        val dummyClients = listOf(
            Client("John Doe", "2025-04-29", "Happy"),
            Client("Jane Smith", "2025-04-28", "Stressed"),
            Client("Ali Khan", "2025-04-26", "Neutral"),
            Client("Maria Lopez", "2025-04-22", "Sad"),
            Client("Tom Lee", "2025-04-19", "Anxious")
        )

        val adapter = TherapistAdapter(dummyClients)
        recyclerView.adapter = adapter
    }
}
