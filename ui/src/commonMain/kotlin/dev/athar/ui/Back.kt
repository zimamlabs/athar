package dev.athar.ui

import androidx.compose.runtime.Composable

/** The system back gesture on Android; Escape on the desktop. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
