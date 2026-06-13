package dev.atmos.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

suspend fun uploadAvatarToFirebase(context: Context, userId: String, uri: Uri): Result<String> = runCatching {
    // Sign in anonymously so Firebase Storage rules (auth != null) pass.
    // Anonymous users are persistent — this is a no-op on subsequent calls.
    val auth = FirebaseAuth.getInstance()
    if (auth.currentUser == null) {
        auth.signInAnonymously().await()
    }

    // Read, decode, and resize to max 400px to keep upload size reasonable.
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Cannot read selected image")
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("Cannot decode image")

    val (targetW, targetH) = if (original.width >= original.height) {
        400 to (400f * original.height / original.width).toInt().coerceAtLeast(1)
    } else {
        (400f * original.width / original.height).toInt().coerceAtLeast(1) to 400
    }

    // createScaledBitmap can throw OOM; try/finally guarantees original is always recycled.
    val scaled = try {
        Bitmap.createScaledBitmap(original, targetW, targetH, true)
    } catch (t: Throwable) {
        original.recycle()
        throw t
    }
    if (scaled !== original) original.recycle()

    // try/finally guarantees scaled is recycled whether compress succeeds or throws.
    val jpeg = try {
        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    } finally {
        scaled.recycle()
    }

    // Upload to Firebase Storage and return the permanent download URL.
    val ref = FirebaseStorage.getInstance().reference.child("avatars/$userId.jpg")
    ref.putBytes(jpeg).await()
    ref.downloadUrl.await().toString()
}
