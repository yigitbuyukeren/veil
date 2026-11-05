package chat.simplex.common.veil.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.veil.design.*
import chat.simplex.common.veil.screens.chatlist.VeilIcon
import chat.simplex.common.veil.screens.settings.VeilSwitchRow
import chat.simplex.common.veil.screens.settings.VeilValueRow
import chat.simplex.common.views.chat.showUnsavedChangesAlert
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Veil contact preferences: replaces upstream's `ContactPreferencesView`.
 *
 * What upstream's screen does, all kept: the stored choices are turned into `ContactFeaturesAllowed`
 * (`contactUserPrefsToFeaturesAllowed`), edited in memory, and saved by `controller.apiSetContactPrefs` with
 * `contactFeaturesAllowedToPrefs`, after which `updateContact` refreshes the chat. Disappearing messages (switch and
 * time, custom time through upstream's picker), irreversible deletion, reactions and voice messages are offered, and calls
 * only when `VeilNetworkPolicy.callsAllowed()`. Leaving with unsaved changes asks upstream's "Save preferences?" question.
 */
@Composable
fun VeilContactPreferencesScreen(rhId: Long?, contactId: Long, close: () -> Unit) {
  val user = remember { chatModel.currentUser }.value
  val contact = remember(contactId) { derivedStateOf { (chatModel.getContactChat(contactId)?.chatInfo as? ChatInfo.Direct)?.contact } }.value
  if (contact == null || user == null) {
    // the contact was deleted while the screen was open
    LaunchedEffect(Unit) { close() }
    return
  }
  var featuresAllowed by rememberSaveable(contact, user, stateSaver = serializableSaver()) { mutableStateOf(contactUserPrefsToFeaturesAllowed(contact.mergedPreferences)) }
  var currentFeaturesAllowed by rememberSaveable(contact, user, stateSaver = serializableSaver()) { mutableStateOf(featuresAllowed) }
  val changed = featuresAllowed != currentFeaturesAllowed

  fun savePrefs(afterSave: () -> Unit = {}) {
    withBGApi {
      val prefs = contactFeaturesAllowedToPrefs(featuresAllowed)
      val toContact = chatModel.controller.apiSetContactPrefs(rhId, contact.contactId, prefs)
      if (toContact != null) {
        withContext(Dispatchers.Main) {
          chatModel.chatsContext.updateContact(rhId, toContact)
          currentFeaturesAllowed = featuresAllowed
        }
      }
      afterSave()
    }
  }

  val onBack = {
    when (prefsBackAction(changed)) {
      PrefsBack.Close -> close()
      PrefsBack.AskToSave -> showUnsavedChangesAlert({ savePrefs(close) }, close)
    }
  }

  VeilScreenScaffold(title = stringResource(MR.strings.veil_cprefs_title), onBack = onBack, subtitle = contact.displayName) {
    VeilCaption(stringResource(MR.strings.veil_cprefs_intro), Modifier.padding(bottom = 8.dp))
    preferenceFeatures(VeilNetworkPolicy.callsAllowed()).forEach { feature ->
      when (feature) {
        ChatFeature.TimedMessages -> TimedMessagesSection(
          featuresAllowed,
          contact.mergedPreferences.timedMessages,
          onChange = { featuresAllowed = it },
          storedTtl = currentFeaturesAllowed.timedMessagesTTL,
        )
        ChatFeature.FullDelete -> FeatureSection(feature, user.fullPreferences.fullDelete.allow, contact.mergedPreferences.fullDelete, featuresAllowed.fullDelete) {
          featuresAllowed = featuresAllowed.copy(fullDelete = it)
        }
        ChatFeature.Reactions -> FeatureSection(feature, user.fullPreferences.reactions.allow, contact.mergedPreferences.reactions, featuresAllowed.reactions) {
          featuresAllowed = featuresAllowed.copy(reactions = it)
        }
        ChatFeature.Voice -> FeatureSection(feature, user.fullPreferences.voice.allow, contact.mergedPreferences.voice, featuresAllowed.voice) {
          featuresAllowed = featuresAllowed.copy(voice = it)
        }
        ChatFeature.Calls -> FeatureSection(feature, user.fullPreferences.calls.allow, contact.mergedPreferences.calls, featuresAllowed.calls) {
          featuresAllowed = featuresAllowed.copy(calls = it)
        }
        else -> {}
      }
    }
    Spacer(Modifier.height(6.dp))
    VeilButton(
      stringResource(MR.strings.veil_cprefs_save),
      onClick = { savePrefs() },
      enabled = prefsActionsEnabled(changed),
      modifier = Modifier.padding(bottom = 10.dp),
    )
    VeilButton(
      stringResource(MR.strings.veil_cprefs_reset),
      onClick = { featuresAllowed = currentFeaturesAllowed },
      enabled = prefsActionsEnabled(changed),
      style = VeilButtonStyle.Secondary,
    )
  }
}

private fun featureIcon(feature: ChatFeature): ImageResource = when (feature) {
  ChatFeature.TimedMessages -> MR.images.ic_timer_filled
  ChatFeature.FullDelete -> MR.images.ic_delete_forever_filled
  ChatFeature.Reactions -> MR.images.ic_add_reaction_filled
  ChatFeature.Voice -> MR.images.ic_keyboard_voice_filled
  ChatFeature.Files -> MR.images.ic_draft_filled
  ChatFeature.Calls -> MR.images.ic_call_filled
}

/** Feature name with its icon; the icon says who can use it now (mint: you, warning: only your contact, grey: nobody). */
@Composable
private fun FeatureHeader(feature: ChatFeature, enabled: FeatureEnabled) {
  val colors = VeilTheme.colors
  val tint: Color = if (enabled.forUser) colors.mintInk else if (enabled.forContact) colors.warn else colors.ink3
  Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    VeilIcon(featureIcon(feature), tint, size = 20.dp)
    BasicText(feature.text, style = VeilTheme.type.rowTitle.copy(color = colors.ink))
  }
}

