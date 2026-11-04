package chat.simplex.common.veil.data

import chat.simplex.common.model.*
import chat.simplex.common.platform.chatController
import chat.simplex.common.platform.chatModel
import chat.simplex.common.views.helpers.withBGApi
import chat.simplex.common.views.onboarding.OnboardingStage

/**
 * Operators with `enabled` set for [selectedOperatorIds], with the storage/proxy roles guaranteed on at least one of
 * them, or null when there is nothing to enable. Same rules as `enabledOperators` in the upstream onboarding
 * (ChooseServerOperators.kt), which is private there.
 */
fun operatorsWithRequiredRoles(operators: List<ServerOperator>, selectedOperatorIds: Set<Long>): List<ServerOperator>? {
  if (operators.isEmpty()) return null
  val ops = ArrayList(operators)
  for (i in ops.indices) {
    val op = ops[i]
    ops[i] = op.copy(enabled = selectedOperatorIds.contains(op.operatorId))
  }
  val haveSMPStorage = ops.any { it.enabled && it.smpRoles.storage }
  val haveSMPProxy = ops.any { it.enabled && it.smpRoles.proxy }
  val haveXFTPStorage = ops.any { it.enabled && it.xftpRoles.storage }
  val haveXFTPProxy = ops.any { it.enabled && it.xftpRoles.proxy }
  if (haveSMPStorage && haveSMPProxy && haveXFTPStorage && haveXFTPProxy) return ops
  val first = ops.indexOfFirst { it.enabled }
  if (first == -1) return null
  var op = ops[first]
  if (!haveSMPStorage) op = op.copy(smpRoles = op.smpRoles.copy(storage = true))
  if (!haveSMPProxy) op = op.copy(smpRoles = op.smpRoles.copy(proxy = true))
  if (!haveXFTPStorage) op = op.copy(xftpRoles = op.xftpRoles.copy(storage = true))
  if (!haveXFTPProxy) op = op.copy(xftpRoles = op.xftpRoles.copy(proxy = true))
  ops[first] = op
  return ops
}

/** Ids of the operators that are enabled by default (what the upstream onboarding preselects). */
fun defaultOnboardingOperatorIds(operators: List<ServerOperator>): Set<Long> =
  operators.filter { it.enabled }.map { it.operatorId }.toSet()

/**
 * Accepts the current conditions of use for [selectedOperatorIds] and finishes onboarding, with the same controller
 * calls and order as upstream `acceptConditions` (ChooseServerOperators.kt): acceptConditions, then setServerOperators,
 * then `OnboardingStage.OnboardingComplete`. [onFinished] runs once the stage is set; [onFailed] when a call failed
 * (the controller has already shown an alert for a failed accept).
 */
fun acceptOnboardingConditions(selectedOperatorIds: Set<Long>, onFailed: () -> Unit = {}, onFinished: () -> Unit = {}) {
  withBGApi {
    val conditionsId = chatModel.conditions.value.currentConditions.conditionsId
    val r = chatController.acceptConditions(chatModel.remoteHostId(), conditionsId = conditionsId, operatorIds = selectedOperatorIds.toList())
    if (r == null) {
      onFailed()
      return@withBGApi
    }
    chatModel.conditions.value = r
    val enabledOps = operatorsWithRequiredRoles(r.serverOperators, selectedOperatorIds)
    if (enabledOps != null) {
      val r2 = chatController.setServerOperators(rh = chatModel.remoteHostId(), operators = enabledOps)
      if (r2 == null) {
        onFailed()
        return@withBGApi
      }
      chatModel.conditions.value = r2
    }
    chatController.appPrefs.onboardingStage.set(OnboardingStage.OnboardingComplete)
    onFinished()
  }
}
