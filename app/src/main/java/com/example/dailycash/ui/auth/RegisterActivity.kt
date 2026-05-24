package com.example.dailycash.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dailycash.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth by lazy { 
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val email = binding.etRegisterEmail.text.toString()
        val password = binding.etRegisterPassword.text.toString()

        if (auth == null) {
            Toast.makeText(this, "Firebase belum terkonfigurasi. Tambahkan google-services.json!", Toast.LENGTH_LONG).show()
            return
        }

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.registerProgressBar.visibility = View.VISIBLE
        auth?.createUserWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                binding.registerProgressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
