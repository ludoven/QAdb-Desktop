package com.ludoven.adbtool

import com.ludoven.adbtool.entity.AppInfoData
import kotlin.test.Test
import kotlin.test.assertEquals

class AppInfoDataTest {

    @Test
    fun appInfoDataCarriesDetailPageFields() {
        val data = AppInfoData(
            appName = "微信",
            packageName = "com.tencent.mm",
            versionName = "8.0.47",
            versionCode = "2800",
            apkPath = "/data/app/com.tencent.mm-1/base.apk",
            installLocation = "内部存储",
            appSize = "280 MB",
            dataSize = "420 MB",
            cacheSize = "120 MB",
            totalSize = "820 MB",
            isRunning = true
        )

        assertEquals("微信", data.appName)
        assertEquals("/data/app/com.tencent.mm-1/base.apk", data.apkPath)
        assertEquals("内部存储", data.installLocation)
        assertEquals("820 MB", data.totalSize)
        assertEquals(true, data.isRunning)
    }
}
