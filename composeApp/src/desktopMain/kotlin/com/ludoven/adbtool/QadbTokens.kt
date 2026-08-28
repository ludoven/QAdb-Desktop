package com.ludoven.adbtool

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ludoven.adbtool.ui.mac.MaterialTheme

/**
 * QADB Workbench (Linear-Class) design tokens — dual-theme (Light / Dark).
 *
 * Each property maps 1:1 to `canvas/design-tokens.md` §2.2 Color Palette and is annotated with
 * its token name. Theme is resolved from [MaterialTheme.colorScheme] so Light/Dark switch
 * automatically, mirroring [QadbColors]. Shared semantic colors reuse [QadbColors] to avoid
 * palette drift; tokens missing from QadbColors carry dedicated dual-theme values from the spec.
 *
 * Spacing / radius / font-size are intentionally NOT duplicated here — reuse [UiTokens]
 * (mapping per design-tokens.md §2.4: space-1→SpaceXSmall, space-2→SpaceSmall, space-3→SpaceMedium,
 * space-4→SpaceLarge, space-6→SpaceXXLarge, space-8→SpaceXXXLarge; radius-sm/md/lg/xl →
 * RadiusSmall/Medium/Large/XLarge).
 */
object QadbTokens {

    @Composable
    private fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // ============ 2.2.2 Neutral backgrounds (bg-0 … bg-3) ============

    /** bg-0 window base — Light #F6F7F9 / Dark #0F0F12 */
    val bg0: Color
        @Composable get() = if (isDark()) Color(0xFF0F0F12) else Color(0xFFF6F7F9)

    /** bg-1 main surface (chat, cards, input) — Light #FFFFFF / Dark #141418 */
    val bg1: Color
        @Composable get() = if (isDark()) Color(0xFF141418) else Color(0xFFFFFFFF)

    /** bg-2 secondary surface (top bar, right panel) — Light #FBFCFD / Dark #1A1A1F */
    val bg2: Color
        @Composable get() = if (isDark()) Color(0xFF1A1A1F) else Color(0xFFFBFCFD)

    /** bg-3 inset surface (code blocks, meter track) — Light #EFF1F4 / Dark #212127 */
    val bg3: Color
        @Composable get() = if (isDark()) Color(0xFF212127) else Color(0xFFEFF1F4)

    // ============ 2.2.3 Text hierarchy ============

    /** text-primary — Light #16181D / Dark #ECEDEF */
    val textPrimary: Color
        @Composable get() = if (isDark()) Color(0xFFECEDEF) else Color(0xFF16181D)

    /** text-secondary — Light #4B505A / Dark #A9ADB5 */
    val textSecondary: Color
        @Composable get() = if (isDark()) Color(0xFFA9ADB5) else Color(0xFF4B505A)

    /** text-tertiary — Light #717684 / Dark #80858F */
    val textTertiary: Color
        @Composable get() = if (isDark()) Color(0xFF80858F) else Color(0xFF717684)

    /** text-muted (placeholders / disabled) — Light #9AA0AC / Dark #565B64 */
    val textMuted: Color
        @Composable get() = if (isDark()) Color(0xFF565B64) else Color(0xFF9AA0AC)

    // ============ 2.2.4 Borders / dividers ============

    /** border — Light #E4E6EA / Dark #26262C */
    val border: Color
        @Composable get() = if (isDark()) Color(0xFF26262C) else Color(0xFFE4E6EA)

    /** border-strong — Light #D3D6DC / Dark #35353D */
    val borderStrong: Color
        @Composable get() = if (isDark()) Color(0xFF35353D) else Color(0xFFD3D6DC)

    /** divider — Light #EDEFF2 / Dark #1E1E23 */
    val divider: Color
        @Composable get() = if (isDark()) Color(0xFF1E1E23) else Color(0xFFEDEFF2)

    // ============ 2.2.1 Brand blue ramp ============

    /** brand-500 #2196F3 — focus rings, active icons, busy dots, progress fill */
    val brand: Color
        @Composable get() = Color(0xFF2196F3)

    /** brand-action — Light brand-700 #1976D2 / Dark brand-400 #42A5F5 (primary button bg) */
    val brandAction: Color
        @Composable get() = if (isDark()) Color(0xFF42A5F5) else Color(0xFF1976D2)

    /** brand-hover — Light brand-800 #1565C0 / Dark brand-300 #64B5F6 */
    val brandHover: Color
        @Composable get() = if (isDark()) Color(0xFF64B5F6) else Color(0xFF1565C0)

