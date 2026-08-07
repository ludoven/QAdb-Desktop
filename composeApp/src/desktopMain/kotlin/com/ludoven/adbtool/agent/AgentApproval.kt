package com.ludoven.adbtool.agent

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Controls how aggressively the Agent asks before a non-hard-blocked action. */
enum class AgentApprovalPolicy {
    /** Prompts only for an explicit high-risk intent, destructive device action, or local capability install. */
    SMART,

    /** Keeps the previous broad text-based risk heuristic for users who prefer extra review. */
    CAUTIOUS
}

class AgentApprovalPreferences(
    private val preferences: Preferences = Preferences.userNodeForPackage(AgentApprovalPreferences::class.java)
) {
    private val _policy = MutableStateFlow(readPolicy())
    val policy: StateFlow<AgentApprovalPolicy> = _policy.asStateFlow()

    fun setPolicy(policy: AgentApprovalPolicy) {
        preferences.put(KEY_POLICY, policy.name)
        preferences.flush()
        _policy.value = policy
    }

    private fun readPolicy(): AgentApprovalPolicy = preferences.get(KEY_POLICY, AgentApprovalPolicy.SMART.name)
        .let { saved -> AgentApprovalPolicy.entries.firstOrNull { it.name == saved } ?: AgentApprovalPolicy.SMART }

    private companion object {
        const val KEY_POLICY = "agent.approval.policy"
    }
}

object AgentApprovalRuntime {
    val preferences: AgentApprovalPreferences by lazy { AgentApprovalPreferences() }
}
