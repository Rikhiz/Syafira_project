package com.example.project

import TambahKamar
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Data.Ruangan
import com.example.project.databinding.FragmentKamarAdmBinding
import com.example.project.kamarPage.AturKamar
import com.example.project.kamar_admin.Card_kamar_adm
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.database.*

class kamar_adm : Fragment() {
    private lateinit var ref_ruangan: DatabaseReference
    private var _binding: FragmentKamarAdmBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: Card_kamar_adm
    private val kategoriList = listOf("All", "VVIP", "VIP", "Kelas I", "Kelas II", "Kelas III", "Laboratorium", "ICU", "HCU")
    private var dataRuangan = mutableListOf<Ruangan>()
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.ungu)

        _binding = FragmentKamarAdmBinding.inflate(inflater, container, false)
        setupPieChart()
        setupSpinner()
        setupRecyclerView()

        ref_ruangan = FirebaseDatabase.getInstance().reference.child("Ruangan")
        fetchRealtimeData()
        binding.buttonTambahPasien.setOnClickListener {
            val dialog = AturKamar()
            dialog.show(parentFragmentManager, "AturKamarDialog")
        }
        // In kamar_adm.kt
        binding.buttonTambahRuangan.setOnClickListener {
            val dialog = TambahKamar()
            dialog.show(parentFragmentManager, "TambahKamarDialog")
        }
        binding.buttonCari.setOnClickListener {
            val cariPasien = CariPasien()
            setCurrentFragment(cariPasien)
        }
        return binding.root
    }

    private fun setupPieChart() {
        binding.pieChart.description.isEnabled = false
        binding.pieChart.isDrawHoleEnabled = true
        binding.pieChart.setHoleColor(Color.TRANSPARENT)
        binding.pieChart.setHoleRadius(50f)
        binding.pieChart.setEntryLabelTextSize(12f)
        binding.pieChart.setEntryLabelColor(Color.BLACK)
        binding.pieChart.animateY(1000)
        binding.pieChart.legend.isEnabled = true
    }

    private fun setupSpinner() {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kategoriList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = spinnerAdapter

        binding.spinnerFilter.setSelection(0)

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = kategoriList[position]
                filterData(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        recyclerView = binding.recyclerViewKamar
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3, GridLayoutManager.HORIZONTAL, false)
        adapter = Card_kamar_adm(listOf(), this)
        recyclerView.adapter = adapter
    }

    private fun fetchRealtimeData() {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dataRuangan.clear()
                for (ruanganSnapshot in snapshot.children) {
                    val ruangan = ruanganSnapshot.getValue(Ruangan::class.java)
                    if (ruangan != null) {
                        dataRuangan.add(ruangan)
                    }
                }
                filterData(kategoriList.first())
                binding.shimmerLayout.apply {
                    stopShimmer()
                    visibility = View.GONE
                }
                binding.recyclerViewKamar.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("kamarJenis", "Error fetching data: ${error.message}")
            }
        }
        ref_ruangan.addValueEventListener(valueEventListener)
    }

    private fun filterData(category: String) {
        val filteredData = if (category == "All") dataRuangan else dataRuangan.filter { it.jenis == category }
        adapter.updateData(filteredData)
        updatePieChart(filteredData)
    }

    private fun updatePieChart(filteredData: List<Ruangan>) {
        val counts = filteredData.groupingBy { it.status }.eachCount()
        val total = counts.values.sum().toFloat()

        val entries = counts.map {
            val percentage = (it.value / total) * 100
            PieEntry(percentage, "${it.key} (${it.value} kamar)")
        }

        val dataSet = PieDataSet(entries, "Status").apply {
            colors = listOf(Color.GREEN, Color.RED, Color.YELLOW)
            sliceSpace = 3f
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        val pieData = PieData(dataSet)
        binding.pieChart.data = pieData
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ref_ruangan.removeEventListener(valueEventListener)
        _binding = null
    }

    private fun setCurrentFragment(fragment: Fragment) =
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            addToBackStack(null)
            commit()
        }
}
