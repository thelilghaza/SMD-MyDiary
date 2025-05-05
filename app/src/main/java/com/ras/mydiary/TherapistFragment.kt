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

class TherapistFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TherapistListAdapter
    private val therapistList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_therapist, container, false)
        recyclerView = view.findViewById(R.id.recyclerView_therapist)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = TherapistListAdapter(therapistList)
        recyclerView.adapter = adapter

        loadTherapists()
        return view
    }

    private fun loadTherapists() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Users")

        dbRef.orderByChild("role").equalTo("therapist")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    therapistList.clear()
                    for (data in snapshot.children) {
                        val user = data.getValue(User::class.java)
                        user?.let {
                            therapistList.add(it)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TherapistFragment", "Error fetching therapists", error.toException())
                }
            })
    }
}