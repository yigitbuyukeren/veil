package chat.simplex.common.veil.screens.settings

import androidx.compose.runtime.Composable

// The Veil settings screens are Android only (VeilUi.settings is ignored on desktop); these are never called there.
@Composable
actual fun rememberScreenProtectionApplier(): (Boolean) -> Unit = { }

@Composable
actual fun rememberAppLanguageApplier(): (String) -> Unit = { }
