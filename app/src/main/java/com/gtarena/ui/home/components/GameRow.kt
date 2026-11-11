package com.gtarena.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.GameEntry
import com.gtarena.model.label
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.theme.AppIcon
import com.gtarena.ui.theme.BevelBrush
import com.gtarena.ui.theme.ChamferShape
import com.gtarena.ui.theme.GaugeTrack
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.textOnDark

private val TileShape = ChamferShape(9.dp)

fun gameSubtitle(game: GameEntry): String = buildList {
    add(game.profile.label)
    add("${game.refreshHz} Hz")
    if (game.focus) add("Fokus")
    if (game.bypassCharge) add("Bypass cas")
}.joinToString(" · ")

@Composable
fun GameRow(
    game: GameEntry,
    accent: Color,
    primary: Boolean,
    showDivider: Boolean,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIcon(
                pkg = game.pkg,
                modifier = Modifier
                    .size(48.dp)
                    .clip(TileShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.label,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Label(
                    text = gameSubtitle(game),
                    color = game.profile.textOnDark,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(TileShape)
                    .then(
                        if (primary) Modifier.background(accent)
                        else Modifier.border(1.dp, BevelBrush, TileShape)
                    )
                    .clickable(role = Role.Button, onClick = onLaunch),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GTIcons.Play,
                    contentDescription = "Main ${game.label}",
                    tint = if (primary) OnLime else TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GaugeTrack)
            )
        }
    }
}
