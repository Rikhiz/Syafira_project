package com.example.project

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.databinding.ActivityLoginBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Intent
import android.content.SharedPreferences
class login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var ref: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ref = FirebaseDatabase.getInstance().reference.child("user")
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        binding.buttonLogin.setOnClickListener {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(username, password)
            }
        }
    }

    private fun loginUser(username: String, password: String) {
        ref.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val storedPassword = snapshot.child("password").value.toString()

                    if (storedPassword == password) {
                        val nama = snapshot.child("nama").value?.toString() ?: "Guest"

                        // Simpan data user dan nama ke SharedPreferences
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("username", username)
                        editor.putString("nama", nama)  // Simpan nama
                        editor.apply()

                        Toast.makeText(
                            this@login,
                            "Login successful! Welcome, $nama",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(Intent(this@login, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@login, "Invalid password!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@login, "User not found!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
                Toast.makeText(this@login, "Login failed. Try again later.", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
