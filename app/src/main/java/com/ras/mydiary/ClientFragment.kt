package com.ras.mydiary

import User
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ClientFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClientListAdapter
    private val clientList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_client, container, false)
        recyclerView = view.findViewById(R.id.recyclerView_client)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ClientListAdapter(clientList)
        recyclerView.adapter = adapter

        loadClients()
        return view
    }

    private fun loadClients() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Users")

        dbRef.orderByChild("role").equalTo("user")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    clientList.clear()
                    for (data in snapshot.children) {
                        val user = data.getValue(User::class.java)
                        user?.let {
                            clientList.add(it)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("clientFragment", "Error fetching clients", error.toException())
                }
            })
    }
}