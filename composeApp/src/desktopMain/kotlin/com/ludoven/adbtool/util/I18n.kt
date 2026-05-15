package com.ludoven.adbtool.util

import java.util.Locale

fun l10n(zh: String, en: String): String {
    return if (Locale.getDefault().language.startsWith("en")) en else zh
}