@Composable
private fun FeatureSection(
  feature: ChatFeature,
  userDefault: FeatureAllowed,
  pref: ContactUserPreference,
  allowed: ContactFeatureAllowed,
  onSelected: (ContactFeatureAllowed) -> Unit,
) {
  val enabled = FeatureEnabled.enabled(feature.asymmetric, user = SimpleChatPreference(allow = allowed.allowed), contact = pref.contactPreference)
  FeatureHeader(feature, enabled)
  VeilCard(contentPadding = PaddingValues(8.dp)) {
    VeilChoiceRow(
      title = stringResource(MR.strings.veil_cprefs_you_allow),
      options = ContactFeatureAllowed.values(userDefault).map { it to it.text },
      selected = allowed,
      onSelect = onSelected,
    )
    VeilValueRow(stringResource(MR.strings.veil_cprefs_contact_allows), sub = null, readOnly = false, value = pref.contactPreference.allow.text)
  }
  VeilCaption(feature.enabledDescription(enabled), Modifier.padding(bottom = 12.dp))
}

@Composable
private fun TimedMessagesSection(
  featuresAllowed: ContactFeaturesAllowed,
  pref: ContactUserPreferenceTimed,
  onChange: (ContactFeaturesAllowed) -> Unit,
  storedTtl: Int?,
) {
  val youAllow = featuresAllowed.timedMessagesAllowed
  val contactAllows = pref.contactPreference.allow == FeatureAllowed.YES || pref.contactPreference.allow == FeatureAllowed.ALWAYS
  val enabled = FeatureEnabled.enabled(
    ChatFeature.TimedMessages.asymmetric,
    user = TimedMessagesPreference(allow = if (youAllow) FeatureAllowed.YES else FeatureAllowed.NO),
    contact = pref.contactPreference,
  )
  FeatureHeader(ChatFeature.TimedMessages, enabled)
  VeilCard(contentPadding = PaddingValues(8.dp)) {
    VeilSwitchRow(
      title = stringResource(MR.strings.veil_cprefs_you_allow),
      checked = youAllow,
      onChange = { allow ->
        onChange(featuresAllowed.copy(timedMessagesAllowed = allow, timedMessagesTTL = timedMessagesTtlAfterToggle(allow, featuresAllowed.timedMessagesTTL, storedTtl)))
      },
    )
    VeilValueRow(stringResource(MR.strings.veil_cprefs_contact_allows), sub = null, readOnly = false, value = pref.contactPreference.allow.text)
    if (youAllow) {
      val ttl = featuresAllowed.timedMessagesTTL
      val deleteAfter = stringResource(MR.strings.veil_cprefs_delete_after)
      VeilChoiceRow(
        title = deleteAfter,
        options = timedTtlOptions(TimedMessagesPreference.ttlValues, ttl).map { it to timeText(it) },
        selected = ttl,
        onSelect = { onChange(featuresAllowed.copy(timedMessagesTTL = it)) },
        // upstream's own time picker
        custom = stringResource(MR.strings.veil_cprefs_custom) to {
          showCustomTimePickerDialog(
            mutableStateOf(ttl ?: DEFAULT_TIMED_TTL_SECONDS),
            title = deleteAfter,
            confirmButtonText = generalGetString(MR.strings.custom_time_picker_select),
            confirmButtonAction = { onChange(featuresAllowed.copy(timedMessagesTTL = it)) },
            cancel = {},
          )
        },
      )
    } else if (showContactTtl(youAllow, contactAllows)) {
      VeilValueRow(stringResource(MR.strings.veil_cprefs_delete_after), sub = null, readOnly = false, value = timeText(pref.contactPreference.ttl))
    }
  }
  VeilCaption(ChatFeature.TimedMessages.enabledDescription(enabled), Modifier.padding(bottom = 12.dp))
}
