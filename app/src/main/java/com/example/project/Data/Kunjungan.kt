package com.example.project.Data

data class Kunjungan(
    val id_pengunjung: String? = null,
    val nama: String? = null,
    val kamar_pasien: String? = null,
    val nama_pasien: String? = null,
    val tanggal_kunjungan: Long = 0L,
    val jam_kunjungan: String? = null,
    val hubungan: String? = null,
    val status: String? = "menunggu",
    val keterangan: String? = "-"
)
