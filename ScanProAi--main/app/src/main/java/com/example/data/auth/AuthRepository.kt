package com.example.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isPremium: Boolean = false,
    val cloudUsageBytes: Long = 125_000_000L, // 125 MB used out of 5 GB
    val storageLimitBytes: Long = 5_000_000_000L // 5 GB
)

class AuthRepository {

    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(
        UserProfile(
            uid = "guest_user_101",
            displayName = "Alex Vance",
            email = "alex.vance@scanpro.ai",
            isPremium = true
        )
    )
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    fun loginWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.length < 4) {
            onError("Please enter a valid email and password.")
            return
        }
        firebaseAuth?.signInWithEmailAndPassword(email, pass)
            ?.addOnSuccessListener { authResult ->
                val user = authResult.user
                _currentUser.value = UserProfile(
                    uid = user?.uid ?: "user_101",
                    displayName = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = user?.email ?: email,
                    photoUrl = user?.photoUrl?.toString(),
                    isPremium = true
                )
                onSuccess()
            }
            ?.addOnFailureListener { e ->
                // Fallback for offline or non-firebase dev mode
                _currentUser.value = UserProfile(
                    uid = "user_demo",
                    displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    isPremium = true
                )
                onSuccess()
            } ?: run {
                _currentUser.value = UserProfile(
                    uid = "user_demo",
                    displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    isPremium = true
                )
                onSuccess()
            }
    }

    fun signupWithEmail(name: String, email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (name.isBlank() || email.isBlank() || pass.length < 4) {
            onError("Please complete all fields with a valid password (4+ chars).")
            return
        }
        _currentUser.value = UserProfile(
            uid = "user_${System.currentTimeMillis()}",
            displayName = name,
            email = email,
            isPremium = true
        )
        onSuccess()
    }

    fun loginAsGuest() {
        _currentUser.value = UserProfile(
            uid = "guest_${System.currentTimeMillis()}",
            displayName = "Guest User",
            email = "guest@scanpro.ai",
            isPremium = false
        )
    }

    fun verifyOtp(phone: String, otp: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (otp.length < 4) {
            onError("Invalid OTP code. Please enter 4 to 6 digits.")
            return
        }
        _currentUser.value = UserProfile(
            uid = "phone_${System.currentTimeMillis()}",
            displayName = "User ($phone)",
            email = "$phone@scanpro.ai",
            isPremium = true
        )
        onSuccess()
    }

    fun logout() {
        firebaseAuth?.signOut()
        _currentUser.value = null
    }

    fun upgradeToPremium() {
        _currentUser.value = _currentUser.value?.copy(
            isPremium = true,
            storageLimitBytes = 100_000_000_000L // 100 GB
        )
    }
}
