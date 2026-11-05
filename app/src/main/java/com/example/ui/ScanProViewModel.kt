fun toggleOfflineMode() {
    isOfflineMode.value = !isOfflineMode.value
}

fun addCapturedBitmap(bitmap: Bitmap) {
    capturedBitmaps.value = capturedBitmaps.value + bitmap
}

fun updateCapturedBitmap(index: Int, bitmap: Bitmap) {
    val current = capturedBitmaps.value
    if (index !in current.indices) return

    capturedBitmaps.value = current.toMutableList().apply {
        this[index] = bitmap
    }
}

fun removeCapturedBitmap(index: Int) {
    val current = capturedBitmaps.value
    if (index !in current.indices) return

    capturedBitmaps.value = current.toMutableList().apply {
        removeAt(index)
    }
}