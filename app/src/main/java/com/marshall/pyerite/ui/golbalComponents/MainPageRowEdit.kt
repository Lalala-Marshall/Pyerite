package com.marshall.pyerite.ui.golbalComponents

import androidx.compose.runtime.compositionLocalOf

/**
 * When provided, a [BaseLazyColumnItem] replaces its chevron with a visibility
 * control and uses [onToggle] for the whole row.
 */
internal data class MainPageRowEdit(
    val hidden: Boolean,
    val onToggle: () -> Unit,
    val contentDescription: String,
)

internal val LocalMainPageRowEdit = compositionLocalOf<MainPageRowEdit?> { null }

internal object MainPageRowEditConfig {
    const val HIDDEN_CONTENT_ALPHA = 0.4f
}
