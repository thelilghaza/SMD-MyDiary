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
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

class JournalAdapter(private var journalList: List<JournalEntry>) :
    RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private val TAG = "JournalAdapter"
    private val API_BASE_URL = "http://192.168.155.103/mydiary_api"

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

        // Set the profile image
        try {
            holder.profileImageView.setImageResource(R.drawable.icon_profile_foreground)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting profile image: ${e.message}")
        }

        // Handle image preview - hide it by default
        holder.imagePreview.visibility = View.GONE

        // Only try to load an image if we have an entry ID
        if (journal.id.isNotEmpty()) {
            // Construct the API URL to fetch the image as JSON
            val imageUrl = "$API_BASE_URL/fetch_image.php?entryId=${journal.id}&format=json"

            // Use Volley to check if the image exists
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET, imageUrl, null,
                { response ->
                    // Check if the response contains image data
                    if (response.has("imageData") && !response.getString("imageData").isNullOrEmpty()) {
                        // Image exists, convert base64 to bitmap
                        try {
                            val base64Image = response.getString("imageData")
                            val bitmap = ImageUtils.base64ToBitmap(base64Image)

                            if (bitmap != null) {
                                // Set the bitmap to the ImageView
                                holder.imagePreview.setImageBitmap(bitmap)
                                holder.imagePreview.visibility = View.VISIBLE
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting image: ${e.message}")
                        }
                    }
                },
                { error ->
                    Log.e(TAG, "Error checking for image: ${error.message}")
                }
            )

            // Add the request to the queue
            Volley.newRequestQueue(holder.itemView.context).add(jsonObjectRequest)
        }

        // Set click listeners
        holder.itemView.setOnClickListener {
            navigateToDetail(holder, journal)
        }

        holder.readMore.setOnClickListener {
            navigateToDetail(holder, journal)
        }

        holder.likeIcon.setOnClickListener {
            navigateToDetail(holder, journal)
        }
    }

    private fun navigateToDetail(holder: JournalViewHolder, journal: JournalEntry) {
        val bundle = Bundle().apply {
            putString("journal_id", journal.id)
        }
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