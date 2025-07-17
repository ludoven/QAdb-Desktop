package com.ludoven.adbtool.entity

import org.jetbrains.compose.resources.StringResource

sealed interface MsgContent{
    data class Resource(
        val stringResource: StringResource,
        val args: List<Any> = emptyList()
    ): MsgContent
    data class Text(val text: String): MsgContent
}
