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
import mk.vesti.app.data.OutagesRepository
import mk.vesti.app.data.outageWhen
import mk.vesti.core.OutageMatch
import mk.vesti.core.OutageMatcher
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Every few hours: fetch EVN's planned power cuts and warn about those that hit a saved address within the
 * next day. Each outage is announced once per address. At night only imminent cuts are announced, so the
 * warning for tomorrow usually arrives the evening before rather than at 3 a.m.
 */
class OutageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = OutagesRepository(applicationContext)
        val addresses = repository.addresses.value
        if (addresses.isEmpty()) return Result.success()
        val outages = runCatching { repository.fetch() }.getOrElse { return Result.retry() }

        val now = OutagesRepository.now()
        val matches = OutageMatcher().match(outages, addresses, now)
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val notified = prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()
        val night = now.hour < 7 || now.hour >= 22
        val due = matches.filter { m ->
            val ahead = Duration.between(now, m.outage.start)
            key(m) !in notified && !ahead.isNegative &&
                ahead <= Duration.ofHours(if (night) NIGHT_HOURS else AHEAD_HOURS)
        }
        due.take(MAX_PER_RUN).forEach { post(it) }

        // Remember only outages EVN still lists, so the set stays small.
        val current = matches.map(::key).toSet()
        prefs.edit().putStringSet(KEY_NOTIFIED, (notified intersect current) + due.map(::key)).apply()
        return Result.success()
    }

    @SuppressLint("MissingPermission") // canNotify checks it on Android 13+
    private fun post(match: OutageMatch) {
        val ctx = applicationContext
        if (!canNotify(ctx)) return
        ensureChannel(ctx)
        val id = key(match).hashCode()
        val open = PendingIntent.getActivity(
            ctx,
            id,
            Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_OUTAGES, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val outage = match.outage
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(ctx, R.color.ic_launcher_background))
            .setLargeIcon(appIcon(ctx))
            .setContentTitle("Прекин на струја · ${match.address.label}")
            .setContentText(outageWhen(outage).replaceFirstChar { it.uppercase() })
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${outageWhen(outage).replaceFirstChar { it.uppercase() }}\n${outage.place}: ${outage.area}",
                ),
            )
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, notification)
    }

    companion object {
        private const val UNIQUE_NAME = "power-outages"
        private const val CHANNEL_ID = "outages"
        private const val PREFS = "notifications"
        private const val KEY_NOTIFIED = "notified_outages"
        private const val AHEAD_HOURS = 30L
        private const val NIGHT_HOURS = 9L
        private const val MAX_PER_RUN = 3

        private fun key(m: OutageMatch) = "${m.outage.id}|${m.address.label}"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<OutageWorker>(3, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun ensureChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Прекини на струја", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Планирани прекини на ЕВН на вашите зачувани адреси" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
