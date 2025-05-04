package com.ras.mydiary

import java.text.SimpleDateFormat
import java.util.*
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.text.format.DateUtils
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class JournalAdapter(private val journalList: List<JournalEntry>) :
    RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    class JournalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        val userName: TextView = itemView.findViewById(R.id.usernameTextView)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val content: TextView = itemView.findViewById(R.id.contentTextView)
        val mood: TextView = itemView.findViewById(R.id.moodTextView)
        val timestamp: TextView = itemView.findViewById(R.id.timestampTextView)
        val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        val likeCount: TextView = itemView.findViewById(R.id.likeCountTextView)
        val readMore: TextView = itemView.findViewById(R.id.commentCountTextView)
        val privacyIcon: ImageView = itemView.findViewById(R.id.privacyIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val journal = journalList[position]
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Set username
        holder.userName.text = journal.userName

        // Set title - this was missing
        holder.titleTextView.text = journal.title

        // Set content (limit to 3 lines, ellipsize if longer)
        holder.content.text = journal.content

        // Set mood with appropriate background
        holder.mood.text = journal.mood

        // Set time in a friendly format
        holder.timestamp.text = getRelativeTimeSpanString(journal.timestamp)

        // Set like count
        val likesCount = journal.likes.size
        holder.likeCount.text = likesCount.toString()

        // Set privacy icon visibility
        if (!journal.isPublic) {
            holder.privacyIcon.visibility = View.VISIBLE
        } else {
            holder.privacyIcon.visibility = View.GONE
        }

        // Handle like icon state
        val isLiked = currentUser?.let { journal.likes[it.uid] == true } ?: false
        if (isLiked) {
            holder.likeIcon.setImageResource(R.drawable.ic_heart_filled)
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_heart_outline)
        }

        // Add click listener for like icon
        holder.likeIcon.setOnClickListener {
            // Handling like feature should be done in the detail view
            // Navigate to journal detail
            navigateToDetail(holder, journal)
        }

        // Set the profile image
        try {
            // Try to load user image if available
            // For now, just use a placeholder
            Glide.with(holder.itemView.context)
                .load(R.drawable.icon_profile_foreground)
                .circleCrop()
                .into(holder.profileImageView)
        } catch (e: Exception) {
            // Fallback for errors
            holder.profileImageView.setImageResource(R.drawable.icon_profile_foreground)
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            navigateToDetail(holder, journal)
        }

        // Set click listener for read more
        holder.readMore.setOnClickListener {
            navigateToDetail(holder, journal)
        }
    }

    private fun navigateToDetail(holder: JournalViewHolder, journal: JournalEntry) {
        // Create bundle with journal ID
        val bundle = Bundle().apply {
            putString("journal_id", journal.id)
        }

        // Navigate to journal detail fragment
        holder.itemView.findNavController().navigate(R.id.nav_view_journal, bundle)
    }

    private fun getRelativeTimeSpanString(timestamp: Long): String {
        val now = System.currentTimeMillis()

        return when {
            // Same day
            DateUtils.isToday(timestamp) -> {
                // Format as "Today at HH:mm"
                "Today at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))}"
            }
            // Yesterday
            DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> {
                // Format as "Yesterday at HH:mm"
                "Yesterday at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))}"
            }
            // This week (within 7 days)
            now - timestamp < 7 * DateUtils.DAY_IN_MILLIS -> {
                // Format as "DayOfWeek at HH:mm"
                SimpleDateFormat("EEEE 'at' h:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            // This year
            Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR) ==
                    Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR) -> {
                // Format as "MMM d at HH:mm"
                SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            // Different year
            else -> {
                // Format as "MMM d, YYYY"
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    override fun getItemCount() = journalList.size
}