package com.example.dailycash.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            saveImageToInternalStorage(it)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = java.io.File(requireContext().filesDir, "profile_pic.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            
            // Save the file path instead of the content URI
            preferenceManager.setProfileImageUri(file.absolutePath)
            loadProfileImage(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadProfileImage(path: String) {
        try {
            val file = java.io.File(path)
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                binding.ivProfileImage.setImageBitmap(bitmap)
                binding.ivProfileImage.clearColorFilter() // Clear the mocha tint to show actual photo
                binding.ivProfileImage.setPadding(0, 0, 0, 0)
                binding.ivProfileImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        // Set email profil
        binding.tvProfileEmail.text = auth.currentUser?.email ?: preferenceManager.getUserEmail()
        binding.tvProfileName.text = auth.currentUser?.displayName ?: "User"

        // Load Foto Profil Safely
        preferenceManager.getProfileImageUri()?.let { path ->
            loadProfileImage(path)
        }

        // Klik Foto Profil
        binding.cardProfileImage.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        // Ganti Username
        binding.btnChangeUsername.setOnClickListener {
            showChangeUsernameDialog()
        }

        // Ganti Password
        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Logika Dark Mode
        binding.switchDarkMode.isChecked = preferenceManager.isDarkMode()
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setDarkMode(isChecked)
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

    private fun showChangeUsernameDialog() {
        val input = android.widget.EditText(requireContext())
        input.setText(auth.currentUser?.displayName)
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.change_username))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                        displayName = newName
                    }
                    auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            binding.tvProfileName.text = newName
                            android.widget.Toast.makeText(requireContext(), getString(R.string.username_updated), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.change_password))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newPassword = input.text.toString()
                if (newPassword.length >= 6) {
                    auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.widget.Toast.makeText(requireContext(), getString(R.string.password_updated), android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(requireContext(), "Gagal: ${task.exception?.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    android.widget.Toast.makeText(requireContext(), getString(R.string.password_min_chars), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
