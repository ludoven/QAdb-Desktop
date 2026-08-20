package com.ludoven.adbtool.agent

/** Legacy protocol payload retained only while old provider parsing code is removed. */
data class AgentSkillSnapshot(
    val id: String,
    val version: Int,
    val guidance: List<String>
)

/** Legacy protocol payload retained only while old provider parsing code is removed. */
data class AgentAppKnowledgeSnapshot(
    val packageName: String,
    val guidance: List<String>
)
