package com.example.system

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.IconCompat
import com.example.container.ShortcutLaunchActivity
import com.example.data.CloneCatalog
import com.example.data.ClonedSpace

sealed class ShortcutPinResult {
    data object Requested : ShortcutPinResult()
    data object NotSupported : ShortcutPinResult()
    data class Failed(val reason: String) : ShortcutPinResult()
}

object ShortcutHelper {

    const val ACTION_LAUNCH_CLONE = "com.aistudio.dualdock.ACTION_LAUNCH_CLONE"
    const val EXTRA_CLONE_ID = "extra_clone_id"

    fun isPinningSupported(context: Context): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    fun pinCloneShortcut(
        context: Context,
        space: ClonedSpace,
        appIconDrawable: Drawable?
    ): ShortcutPinResult {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return ShortcutPinResult.NotSupported
        }

        return try {
            val bitmap = renderBadgedShortcutBitmap(space, appIconDrawable)
            val launchIntent = Intent(context, ShortcutLaunchActivity::class.java).apply {
                action = ACTION_LAUNCH_CLONE
                putExtra(EXTRA_CLONE_ID, space.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val shortcutInfo = ShortcutInfoCompat.Builder(context, "dualdock_clone_${space.id}")
                .setShortLabel(space.displayName.take(18))
                .setLongLabel("${space.displayName} (DualDock)")
                .setIcon(IconCompat.createWithBitmap(bitmap))
                .setIntent(launchIntent)
                .build()

            val accepted = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            if (accepted) ShortcutPinResult.Requested
            else ShortcutPinResult.Failed("Launcher declined shortcut request.")
        } catch (e: Exception) {
            ShortcutPinResult.Failed(e.message ?: "Unable to create Home Screen shortcut.")
        }
    }

    fun renderBadgedShortcutBitmap(
        space: ClonedSpace,
        appIconDrawable: Drawable?,
        sizePx: Int = 192
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = sizePx.toFloat()

        // 1. Deep Aurora Indigo rounded-squircle base
        val baseRect = RectF(s * 0.04f, s * 0.04f, s * 0.96f, s * 0.96f)
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121329")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(baseRect, s * 0.25f, s * 0.25f, basePaint)

        // Luminous accent rim
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = space.badgeColorHex.toInt()
            alpha = 160
            style = Paint.Style.STROKE
            strokeWidth = s * 0.028f
        }
        canvas.drawRoundRect(baseRect, s * 0.25f, s * 0.25f, borderPaint)

        // 2. App icon or branded monogram centerpiece
        if (appIconDrawable != null) {
            val pad = (s * 0.17f).toInt()
            appIconDrawable.setBounds(pad, pad, sizePx - pad, sizePx - pad)
            appIconDrawable.draw(canvas)
        } else {
            val preset = CloneCatalog.findByKey(space.presetKey)
            val brandColor = preset?.brandColorHex?.toInt() ?: space.badgeColorHex.toInt()
            val monogram = preset?.monogram ?: space.displayName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifEmpty { "DD" }

            val innerRect = RectF(s * 0.17f, s * 0.17f, s * 0.83f, s * 0.83f)
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = brandColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(innerRect, s * 0.19f, s * 0.19f, innerPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = s * 0.30f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textY = s * 0.50f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(monogram, s * 0.50f, textY, textPaint)
        }

        // 3. Bottom-right luminous custom DualDock badge
        val badgeRadius = s * 0.21f
        val cx = s * 0.77f
        val cy = s * 0.77f

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#090916")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, badgeRadius + s * 0.04f, ringPaint)

        val badgeColorInt = space.badgeColorHex.toInt()
        val badgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColorInt
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, badgeRadius, badgeFillPaint)

        val badgeLabel = space.badgeText.trim().take(2).ifEmpty { "2" }
        val badgeInk = if (ColorUtils.calculateLuminance(badgeColorInt) > 0.5) {
            Color.parseColor("#090916")
        } else {
            Color.WHITE
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeInk
            textSize = if (badgeLabel.length == 1) s * 0.23f else s * 0.17f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeTextY = cy - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
        canvas.drawText(badgeLabel, cx, badgeTextY, badgeTextPaint)

        return bitmap
    }
}
