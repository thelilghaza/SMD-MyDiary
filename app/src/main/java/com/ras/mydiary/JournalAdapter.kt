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
import android.util.Log
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class JournalAdapter(private var journalList: List<JournalEntry>) :
    RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private val TAG = "JournalAdapter"
    private val API_BASE_URL = "http://192.168.100.69/mydiary_api" // Update this to match your actual API URL

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
        val imagePreview: ImageView = itemView.findViewById(R.id.image_preview)
    }

    fun updateData(newList: List<JournalEntry>) {
        this.journalList = newList
        notifyDataSetChanged()
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

        // Set title
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
        if (!journal.public) {
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
            // Navigate to journal detail
            navigateToDetail(holder, journal)
        }

        // Set the profile image
        try {
            Glide.with(holder.itemView.context)
                .load(R.drawable.icon_profile_foreground)
                .circleCrop()
                .into(holder.profileImageView)
        } catch (e: Exception) {
            holder.profileImageView.setImageResource(R.drawable.icon_profile_foreground)
        }

        // Handle image preview - using the fetch_image.php API
        if (journal.id.isNotEmpty()) {
            // Construct the API URL to fetch the image
            val imageUrl = "$API_BASE_URL/fetch_image.php?entryId=${journal.id}"

            try {
                // Load image using Glide
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .centerCrop()
                    .into(holder.imagePreview)

                holder.imagePreview.visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image from API: ${e.message}")
                holder.imagePreview.visibility = View.GONE
            }
        } else {
            holder.imagePreview.visibility = View.GONE
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
            DateUtils.isToday(timestamp) -> {
                "Today at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))}"
            }
            DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> {
                "Yesterday at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))}"
            }
            now - timestamp < 7 * DateUtils.DAY_IN_MILLIS -> {
                SimpleDateFormat("EEEE 'at' h:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR) ==
                    Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    override fun getItemCount() = journalList.size
}