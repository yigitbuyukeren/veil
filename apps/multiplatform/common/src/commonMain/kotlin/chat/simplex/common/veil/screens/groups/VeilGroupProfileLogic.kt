package chat.simplex.common.veil.screens.groups

import chat.simplex.common.model.GroupProfile

/*
 * Pure rules of the Veil group profile screen, the same checks upstream's `GroupProfileView` makes. The checks that need
 * the chat core (name validity, description length) are passed in so they can be tested.
 */

/** What the fields hold while editing. */
data class VeilGroupProfileDraft(val displayName: String, val fullName: String, val shortDescr: String, val image: String?)

fun draftOf(p: GroupProfile): VeilGroupProfileDraft = VeilGroupProfileDraft(p.displayName, p.fullName, p.shortDescr ?: "", p.image)

fun isDraftUnchanged(d: VeilGroupProfileDraft, p: GroupProfile): Boolean =
  d.displayName.trim() == p.displayName &&
    d.fullName.trim() == p.fullName &&
    d.shortDescr.trim() == (p.shortDescr ?: "") &&
    p.image == d.image

/** The name is fine when it was not touched, or when upstream's `isValidDisplayName` accepts it. */
fun isValidNewName(displayName: String, p: GroupProfile, isValid: (String) -> Boolean): Boolean =
  displayName == p.displayName || isValid(displayName.trim())

fun canUpdateProfile(d: VeilGroupProfileDraft, p: GroupProfile, isValid: (String) -> Boolean, bioFits: (String) -> Boolean): Boolean =
  d.displayName.trim().isNotEmpty() && isValidNewName(d.displayName, p, isValid) && bioFits(d.shortDescr)

fun canSaveProfile(d: VeilGroupProfileDraft, p: GroupProfile, isValid: (String) -> Boolean, bioFits: (String) -> Boolean): Boolean =
  !isDraftUnchanged(d, p) && canUpdateProfile(d, p, isValid, bioFits)

enum class VeilProfileClose { Close, AskToSave }

/** Leaving closes at once when nothing changed or the edits could not be saved anyway (as upstream); otherwise asks. */
fun profileCloseAction(d: VeilGroupProfileDraft, p: GroupProfile, isValid: (String) -> Boolean, bioFits: (String) -> Boolean): VeilProfileClose =
  if (isDraftUnchanged(d, p) || !canUpdateProfile(d, p, isValid, bioFits)) VeilProfileClose.Close else VeilProfileClose.AskToSave

/** The profile sent to the core: trimmed texts, empty description as null. */
fun applyDraft(p: GroupProfile, d: VeilGroupProfileDraft): GroupProfile = p.copy(
  displayName = d.displayName.trim(),
  fullName = d.fullName.trim(),
  shortDescr = d.shortDescr.trim().ifEmpty { null },
  image = d.image,
)

/** Upstream shows the full-name field only when the group has a full name different from its name. */
fun showFullNameField(p: GroupProfile): Boolean = p.fullName.trim().isNotEmpty() && p.fullName.trim() != p.displayName.trim()
