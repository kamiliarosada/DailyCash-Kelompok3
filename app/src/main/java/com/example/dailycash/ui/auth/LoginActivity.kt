package com.example.dailycash.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dailycash.MainActivity
import com.example.dailycash.databinding.ActivityLoginBinding
import com.example.dailycash.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { 
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(this)

        if (preferenceManager.isLoggedIn() && preferenceManager.isRememberMe()) {
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = binding.etLoginEmail.text.toString()
        val password = binding.etLoginPassword.text.toString()

        if (auth == null) {
            Toast.makeText(this, "Firebase belum terkonfigurasi. Tambahkan google-services.json!", Toast.LENGTH_LONG).show()
            return
        }

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loginProgressBar.visibility = View.VISIBLE
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                binding.loginProgressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    preferenceManager.setLoggedIn(true)
                    preferenceManager.setUserEmail(email)
                    preferenceManager.setRememberMe(binding.cbRememberMe.isChecked)
                    startMainActivity()
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
