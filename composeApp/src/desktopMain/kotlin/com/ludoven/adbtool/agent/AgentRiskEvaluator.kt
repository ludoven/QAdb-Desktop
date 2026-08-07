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
        if (!localCapabilityReason.isNullOrBlank()) {
            return AgentRiskAssessment(AgentRiskLevel.CONFIRMATION_REQUIRED, localCapabilityReason)
        }
        if (action.requiresConfirmation) {
            return AgentRiskAssessment(
                AgentRiskLevel.CONFIRMATION_REQUIRED,
                "This device-management action can change device or application data"
            )
        }

        val modelRisk = action.meta.operationKind in CONFIRMATION_OPERATION_KINDS
        val targetText = actionTargetText(action, observation)
        val explicitTargetRisk = targetText.hasExplicitRiskTerm()
        val cautiousTextRisk = targetText.hasCautiousOnlyRiskTerm()
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

    private fun actionTargetText(action: AgentAction, observation: AgentObservation): String {
        val node = when (action) {
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
        return buildString {
            append(action.meta.intent).append(' ')
            append(action.meta.target).append(' ')
            node?.let {
                append(it.text).append(' ')
                append(it.contentDescription)
            }
        }.trim()
    }
}

private val CONFIRMATION_OPERATION_KINDS = setOf(
    AgentOperationKind.SEND,
    AgentOperationKind.PURCHASE,
    AgentOperationKind.PERMISSION,
    AgentOperationKind.DELETE,
    AgentOperationKind.ACCOUNT,
    AgentOperationKind.SYSTEM_CHANGE
)

/** Clear, user-visible commit or destructive controls. These remain guarded in every policy. */
private fun String.hasExplicitRiskTerm(): Boolean {
    val english = lowercase()
    return EXPLICIT_ENGLISH_RISK.containsMatchIn(english) || EXPLICIT_CHINESE_RISK.any(::contains)
}

/** Legacy broad matches are intentionally opt-in because they flag harmless controls such as search clearing. */
private fun String.hasCautiousOnlyRiskTerm(): Boolean =
    CAUTIOUS_ENGLISH_RISK.containsMatchIn(lowercase()) || CAUTIOUS_CHINESE_RISK.any(::contains)

private val EXPLICIT_ENGLISH_RISK = Regex("\\b(send|submit|publish|post|pay|purchase|buy|allow|permission|delete|clear data|sign in|log in|uninstall|reboot|factory reset)\\b")
private val EXPLICIT_CHINESE_RISK = listOf(
    "发送", "提交", "发布", "付款", "支付", "购买", "下单", "授权", "允许", "始终允许",
    "删除", "清除数据", "清空数据", "登录", "注册", "退出账号", "卸载", "重启", "恢复出厂"
)
private val CAUTIOUS_ENGLISH_RISK = Regex("\\b(remove|clear)\\b")
private val CAUTIOUS_CHINESE_RISK = listOf("移除", "清除")
