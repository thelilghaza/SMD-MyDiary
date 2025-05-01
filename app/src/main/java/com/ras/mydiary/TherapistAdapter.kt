package com.ras.mydiary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TherapistAdapter(private val data: List<Client>) :
    RecyclerView.Adapter<TherapistAdapter.TherapistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TherapistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_therapist_feed, parent, false)
        return TherapistViewHolder(view)
    }

    override fun onBindViewHolder(holder: TherapistViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    inner class TherapistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.textView_client_name)
        private val sessionDateTextView: TextView = itemView.findViewById(R.id.textView_session_date)
        private val moodTextView: TextView = itemView.findViewById(R.id.textView_mood_status)

        fun bind(client: Client) {
            nameTextView.text = client.name
            sessionDateTextView.text = "Last Session: ${client.lastSessionDate}"
            moodTextView.text = "Mood: ${client.moodStatus}"
        }
    }
}