    /** brand-soft — Light #E3F2FD / Dark rgba(33,150,243,.16) — user bubble background */
    val brandSoft: Color
        @Composable get() = if (isDark()) Color(0x292196F3) else Color(0xFFE3F2FD)

    /** brand-900 — Light #0D47A1 / Dark #90CAF9 — text inside a brand container */
    val brand900: Color
        @Composable get() = if (isDark()) Color(0xFF90CAF9) else Color(0xFF0D47A1)

    // ============ 2.2.5 Status colors ============

    /** success — Light #1E8E3E / Dark #3FB950 */
    val success: Color
        @Composable get() = if (isDark()) Color(0xFF3FB950) else Color(0xFF1E8E3E)

    /** warning — Light #B45309 / Dark #F0A020 */
    val warning: Color
        @Composable get() = if (isDark()) Color(0xFFF0A020) else Color(0xFFB45309)

    /** danger — Light #D93025 / Dark #F2555A */
    val danger: Color
        @Composable get() = if (isDark()) Color(0xFFF2555A) else Color(0xFFD93025)

    /** info — Light #1976D2 / Dark #42A5F5 */
    val info: Color
        @Composable get() = if (isDark()) Color(0xFF42A5F5) else Color(0xFF1976D2)

    // ---- Status containers (2.2.5) ----

    /** success-container — Light #E7F4EC / Dark #12301F */
    val successContainer: Color
        @Composable get() = if (isDark()) Color(0xFF12301F) else Color(0xFFE7F4EC)

    /** success-container text — Light #135C28 / Dark #7EE2A0 */
    val successText: Color
        @Composable get() = if (isDark()) Color(0xFF7EE2A0) else Color(0xFF135C28)

    /** warning-container — Light #FDF0D9 / Dark #3A2B0C */
    val warningContainer: Color
        @Composable get() = if (isDark()) Color(0xFF3A2B0C) else Color(0xFFFDF0D9)

    /** warning-container text — Light #7C4A03 / Dark #FFD28A */
    val warningText: Color
        @Composable get() = if (isDark()) Color(0xFFFFD28A) else Color(0xFF7C4A03)

    /** danger-container — Light #FCEBEA / Dark #3A1A1E */
    val dangerContainer: Color
        @Composable get() = if (isDark()) Color(0xFF3A1A1E) else Color(0xFFFCEBEA)

    /** danger-container text — Light #A11C12 / Dark #FF9C96 */
    val dangerText: Color
        @Composable get() = if (isDark()) Color(0xFFFF9C96) else Color(0xFFA11C12)

    /** info-container — Light #E8F1FC / Dark #0E2A47 */
    val infoContainer: Color
        @Composable get() = if (isDark()) Color(0xFF0E2A47) else Color(0xFFE8F1FC)

    /** info-container text — Light #0B4FA0 / Dark #9ECBEF */
    val infoText: Color
        @Composable get() = if (isDark()) Color(0xFF9ECBEF) else Color(0xFF0B4FA0)

    // ============ 2.2.6 AI accent (agent telemetry only) ============

    /** ai — Light #7C3AED / Dark #A78BFA — thinking/tool/token/observation icons */
    val ai: Color
        @Composable get() = if (isDark()) Color(0xFFA78BFA) else Color(0xFF7C3AED)

    /** ai-container — Light #EFEAFE / Dark #241A3D — thinking/tool card background */
    val aiContainer: Color
        @Composable get() = if (isDark()) Color(0xFF241A3D) else Color(0xFFEFEAFE)

    /** ai-container text — Light #4C1D95 / Dark #C4B5FD */
    val aiText: Color
        @Composable get() = if (isDark()) Color(0xFFC4B5FD) else Color(0xFF4C1D95)

    /** ai-border — Light #DCD1F8 / Dark #3A2F5E */
    val aiBorder: Color
        @Composable get() = if (isDark()) Color(0xFF3A2F5E) else Color(0xFFDCD1F8)

    // ============ 2.2.7 Quick-reference comments ============
    // Light: bg-0 #F6F7F9 · bg-1 #FFFFFF · bg-2 #FBFCFD · bg-3 #EFF1F4
    //        text #16181D / #4B505A / #717684 / #9AA0AC
    //        border #E4E6EA · divider #EDEFF2 · brand #2196F3 · brand-action #1976D2 · ai #7C3AED
    // Dark : bg-0 #0F0F12 · bg-1 #141418 · bg-2 #1A1A1F · bg-3 #212127
    //        text #ECEDEF / #A9ADB5 / #80858F / #565B64
    //        border #26262C · divider #1E1E23 · brand #2196F3 · brand-action #42A5F5 · ai #A78BFA
}
