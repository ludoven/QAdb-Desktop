package com.ludoven.adbtool.agent

internal object AgentReadOnlyAnswers {
    fun deviceStatusEvidence(
        snapshot: DeviceStatusSnapshot,
        requestedFields: Set<DeviceStatusField>
    ): AgentTrustedEvidence {
        val fields = requestedFields.ifEmpty { DeviceStatusField.entries.toSet() }
        val facts = linkedMapOf(
            "observed_at_ms" to snapshot.observedAtMs.toString(),
            "requested_fields" to fields.joinToString(",") { it.name.lowercase() }
        )
        val unavailable = snapshot.failures.mapTo(linkedSetOf()) { it.probe.name.lowercase() }

        if (DeviceStatusField.IDENTITY in fields) {
            snapshot.identity?.let { identity ->
                identity.manufacturer?.let { facts["device.manufacturer"] = it }
                identity.model?.let { facts["device.model"] = it }
            }
        }
        if (DeviceStatusField.ANDROID in fields) {
            snapshot.identity?.let { identity ->
                identity.androidVersion?.let { facts["android.version"] = it }
                identity.sdkVersion?.let { facts["android.api_level"] = it.toString() }
            }
        }
        if (DeviceStatusField.DISPLAY in fields) {
            snapshot.display?.let { display ->
                display.widthPx?.let { facts["display.width_px"] = it.toString() }
                display.heightPx?.let { facts["display.height_px"] = it.toString() }
                display.densityDpi?.let { facts["display.density_dpi"] = it.toString() }
                display.fontScale?.let { facts["display.font_scale"] = it.toString() }
            }
        }
        if (DeviceStatusField.BATTERY in fields) {
            snapshot.battery?.let { battery ->
                battery.levelPercent?.let { facts["battery.level_percent"] = it.toString() }
                facts["battery.state"] = battery.state.name.lowercase()
                battery.temperatureCelsius?.let { facts["battery.temperature_celsius"] = it.toString() }
            }
        }
        if (DeviceStatusField.CPU in fields) {
            snapshot.cpu?.let { facts["cpu.usage_percent"] = it.usagePercent.toString() }
        }
        if (DeviceStatusField.MEMORY in fields) {
            snapshot.memory?.let { memory ->
                facts["memory.total_bytes"] = memory.totalBytes.toString()
                facts["memory.available_bytes"] = memory.availableBytes.toString()
                facts["memory.used_percent"] = memory.usedPercent.toString()
            }
        }
        if (DeviceStatusField.STORAGE in fields) {
            snapshot.storage?.let { storage ->
                facts["storage.total_bytes"] = storage.totalBytes.toString()
                facts["storage.used_bytes"] = storage.usedBytes.toString()
                facts["storage.available_bytes"] = storage.availableBytes.toString()
                facts["storage.used_percent"] = storage.usedPercent.toString()
            }
        }
        if (DeviceStatusField.NETWORK in fields) {
            facts["network.adb_transport"] = snapshot.network.connectionType.name.lowercase()
            snapshot.network.wifiEnabled?.let { facts["network.wifi_enabled"] = it.toString() }
                ?: unavailable.add("network.wifi_enabled")
            snapshot.network.wifiConnected?.let { facts["network.wifi_connected"] = it.toString() }
                ?: unavailable.add("network.wifi_connected")
        }
        if (DeviceStatusField.FOREGROUND_APP in fields) {
            snapshot.foreground?.packageName?.let { facts["foreground.package_name"] = it }
        }
        if (DeviceStatusField.ADB_LATENCY in fields) {
            snapshot.adbLatencyMs?.let { facts["adb.latency_ms"] = it.toString() }
        }
        return AgentTrustedEvidence(
            source = AgentTrustedEvidenceSource.DEVICE_STATUS,
            facts = facts,
            unavailableFields = unavailable,
            complete = unavailable.isEmpty()
        )
    }

    fun installedAppsEvidence(appQuery: String?, apps: List<InstalledAgentApp>): AgentTrustedEvidence {
        return installedAppsEvidence(listOfNotNull(appQuery), apps)
    }

    fun installedAppsEvidence(appQueries: List<String>, apps: List<InstalledAgentApp>): AgentTrustedEvidence {
        val queries = appQueries.asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase() }
            .take(MAX_APP_QUERIES)
            .toList()
        val eligibleApps = apps.filter { it.enabled && it.launchable }
        val matchesByQuery = queries.associateWith { query -> rankInstalledApps(eligibleApps, query) }
        val filtered = if (queries.isEmpty()) eligibleApps else matchesByQuery.values
            .flatten()
            .distinctBy(InstalledAgentApp::packageName)
        val visible = filtered.take(MAX_VISIBLE_APPS)
        val facts = linkedMapOf(
            "catalog_count" to eligibleApps.size.toString(),
            "query_count" to queries.size.toString(),
            "match_count" to filtered.size.toString(),
            "returned_count" to visible.size.toString(),
            "truncated" to (filtered.size > visible.size).toString(),
            "absence_conclusive" to queries.isNotEmpty().toString(),
            "catalog_scope" to if (queries.isEmpty()) "bounded_sample" else "full_catalog_query"
        )
        matchesByQuery.entries.forEachIndexed { index, (query, matches) ->
            facts["queries.$index.value"] = query
            facts["queries.$index.found"] = matches.isNotEmpty().toString()
            facts["queries.$index.match_count"] = matches.size.toString()
        }
        visible.forEachIndexed { index, app ->
            facts["apps.$index.label"] = app.label.ifBlank { app.packageName }
            facts["apps.$index.package_name"] = app.packageName
            facts["apps.$index.enabled"] = app.enabled.toString()
            facts["apps.$index.launchable"] = app.launchable.toString()
        }
        return AgentTrustedEvidence(
            source = AgentTrustedEvidenceSource.APP_CATALOG,
            facts = facts,
            unavailableFields = if (queries.isEmpty() && filtered.size > visible.size) {
                setOf("complete_app_listing")
            } else {
                emptySet()
            },
            complete = queries.isNotEmpty() || filtered.size <= visible.size
        )
    }

    fun systemProbeEvidence(probeId: String, rawValue: String): AgentTrustedEvidence {
        val rawEnabled = rawValue.trim().equals("true", ignoreCase = true)
        val enabled = if (probeId == "rotation_locked") !rawEnabled else rawEnabled
        return AgentTrustedEvidence(
            source = AgentTrustedEvidenceSource.SYSTEM_PROBE,
            facts = linkedMapOf(
                "probe_id" to probeId,
                "enabled" to enabled.toString()
            )
        )
    }

    private const val MAX_VISIBLE_APPS = 30
    private const val MAX_APP_QUERIES = 8
}
