package mk.vesti.app.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import mk.vesti.app.MainActivity
import mk.vesti.app.R
import mk.vesti.app.data.NewsRepository
import mk.vesti.core.BreakingNewsDetector
import mk.vesti.core.Story
import java.util.concurrent.TimeUnit

/** Every ~30 minutes: load the feeds and notify about stories that 5+ outlets are covering. */
class BreakingNewsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val stories = runCatching { NewsRepository(applicationContext).load().stories }
            .getOrElse { return Result.retry() }
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val notified = prefs.getStringSet(KEY_LINKS, emptySet()).orEmpty()
        val now = System.currentTimeMillis()
        val breaking = BreakingNewsDetector().detect(stories, notified, now).take(MAX_PER_RUN)
        if (breaking.isEmpty()) return Result.success()

        breaking.forEach { post(it) }
        // Keep the remembered links bounded: only links still present in the current feed matter.
        val current = stories.flatMap { s -> s.articles.map { it.link } }.toSet()
        val updated = (notified intersect current) + breaking.flatMap { s -> s.articles.map { it.link } }
        prefs.edit().putStringSet(KEY_LINKS, updated).apply()
        return Result.success()
    }

    @SuppressLint("MissingPermission") // canNotify checks it on Android 13+
    private fun post(story: Story) {
        val ctx = applicationContext
        if (!canNotify(ctx)) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx,
            story.lead.link.hashCode(),
            Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_STORY_LINK, story.lead.link)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(ctx, R.color.ic_launcher_background))
            .setLargeIcon(appIcon(ctx))
            .setContentTitle(story.lead.title)
            .setContentText("Пишуваат ${story.sourceCount} медиуми · ${story.category.label}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(story.lead.summary.ifBlank { story.lead.title }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(ctx).notify(story.lead.link.hashCode(), notification)
    }

    companion object {
        private const val UNIQUE_NAME = "breaking-news"
        private const val CHANNEL_ID = "breaking"
        private const val PREFS = "notifications"
        private const val KEY_LINKS = "notified_links"
        private const val MAX_PER_RUN = 2

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BreakingNewsWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun ensureChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Големи вести", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Кога 5 или повеќе медиуми пишуваат за истата вест" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
