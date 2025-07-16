package com.ludoven.adbtool.entity

import org.jetbrains.compose.resources.StringResource

data class LocalizedText(
    val stringResource: StringResource,
    val args: List<Any> = emptyList()
)