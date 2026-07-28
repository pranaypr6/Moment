package com.pranayburra.moment.util

import android.content.Context
import android.view.Gravity
import android.widget.Toast

/**
 * Shows a short Toast anchored a bit above the screen's bottom edge so it doesn't
 * visually collide with the app's own bottom navigation bar. A default-gravity Toast
 * sits very close to the bottom edge, which overlaps the custom Compose bottom nav
 * (Moments / Us / Hub tabs) since that's part of our UI, not the system nav bar.
 */
fun showAppToast(context: Context, message: String, durationOffsetDp: Int = 96) {
    val offsetPx = (durationOffsetDp * context.resources.displayMetrics.density).toInt()
    Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
        setGravity(Gravity.BOTTOM, 0, offsetPx)
    }.show()
}
