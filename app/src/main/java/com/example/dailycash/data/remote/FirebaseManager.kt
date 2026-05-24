package com.example.dailycash.data.remote

import com.example.dailycash.data.local.entity.TransactionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun backupTransaction(transaction: TransactionEntity) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("transactions").document(transaction.id.toString())
            .set(transaction)
    }

    fun syncFromFirestore(onSuccess: (List<TransactionEntity>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { result ->
                val transactions = result.toObjects(TransactionEntity::class.java)
                onSuccess(transactions)
            }
    }
}
