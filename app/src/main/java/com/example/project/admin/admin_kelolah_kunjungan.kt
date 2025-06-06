package com.example.project.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.Dashboard
import com.example.project.Data.DateItem
import com.example.project.Data.Kunjungan
import com.example.project.R
import com.example.project.admin.adapter.KelolahKunjunganAdapter
import com.example.project.admin.adapter.TanggalAdapter
import com.example.project.databinding.FragmentAdminKelolahKunjunganBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.util.Log


class admin_kelolah_kunjungan : Fragment() {
    private lateinit var binding: FragmentAdminKelolahKunjunganBinding
    private lateinit var tanggalAdapter: TanggalAdapter
    private lateinit var kunjunganAdapter: KelolahKunjunganAdapter
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.bg)
        binding = FragmentAdminKelolahKunjunganBinding.inflate(inflater, container, false)
        setupViews()
        return binding.root
    }

    private fun showLoading() {
        binding.loadingText.visibility = View.VISIBLE
        binding.recyclerViewKunjungan.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.loadingText.visibility = View.GONE
        if (kunjunganAdapter.itemCount > 0) {
            binding.recyclerViewKunjungan.visibility = View.VISIBLE
        }
    }




    private fun setupViews() {
        setupBackButton()
        setupRecyclerViews()
        loadInitialData()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            setCurrentFragment(Dashboard())
        }
    }

    private fun setupRecyclerViews() {
        // Setup Tanggal RecyclerView
        tanggalAdapter = TanggalAdapter(generateDateList(), ::onDateSelected)
        binding.recyclerViewTanggal.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = tanggalAdapter
        }

        // Setup Kunjungan RecyclerView
        kunjunganAdapter = KelolahKunjunganAdapter(
            emptyList(),
            ::onTerimaClick,
            ::onTolakClick
        )
        binding.recyclerViewKunjungan.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = kunjunganAdapter
        }
    }

    private fun loadInitialData() {
        // Load data for current date
        val calendar = Calendar.getInstance()
        loadKunjunganData(calendar.time)
    }

    private fun loadKunjunganData(selectedDate: Date) {
        try {
            showLoading()

            val calendar = Calendar.getInstance().apply {
                time = selectedDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis

            databaseReference.child("Kunjungan")
                .orderByChild("tanggal_kunjungan")
                .startAt(startOfDay.toDouble())
                .endAt(endOfDay.toDouble())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val kunjunganList = mutableListOf<Kunjungan>()

                            for (dataSnapshot in snapshot.children) {
                                dataSnapshot.getValue(Kunjungan::class.java)?.let { kunjungan ->
                                    // Only add kunjungan with status "menunggu"
                                    if (kunjungan.status == "menunggu") {
                                        kunjunganList.add(kunjungan)
                                    }
                                }
                            }

                            if (isAdded && context != null) {
                                // Update RecyclerView with filtered data
                                kunjunganAdapter.updateData(kunjunganList)

                                // Show/hide empty state based on filtered results
                                if (kunjunganList.isEmpty()) {
                                    binding.emptyStateText.visibility = View.VISIBLE
                                    binding.recyclerViewKunjungan.visibility = View.GONE
                                } else {
                                    binding.emptyStateText.visibility = View.GONE
                                    binding.recyclerViewKunjungan.visibility = View.VISIBLE
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("LoadData", "Error processing data: ${e.message}")
                            e.printStackTrace()
                        } finally {
                            hideLoading()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (isAdded && context != null) {
                            hideLoading()
                            Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        } catch (e: Exception) {
            Log.e("LoadData", "Error in loadKunjunganData: ${e.message}")
            e.printStackTrace()
            hideLoading()
        }
    }
    private fun onTerimaClick(kunjungan: Kunjungan, keterangan: String) {
        if (!isAdded) return

        try {
            showLoading()

            // Langsung update menggunakan path yang benar
            val kunjunganRef = databaseReference.child("Kunjungan")

            // 1. Pertama, cari data berdasarkan id_pengunjung untuk mendapatkan push ID
            kunjunganRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var pushId: String? = null

                    // Cari push ID yang sesuai dengan id_pengunjung
                    for (child in snapshot.children) {
                        val data = child.getValue(Kunjungan::class.java)
                        if (data?.id_pengunjung == kunjungan.id_pengunjung) {
                            pushId = child.key
                            break
                        }
                    }

                    if (pushId != null) {
                        Log.d("Firebase", "Found Push ID: $pushId")

                        // 2. Update data menggunakan push ID yang ditemukan
                        val updates = HashMap<String, Any>()
                        updates["hubungan"] = kunjungan.hubungan ?: ""
                        updates["id_pengunjung"] = kunjungan.id_pengunjung ?: ""
                        updates["jam_kunjungan"] = kunjungan.jam_kunjungan ?: ""
                        updates["kamar_pasien"] = kunjungan.kamar_pasien ?: ""
                        updates["nama"] = kunjungan.nama ?: ""
                        updates["nama_pasien"] = kunjungan.nama_pasien ?: ""
                        updates["tanggal_kunjungan"] = kunjungan.tanggal_kunjungan
                        updates["status"] = "Diterima"  // Update status
                        updates["keterangan"] = keterangan  // Update keterangan

                        // Update menggunakan push ID
                        kunjunganRef.child(pushId).updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d("Firebase", "Update berhasil dengan Push ID: $pushId")
                                if (isAdded) {
                                    hideLoading()
                                    Toast.makeText(context, "Kunjungan diterima", Toast.LENGTH_SHORT).show()
                                    // Verifikasi update
                                    kunjunganRef.child(pushId).get().addOnSuccessListener { updatedSnapshot ->
                                        Log.d("Firebase", "Data setelah update: ${updatedSnapshot.value}")
                                    }
                                    loadKunjunganData(Calendar.getInstance().time)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase", "Update gagal: ${exception.message}")
                                if (isAdded) {
                                    hideLoading()
                                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Log.d("Firebase", "Push ID tidak ditemukan untuk ID Pengunjung: ${kunjungan.id_pengunjung}")
                        hideLoading()
                        Toast.makeText(context, "Data kunjungan tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Query dibatalkan: ${error.message}")
                    hideLoading()
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        } catch (e: Exception) {
            Log.e("Firebase", "Error umum: ${e.message}")
            e.printStackTrace()
            hideLoading()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi onTolakClick dengan cara yang sama
    private fun onTolakClick(kunjungan: Kunjungan, keterangan: String) {
        if (!isAdded) return

        try {
            showLoading()

            val kunjunganRef = databaseReference.child("Kunjungan")

            kunjunganRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var pushId: String? = null

                    for (child in snapshot.children) {
                        val data = child.getValue(Kunjungan::class.java)
                        if (data?.id_pengunjung == kunjungan.id_pengunjung) {
                            pushId = child.key
                            break
                        }
                    }

                    if (pushId != null) {
                        Log.d("Firebase", "Found Push ID: $pushId")

                        val updates = HashMap<String, Any>()
                        updates["hubungan"] = kunjungan.hubungan ?: ""
                        updates["id_pengunjung"] = kunjungan.id_pengunjung ?: ""
                        updates["jam_kunjungan"] = kunjungan.jam_kunjungan ?: ""
                        updates["kamar_pasien"] = kunjungan.kamar_pasien ?: ""
                        updates["nama"] = kunjungan.nama ?: ""
                        updates["nama_pasien"] = kunjungan.nama_pasien ?: ""
                        updates["tanggal_kunjungan"] = kunjungan.tanggal_kunjungan
                        updates["status"] = "Ditolak"
                        updates["keterangan"] = keterangan

                        kunjunganRef.child(pushId).updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d("Firebase", "Update berhasil dengan Push ID: $pushId")
                                if (isAdded) {
                                    hideLoading()
                                    Toast.makeText(context, "Kunjungan ditolak", Toast.LENGTH_SHORT).show()
                                    kunjunganRef.child(pushId).get().addOnSuccessListener { updatedSnapshot ->
                                        Log.d("Firebase", "Data setelah update: ${updatedSnapshot.value}")
                                    }
                                    loadKunjunganData(Calendar.getInstance().time)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firebase", "Update gagal: ${exception.message}")
                                if (isAdded) {
                                    hideLoading()
                                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Log.d("Firebase", "Push ID tidak ditemukan untuk ID Pengunjung: ${kunjungan.id_pengunjung}")
                        hideLoading()
                        Toast.makeText(context, "Data kunjungan tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Query dibatalkan: ${error.message}")
                    hideLoading()
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        } catch (e: Exception) {
            Log.e("Firebase", "Error umum: ${e.message}")
            e.printStackTrace()
            hideLoading()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun generateDateList(): List<DateItem> {
        val dateList = mutableListOf<DateItem>()
        val calendar = Calendar.getInstance()
        repeat(30) {
            dateList.add(
                DateItem(
                    date = calendar.time,
                    isSelected = it == 0
                )
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dateList
    }

    private fun onDateSelected(position: Int) {
        tanggalAdapter.updateSelectedPosition(position)
        val selectedDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, position)
        }.time
        loadKunjunganData(selectedDate)
    }

    private fun setCurrentFragment(fragment: Fragment) =
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            addToBackStack(null)
            commit()
        }
}
