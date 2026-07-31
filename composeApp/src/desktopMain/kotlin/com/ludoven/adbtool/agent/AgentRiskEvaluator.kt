package com.ludoven.adbtool.agent

data class AgentRiskAssessment(
    val level: AgentRiskLevel,
    val reason: String = ""
)

class AgentRiskEvaluator {
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
        val localRisk = RISK_TERMS.any { targetText.contains(it, ignoreCase = true) }
        return if (modelRisk || localRisk) {
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

private val RISK_TERMS = listOf(
    "send", "发送", "提交", "publish", "发布", "post",
    "pay", "purchase", "buy", "付款", "支付", "购买", "下单",
    "allow", "permission", "授权", "允许", "始终允许",
    "delete", "remove", "clear", "删除", "移除", "清除",
    "sign in", "log in", "登录", "注册", "退出账号",
    "uninstall", "reboot", "factory reset", "卸载", "重启", "恢复出厂"
)
