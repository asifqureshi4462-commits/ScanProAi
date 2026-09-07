package com.example.data.auth

import android.app.Activity
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isPremium: Boolean = false,
    val cloudUsageBytes: Long = 0L,
    val storageLimitBytes: Long = 5_000_000_000L // 5 GB free tier
)

/**
 * Handles authentication against Firebase.
 *
 * IMPORTANT: This class is now fail-closed. If Firebase is not configured
 * (no google-services.json) or a Firebase call fails, the user is NOT
 * silently logged in as a fake/demo account. Callers receive onError and
 * must surface it to the user.
 */
class AuthRepository {

    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Holds the verification ID between sendOtp() and verifyOtp()
    private var pendingVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun firebaseUnavailableMessage() =
        "Sign-in service is not configured yet (missing google-services.json). " +
            "Add your Firebase config to app/ to enable real accounts, or continue as Guest."

    fun loginWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!isValidEmail(email)) {
            onError("Please enter a valid email address.")
            return
        }
        if (pass.length < 6) {
            onError("Password must be at least 6 characters.")
            return
        }
        val auth = firebaseAuth
        if (auth == null) {
            onError(firebaseUnavailableMessage())
            return
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    onError("Login failed: no user returned.")
                    return@addOnSuccessListener
                }
                _currentUser.value = UserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = user.email ?: email,
                    photoUrl = user.photoUrl?.toString(),
                    isPremium = false
                )
                onSuccess()
            }
            .addOnFailureListener { e ->
                // FAIL CLOSED: do not log the user in on failure.
                onError(e.message ?: "Login failed. Please check your credentials and try again.")
            }
    }

    fun signupWithEmail(name: String, email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (name.isBlank()) {
            onError("Please enter your name.")
            return
        }
        if (!isValidEmail(email)) {
            onError("Please enter a valid email address.")
            return
        }
        if (pass.length < 6) {
            onError("Password must be at least 6 characters.")
            return
        }
        val auth = firebaseAuth
        if (auth == null) {
            onError(firebaseUnavailableMessage())
            return
        }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    onError("Signup failed: no user created.")
                    return@addOnSuccessListener
                }
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates)
                _currentUser.value = UserProfile(
                    uid = user.uid,
                    displayName = name,
                    email = email,
                    isPremium = false
                )
                onSuccess()
            }
            .addOnFailureListener { e ->
                // FAIL CLOSED: no account created means no local session either.
                onError(e.message ?: "Could not create account. Please try again.")
            }
    }

    fun loginAsGuest() {
        _currentUser.value = UserProfile(
            uid = "guest_${System.currentTimeMillis()}",
            displayName = "Guest User",
            email = "guest@scanpro.ai",
            isPremium = false
        )
    }

    /**
     * Step 1 of real phone auth: sends a real SMS code via Firebase Phone Auth.
     * Requires an Activity and a configured Firebase project (SHA-1 + Phone Auth enabled).
     */
    fun sendOtp(
        phone: String,
        activity: Activity,
        onCodeSent: () -> Unit,
        onAutoVerified: () -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            onError(firebaseUnavailableMessage())
            return
        }
        if (phone.isBlank() || phone.length < 8) {
            onError("Please enter a valid phone number including country code.")
            return
        }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Android auto-retrieved the SMS code; sign in immediately.
                signInWithPhoneCredential(credential, onAutoVerified, onError)
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                onError(e.message ?: "Phone verification failed.")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                pendingVerificationId = verificationId
                resendToken = token
                onCodeSent()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Step 2 of real phone auth: verifies the code the user typed against the
     * verification ID Firebase issued in sendOtp(). No code is ever accepted
     * without a matching, server-issued verification ID.
     */
    fun verifyOtp(phone: String, otp: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onError(firebaseUnavailableMessage())
            return
        }
        val verificationId = pendingVerificationId
        if (verificationId == null) {
            onError("Please request an OTP code first.")
            return
        }
        if (otp.length < 4) {
            onError("Please enter the full verification code.")
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithPhoneCredential(credential, onSuccess, onError)
    }

    private fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = firebaseAuth ?: run {
            onError(firebaseUnavailableMessage())
            return
        }
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    onError("Verification failed: no user returned.")
                    return@addOnSuccessListener
                }
                _currentUser.value = UserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: "User (${user.phoneNumber ?: ""})",
                    email = user.email ?: "${user.phoneNumber}@scanpro.ai",
                    isPremium = false
                )
                pendingVerificationId = null
                onSuccess()
            }
            .addOnFailureListener { e ->
                // FAIL CLOSED: wrong/expired code never logs the user in.
                onError(e.message ?: "Invalid or expired code. Please try again.")
            }
    }

    fun logout() {
        firebaseAuth?.signOut()
        _currentUser.value = null
    }

    /**
     * NOTE: This flips a local flag only. Real entitlement checks for paid
     * features must be verified server-side (e.g. via Firestore + a purchase
     * receipt validated in Cloud Functions) before unlocking premium
     * behavior anywhere that matters. This client-side flag should be
     * treated as a UI hint only, not a security boundary.
     */
    fun upgradeToPremium() {
        _currentUser.value = _currentUser.value?.copy(
            isPremium = true,
            storageLimitBytes = 100_000_000_000L // 100 GB
        )
    }
}
