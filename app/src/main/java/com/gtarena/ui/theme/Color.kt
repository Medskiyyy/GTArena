package com.gtarena.ui.theme

import androidx.compose.ui.graphics.Color
import com.gtarena.model.Profile

val Ground = Color(0xFF0A0B0D)
val Panel = Color(0xFF14161A)
val Hairline = Color(0xFF262A31)
val TextPrimary = Color(0xFFF2F4F7)
val TextSecondary = Color(0xFF9AA3AF)
val TextSoft = Color(0xFFB6BEC9)
val TextInactive = Color(0xFFD5DAE1)
val Lime = Color(0xFFC8F031)
val OnLime = Color(0xFF0A0B0D)
val GaugeTrack = Color(0xFF1D2026)
val GaugeRedZone = Color(0xFF7A2A1C)
val GaugeTick = Color(0xFF3A404A)
val OutlineButton = Color(0xFF3A404A)
val Warn = Color(0xFFFFB020)
val NavBar = Color(0xFF0F1114)
val ProfileHemat = Color(0xFF46C8FF)
val ProfileSeimbang = Lime
val ProfileGaming = Color(0xFFFF4B2B)
val GamingOnDark = Color(0xFFFF6A4D)

val Profile.color: Color
    get() = when (this) {
        Profile.HEMAT -> ProfileHemat
        Profile.SEIMBANG -> ProfileSeimbang
        Profile.GAMING -> ProfileGaming
    }

val Profile.textOnDark: Color
    get() = if (this == Profile.GAMING) GamingOnDark else color
