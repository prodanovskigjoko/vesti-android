package mk.vesti.app.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.core.content.ContextCompat
import mk.vesti.app.R

/** False on Android 13+ until the reader allows notifications. */
internal fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** The full-colour app icon (red circle, white "В") shown beside the notification text. */
internal fun appIcon(context: Context): Bitmap {
    val size = (64 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ic_launcher_background)
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.apply {
        setBounds(0, 0, size, size)
        draw(canvas)
    }
    return bitmap
}
