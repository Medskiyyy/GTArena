package com.gtarena.ui.theme

import androidx.compose.ui.graphics.Color
import com.gtarena.model.Profile

val Ground = Color(0xFF060607)
val Panel = Color(0xFF121315)
val PanelHi = Color(0xFF1C1D21)
val PanelLo = Color(0xFF0C0D0F)
val Hairline = Color(0xFF2A2B30)
val TextPrimary = Color(0xFFF5F5F6)
val TextSecondary = Color(0xFF8E9096)
val TextSoft = Color(0xFFBDBFC4)
val TextInactive = Color(0xFFD8D9DC)
val Lime = Color(0xFFC8F031)
val OnLime = Color(0xFF060607)
val GaugeTrack = Color(0xFF222327)
val GaugeRedZone = Color(0xFF7A2A1C)
val GaugeTick = Color(0xFF3C3D43)
val OutlineButton = Color(0xFF3C3D43)
val Warn = Color(0xFFFFB020)
val NavBar = Color(0xFF0A0A0B)
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
