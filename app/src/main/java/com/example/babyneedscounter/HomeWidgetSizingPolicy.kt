package com.example.babyneedscounter

internal data class WidgetBoundsDp(
    val widthDp: Float,
    val heightDp: Float,
)

internal data class WidgetContentBoxDp(
    val widthDp: Float,
    val heightDp: Float,
)

private data class HomeWidgetFrameSpec(
    val fallbackWidthDp: Float,
    val fallbackHeightDp: Float,
    val actionWeight: Float,
    val contentWeight: Float,
    val rootHorizontalPaddingDp: Float,
    val rootVerticalPaddingDp: Float,
    val contentGapDp: Float,
    val contentHorizontalPaddingDp: Float,
    val contentVerticalPaddingDp: Float,
    val measurementGuardDp: Float,
)

internal object HomeWidgetSizingPolicy {
    fun fallbackBounds(kind: HomeWidgetKind): WidgetBoundsDp {
        val spec = frameSpec(kind)
        return WidgetBoundsDp(
            widthDp = spec.fallbackWidthDp,
            heightDp = spec.fallbackHeightDp,
        )
    }

    fun contentBox(kind: HomeWidgetKind, boundsDp: WidgetBoundsDp): WidgetContentBoxDp {
        val spec = frameSpec(kind)
        val safeWidthDp = boundsDp.widthDp.takeIf { it > 0f } ?: spec.fallbackWidthDp
        val safeHeightDp = boundsDp.heightDp.takeIf { it > 0f } ?: spec.fallbackHeightDp

        val horizontalSpaceDp = (safeWidthDp - spec.rootHorizontalPaddingDp - spec.contentGapDp)
            .coerceAtLeast(0f)
        val contentOuterWidthDp = horizontalSpaceDp * (spec.contentWeight / (spec.actionWeight + spec.contentWeight))
        val contentInnerWidthDp = (
            contentOuterWidthDp -
                spec.contentHorizontalPaddingDp -
                spec.measurementGuardDp
            ).coerceAtLeast(0f)
        val contentInnerHeightDp = (
            safeHeightDp -
                spec.rootVerticalPaddingDp -
                spec.contentVerticalPaddingDp -
                spec.measurementGuardDp
            ).coerceAtLeast(0f)

        return WidgetContentBoxDp(
            widthDp = contentInnerWidthDp,
            heightDp = contentInnerHeightDp,
        )
    }

    fun measurementSample(text: String): String {
        return buildString(text.length) {
            text.forEach { character ->
                append(if (character.isDigit()) '8' else character)
            }
        }
    }

    private fun frameSpec(kind: HomeWidgetKind): HomeWidgetFrameSpec {
        return when (kind) {
            HomeWidgetKind.FEED_SMALL,
            HomeWidgetKind.SLEEP_SMALL,
            -> HomeWidgetFrameSpec(
                fallbackWidthDp = 120f,
                fallbackHeightDp = 56f,
                actionWeight = 0.28f,
                contentWeight = 0.72f,
                rootHorizontalPaddingDp = 8f,
                rootVerticalPaddingDp = 8f,
                contentGapDp = 4f,
                contentHorizontalPaddingDp = 8f,
                contentVerticalPaddingDp = 8f,
                measurementGuardDp = 2f,
            )

            HomeWidgetKind.FEED_LARGE,
            HomeWidgetKind.SLEEP_LARGE,
            -> HomeWidgetFrameSpec(
                fallbackWidthDp = 250f,
                fallbackHeightDp = 110f,
                actionWeight = 0.22f,
                contentWeight = 0.78f,
                rootHorizontalPaddingDp = 10f,
                rootVerticalPaddingDp = 10f,
                contentGapDp = 4f,
                contentHorizontalPaddingDp = 12f,
                contentVerticalPaddingDp = 12f,
                measurementGuardDp = 2f,
            )
        }
    }
}
