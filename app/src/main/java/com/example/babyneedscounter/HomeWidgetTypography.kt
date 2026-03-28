package com.example.babyneedscounter

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.util.TypedValue
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal data class SmallWidgetTextSizing(
    val labelSp: Float,
    val primarySp: Float,
)

internal data class FeedLargeWidgetTextSizing(
    val titleSp: Float,
    val primarySp: Float,
    val detailSp: Float,
    val footerSp: Float,
)

internal data class SleepLargeWidgetTextSizing(
    val titleSp: Float,
    val primarySp: Float,
    val footerSp: Float,
)

internal object HomeWidgetTypography {
    fun feedSmall(
        context: Context,
        options: Bundle?,
        model: FeedSmallWidgetModel,
    ): SmallWidgetTextSizing {
        return smallWidgetSizing(
            context = context,
            kind = HomeWidgetKind.FEED_SMALL,
            options = options,
            labelText = model.label,
            primaryText = model.primaryText,
        )
    }

    fun sleepSmall(
        context: Context,
        options: Bundle?,
        model: SleepSmallWidgetModel,
    ): SmallWidgetTextSizing {
        return smallWidgetSizing(
            context = context,
            kind = HomeWidgetKind.SLEEP_SMALL,
            options = options,
            labelText = model.state,
            primaryText = model.durationText,
        )
    }

    fun feedLarge(
        context: Context,
        options: Bundle?,
        model: FeedLargeWidgetModel,
    ): FeedLargeWidgetTextSizing {
        val contentBox = contentBoxPx(context, options, HomeWidgetKind.FEED_LARGE)
        val titleSp = largestSingleLineSp(
            context = context,
            text = model.title,
            maxWidthPx = contentBox.widthPx,
            maxHeightPx = contentBox.heightPx * 0.16f,
            minSp = 7f,
            maxSp = 11f,
            bold = true,
        )

        val detailSample = buildString {
            append(model.countdownText)
            if (model.countText.isNotBlank()) {
                append("  ")
                append(model.countText)
            }
        }
        val detailSp = largestSingleLineSp(
            context = context,
            text = detailSample,
            maxWidthPx = contentBox.widthPx,
            maxHeightPx = contentBox.heightPx * 0.16f,
            minSp = 7f,
            maxSp = 12f,
            bold = true,
        )

        val footerSp = if (model.lastFeedText.isBlank()) {
            0f
        } else {
            largestSingleLineSp(
                context = context,
                text = model.lastFeedText,
                maxWidthPx = contentBox.widthPx,
                maxHeightPx = contentBox.heightPx * 0.15f,
                minSp = 7f,
                maxSp = 11f,
                bold = false,
            )
        }

        val reservedHeightPx = lineHeightPx(context, titleSp, bold = true) +
            lineHeightPx(context, detailSp, bold = true) +
            if (footerSp > 0f) lineHeightPx(context, footerSp, bold = false) else 0f +
            dpToPx(context, 1f + 4f + if (footerSp > 0f) 2f else 0f)

        val primarySp = largestSingleLineSp(
            context = context,
            text = model.primaryText,
            maxWidthPx = contentBox.widthPx,
            maxHeightPx = (contentBox.heightPx - reservedHeightPx).coerceAtLeast(contentBox.heightPx * 0.32f),
            minSp = 8f,
            maxSp = 72f,
            bold = true,
        )

        return FeedLargeWidgetTextSizing(
            titleSp = titleSp.roundToTenth(),
            primarySp = primarySp.roundToTenth(),
            detailSp = detailSp.roundToTenth(),
            footerSp = footerSp.roundToTenth(),
        )
    }

    fun sleepLarge(
        context: Context,
        options: Bundle?,
        model: SleepLargeWidgetModel,
    ): SleepLargeWidgetTextSizing {
        val contentBox = contentBoxPx(context, options, HomeWidgetKind.SLEEP_LARGE)
        val titleSp = largestSingleLineSp(
            context = context,
            text = model.state,
            maxWidthPx = contentBox.widthPx,
            maxHeightPx = contentBox.heightPx * 0.16f,
            minSp = 7f,
            maxSp = 11f,
            bold = true,
        )
        val footerSp = largestSingleLineSp(
            context = context,
            text = model.previousSleepText,
            maxWidthPx = contentBox.widthPx,
            maxHeightPx = contentBox.heightPx * 0.15f,
            minSp = 7f,
            maxSp = 11f,
            bold = false,
        )

        val reservedHeightPx = lineHeightPx(context, titleSp, bold = true) +
            lineHeightPx(context, footerSp, bold = false) +
            dpToPx(context, 1f + 2f)

        val primarySp = largestSingleLineSp(
            context = context,
            text = model.durationText,
            maxWidthPx = contentBox.widthPx,
            maxHeightPx = (contentBox.heightPx - reservedHeightPx).coerceAtLeast(contentBox.heightPx * 0.4f),
            minSp = 8f,
            maxSp = 72f,
            bold = true,
        )

        return SleepLargeWidgetTextSizing(
            titleSp = titleSp.roundToTenth(),
            primarySp = primarySp.roundToTenth(),
            footerSp = footerSp.roundToTenth(),
        )
    }

