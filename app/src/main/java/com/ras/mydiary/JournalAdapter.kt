package com.ras.mydiary

import java.text.SimpleDateFormat
import java.util.*
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class JournalAdapter(private val journalList: List<JournalEntry>) :
    RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    class JournalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.usernameTextView)
        val content: TextView = itemView.findViewById(R.id.contentTextView)
        val mood: TextView = itemView.findViewById(R.id.moodTextView)
        val timestamp: TextView = itemView.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val journal = journalList[position]
        holder.userName.text = journal.userName
        holder.content.text = journal.content
        holder.mood.text = journal.mood
        holder.timestamp.text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date(journal.timestamp))
    }

    override fun getItemCount() = journalList.size
}