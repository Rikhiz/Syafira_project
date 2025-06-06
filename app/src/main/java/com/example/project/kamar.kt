package com.example.project

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Data.Ruangan
import com.example.project.Data.cardData
import com.example.project.databinding.FragmentKamarBinding
import com.example.project.kamarPage.AturKamar
import com.example.project.kamarPage.kamarJenis
import com.google.firebase.database.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class kamar : Fragment() {
    private lateinit var ref: DatabaseReference
    private lateinit var binding: FragmentKamarBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var sharedPreferences: SharedPreferences

    private val items = mutableListOf<Ruangan>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentKamarBinding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        recyclerView = binding.recyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ItemAdapter(listOf()) { jenis ->
            val detailFragment = kamarJenis.newInstance(jenis)
            setCurrentFragment(detailFragment)
        }
        recyclerView.adapter = adapter

        ref = FirebaseDatabase.getInstance().reference.child("Ruangan")
        binding.shimmerLayout.startShimmer()

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val groupedData = mutableMapOf<String, MutableList<Ruangan>>()

                    for (dataSnapshot in snapshot.children) {
                        val ruangan = dataSnapshot.getValue(Ruangan::class.java)
                        if (ruangan != null && ruangan.jenis != null) {
                            groupedData.getOrPut(ruangan.jenis) { mutableListOf() }.add(ruangan)
                        }
                    }

                    val resultList = groupedData.map { entry ->
                        val jenis = entry.key
                        val list = entry.value
                        val kosongCount = list.count { it.status == "kosong" }
                        val terisiCount = list.count { it.status == "terisi" }

                        cardData(
                            jenis = jenis,
                            kosong = kosongCount,
                            terisi = terisiCount
                        )
                    }
                    adapter.updateData(resultList)
                    binding.shimmerLayout.apply {
                        stopShimmer()
                        visibility = View.GONE
                    }
                    binding.recyclerView.visibility = View.VISIBLE

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
            }
        })


        return binding.root
    }

    private fun setCurrentFragment(fragment: Fragment) =
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            addToBackStack(null)
            commit()
        }
}