    private fun smallWidgetSizing(
        context: Context,
        kind: HomeWidgetKind,
        options: Bundle?,
        labelText: String,
        primaryText: String,
    ): SmallWidgetTextSizing {
        val contentBox = contentBoxPx(context, options, kind)
        val labelSp = largestSingleLineSp(
            context = context,
            text = labelText,
            maxWidthPx = contentBox.widthPx,
            maxHeightPx = contentBox.heightPx * 0.24f,
            minSp = 7f,
            maxSp = 11f,
            bold = true,
        )
        val labelHeightPx = lineHeightPx(context, labelSp, bold = true)
        val primarySp = largestSingleLineSp(
            context = context,
            text = primaryText,
            maxWidthPx = contentBox.widthPx,
            maxHeightPx = (contentBox.heightPx - labelHeightPx - dpToPx(context, 1f))
                .coerceAtLeast(contentBox.heightPx * 0.55f),
            minSp = 8f,
            maxSp = 56f,
            bold = true,
        )
        return SmallWidgetTextSizing(
            labelSp = labelSp.roundToTenth(),
            primarySp = primarySp.roundToTenth(),
        )
    }

    private fun contentBoxPx(
        context: Context,
        options: Bundle?,
        kind: HomeWidgetKind,
    ): WidgetContentBoxPx {
        val density = context.resources.displayMetrics.density
        val boxDp = HomeWidgetSizingPolicy.contentBox(kind, resolveWidgetBoundsDp(options, kind))
        return WidgetContentBoxPx(
            widthPx = boxDp.widthDp * density,
            heightPx = boxDp.heightDp * density,
        )
    }

    private fun resolveWidgetBoundsDp(
        options: Bundle?,
        kind: HomeWidgetKind,
    ): WidgetBoundsDp {
        val fallback = HomeWidgetSizingPolicy.fallbackBounds(kind)
        val widthDp = listOf(
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0),
        )
            .mapNotNull { it?.takeIf { dimension -> dimension > 0 } }
            .minOrNull()
            ?.toFloat()
            ?: fallback.widthDp
        val heightDp = listOf(
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0),
        )
            .mapNotNull { it?.takeIf { dimension -> dimension > 0 } }
            .minOrNull()
            ?.toFloat()
            ?: fallback.heightDp
        return WidgetBoundsDp(widthDp = widthDp, heightDp = heightDp)
    }

    private fun largestSingleLineSp(
        context: Context,
        text: String,
        maxWidthPx: Float,
        maxHeightPx: Float,
        minSp: Float,
        maxSp: Float,
        bold: Boolean,
    ): Float {
        if (maxWidthPx <= 0f || maxHeightPx <= 0f) return minSp

        val measurementText = HomeWidgetSizingPolicy.measurementSample(text.ifBlank { "88" })
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        var low = minSp
        var high = max(maxSp, minSp)
        repeat(18) {
            val candidate = (low + high) / 2f
            paint.textSize = spToPx(context, candidate)
            val widthPx = paint.measureText(measurementText)
            val heightPx = ceil(paint.fontMetrics.descent - paint.fontMetrics.ascent)
            if (widthPx <= maxWidthPx && heightPx <= maxHeightPx) {
                low = candidate
            } else {
                high = candidate
            }
        }

        return min(low, maxSp)
    }

    private fun lineHeightPx(
        context: Context,
        textSizeSp: Float,
        bold: Boolean,
    ): Float {
        if (textSizeSp <= 0f) return 0f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textSize = spToPx(context, textSizeSp)
        }
        return ceil(paint.fontMetrics.descent - paint.fontMetrics.ascent)
    }

    private fun dpToPx(
        context: Context,
        valueDp: Float,
    ): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            valueDp,
            context.resources.displayMetrics,
        )
    }

    private fun spToPx(
        context: Context,
        valueSp: Float,
    ): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            valueSp,
            context.resources.displayMetrics,
        )
    }

    private fun Float.roundToTenth(): Float {
        return (this * 10f).toInt() / 10f
    }
}

private data class WidgetContentBoxPx(
    val widthPx: Float,
    val heightPx: Float,
)
