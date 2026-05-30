package com.example.dailycash.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.dailycash.R
import com.example.dailycash.databinding.FragmentProfileBinding
import com.example.dailycash.ui.auth.LoginActivity
import com.example.dailycash.utils.PreferenceManager
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        // Set email profil
        binding.tvProfileEmail.text = auth.currentUser?.email ?: preferenceManager.getUserEmail()

        // Logika Dark Mode
        binding.switchDarkMode.isChecked = preferenceManager.isDarkMode()
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setDarkMode(isChecked)
        }

        // Tampilkan bahasa saat ini
        val currentLang = if (preferenceManager.getLanguage() == "in") getString(R.string.lang_indo) else getString(R.string.lang_eng)
        binding.tvCurrentLanguage.text = currentLang

        // Klik untuk ganti bahasa
        binding.btnChangeLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Keluar Akun
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            preferenceManager.clear()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_indo), getString(R.string.lang_eng))
        val langCodes = arrayOf("in", "en")
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.change_language))
            .setItems(languages) { _, which ->
                val selectedLang = langCodes[which]
                if (preferenceManager.getLanguage() != selectedLang) {
                    preferenceManager.setLanguage(selectedLang)
                    // Refresh Activity untuk menerapkan bahasa baru
                    activity?.recreate()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
