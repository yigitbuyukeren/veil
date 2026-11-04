package chat.simplex.common.veil.screens.settings

import androidx.compose.runtime.Composable

/** Returns a function that applies the screen protection (FLAG_SECURE on Android) after the preference was changed. */
@Composable
expect fun rememberScreenProtectionApplier(): (Boolean) -> Unit

/**
 * Returns a function that stores the app language ("system" or a language tag) and applies it.
 * Android recreates the activity; the Veil settings are Android only, so desktop does nothing.
 */
@Composable
expect fun rememberAppLanguageApplier(): (String) -> Unit
