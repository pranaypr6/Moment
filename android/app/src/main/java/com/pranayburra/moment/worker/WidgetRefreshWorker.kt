package com.pranayburra.moment.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pranayburra.moment.widget.RelationshipWidget

/**
 * Periodic, lightweight refresh so the home-screen widget's "days together" count stays
 * accurate even if the app is never opened. Glance widgets don't repaint on their own just
 * because time passed - they only re-render when explicitly told to (RelationshipWidget.
 * forceUpdate) - so without this, the counter could sit stale for days until some unrelated
 * action (pause, anniversary change, etc.) happened to trigger an update. Scheduled at a
 * conservative interval since a day-counter doesn't need to be exact to the minute; this is
 * a correctness backstop; the normal case is an app open forcing a fresh update sooner.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RelationshipWidget.forceUpdate(applicationContext)
        return Result.success()
    }
}
