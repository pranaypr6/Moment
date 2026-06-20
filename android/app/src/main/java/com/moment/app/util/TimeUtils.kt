package com.moment.app.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun getRelativeTimeSpan(isoTimestamp: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(isoTimestamp) ?: return "Just now"
            
            val now = System.currentTimeMillis()
            val time = date.time
            
            DateUtils.getRelativeTimeSpanString(
                time,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (e: Exception) {
            "Recently"
        }
    }

    fun getRelativeTimeSpan(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMillis
        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return "Just now"
        }
        return DateUtils.getRelativeTimeSpanString(
            timeMillis,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}
