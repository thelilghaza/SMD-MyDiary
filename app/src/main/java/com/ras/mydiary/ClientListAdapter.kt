package com.ras.mydiary

import User
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ClientListAdapter(private val clientList: List<User>) :
    RecyclerView.Adapter<ClientListAdapter.ClientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client, parent, false)
        return ClientViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(clientList[position])
    }

    override fun getItemCount(): Int = clientList.size

    inner class ClientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textView_name)
        private val emailTextView: TextView = itemView.findViewById(R.id.textView_email)
        private val profileImageView: ImageView = itemView.findViewById(R.id.imageView_profile)

        fun bind(user: User) {
            nameTextView.text = user.name
            emailTextView.text = user.email
            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.icon_profile_foreground)
                .into(profileImageView)
        }
    }
}
