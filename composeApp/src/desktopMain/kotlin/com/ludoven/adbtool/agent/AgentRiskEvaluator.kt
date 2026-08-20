package com.ludoven.adbtool.agent

data class AgentRiskAssessment(
    val level: AgentRiskLevel,
    val reason: String = ""
)

class AgentRiskEvaluator(
    private val approvalPreferences: AgentApprovalPreferences = AgentApprovalRuntime.preferences
) {
    fun evaluate(
        action: AgentAction,
        observation: AgentObservation,
        localCapabilityReason: String? = null
    ): AgentRiskAssessment {
        val targetNode = actionTargetNode(action, observation)
        val targetText = targetNode.targetText()
        val canCommitUiAction = action is AgentAction.Tap || action is AgentAction.TapElement
        if ((action is AgentAction.InputText && targetNode?.password == true) ||
            canCommitUiAction && (
                action.meta.operationKind in BLOCKED_OPERATION_KINDS || targetText.hasBlockedRiskTerm()
            )
        ) {
            return AgentRiskAssessment(
                AgentRiskLevel.BLOCKED,
                "Payments, account changes, and password entry are outside the enabled Agent scope"
            )
        }
        if (!localCapabilityReason.isNullOrBlank()) {
            return AgentRiskAssessment(AgentRiskLevel.CONFIRMATION_REQUIRED, localCapabilityReason)
        }
        if (action.requiresConfirmation) {
            return AgentRiskAssessment(
                AgentRiskLevel.CONFIRMATION_REQUIRED,
                "This device-management action can change device or application data"
            )
        }

        val modelRisk = canCommitUiAction && action.meta.operationKind == AgentOperationKind.DELETE
        val explicitTargetRisk = canCommitUiAction && targetText.hasDestructiveRiskTerm()
        val cautiousTextRisk = canCommitUiAction && targetText.hasCautiousOnlyRiskTerm()
        val requiresConfirmation = modelRisk || explicitTargetRisk ||
            (approvalPreferences.policy.value == AgentApprovalPolicy.CAUTIOUS && cautiousTextRisk)
        return if (requiresConfirmation) {
            val target = action.meta.target.ifBlank { targetText.take(80).ifBlank { action.toolName } }
            AgentRiskAssessment(
                AgentRiskLevel.CONFIRMATION_REQUIRED,
                "Confirm ${action.meta.operationKind.name.lowercase().replace('_', ' ')} action on $target"
            )
        } else {
            AgentRiskAssessment(AgentRiskLevel.SAFE)
        }
    }

    private fun actionTargetNode(action: AgentAction, observation: AgentObservation): UiNodeSnapshot? =
        when (action) {
            is AgentAction.TapElement -> observation.uiNodes.firstOrNull { it.elementId == action.elementId }
            is AgentAction.InputText -> action.elementId?.let { id ->
                observation.uiNodes.firstOrNull { it.elementId == id }
            }
            is AgentAction.Tap -> observation.uiNodes.minByOrNull {
                val dx = it.bounds.centerX - action.x
                val dy = it.bounds.centerY - action.y
                dx * dx + dy * dy
            }?.takeIf {
                kotlin.math.abs(it.bounds.centerX - action.x) <= 80 &&
                    kotlin.math.abs(it.bounds.centerY - action.y) <= 80
            }
            else -> null
        }

}

private val BLOCKED_OPERATION_KINDS = setOf(AgentOperationKind.PURCHASE, AgentOperationKind.ACCOUNT)

private fun UiNodeSnapshot?.targetText(): String = this?.let {
    "${it.text} ${it.contentDescription}".trim()
}.orEmpty()

private fun String.hasBlockedRiskTerm(): Boolean =
    BLOCKED_ENGLISH_RISK.containsMatchIn(lowercase()) || BLOCKED_CHINESE_RISK.any(::contains)

/** Only irreversible data and application removal controls require approval. */
private fun String.hasDestructiveRiskTerm(): Boolean {
    val english = lowercase()
    return DESTRUCTIVE_ENGLISH_RISK.containsMatchIn(english) || DESTRUCTIVE_CHINESE_RISK.any(::contains)
}

/** Legacy broad matches are intentionally opt-in because they flag harmless controls such as search clearing. */
private fun String.hasCautiousOnlyRiskTerm(): Boolean =
    CAUTIOUS_ENGLISH_RISK.containsMatchIn(lowercase()) || CAUTIOUS_CHINESE_RISK.any(::contains)

private val DESTRUCTIVE_ENGLISH_RISK = Regex("\\b(delete|clear data|erase data|uninstall|factory reset)\\b")
private val DESTRUCTIVE_CHINESE_RISK = listOf("删除", "清除数据", "清空数据", "卸载", "恢复出厂")
private val CAUTIOUS_ENGLISH_RISK = Regex("\\b(remove|clear)\\b")
private val CAUTIOUS_CHINESE_RISK = listOf("移除", "清除")
private val BLOCKED_ENGLISH_RISK = Regex("\\b(pay|purchase|buy|checkout|sign in|log in|register|create account|password|passcode)\\b")
private val BLOCKED_CHINESE_RISK = listOf("付款", "支付", "购买", "下单", "结账", "登录", "注册账号", "创建账号", "密码", "口令")
