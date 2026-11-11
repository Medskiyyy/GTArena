package com.gtarena.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gtarena.ui.AppTab
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.intro.IntroTiming
import com.gtarena.ui.intro.introFromBelow
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.NavBar
import com.gtarena.ui.theme.SlantShape
import com.gtarena.ui.theme.TextSecondary

private val IndicatorShape = SlantShape(3.dp)

@Composable
fun BottomNav(
    selected: AppTab,
    accent: Color,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .introFromBelow(IntroTiming.NAV_START, IntroTiming.TOTAL)
            .fillMaxWidth()
            .background(NavBar)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Row(modifier = Modifier.fillMaxWidth().height(60.dp)) {
            NavItem("Arena", GTIcons.Arena, selected == AppTab.ARENA, accent, { onSelect(AppTab.ARENA) }, Modifier.weight(1f))
            NavItem("Game", GTIcons.Games, selected == AppTab.GAME, accent, { onSelect(AppTab.GAME) }, Modifier.weight(1f))
            NavItem("Overlay", GTIcons.Overlay, selected == AppTab.OVERLAY, accent, { onSelect(AppTab.OVERLAY) }, Modifier.weight(1f))
            NavItem("Setelan", GTIcons.Settings, selected == AppTab.SETELAN, accent, { onSelect(AppTab.SETELAN) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (active) accent else TextSecondary
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 34.dp, height = 3.dp)
                    .clip(IndicatorShape)
                    .background(accent)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Label(text = label, color = color)
        }
    }
}
