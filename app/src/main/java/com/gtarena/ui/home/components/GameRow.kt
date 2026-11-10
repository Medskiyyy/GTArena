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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.GameEntry
import com.gtarena.model.label
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.theme.GaugeTrack
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.OutlineButton
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.textOnDark

fun gameInitials(label: String): String {
    val words = label.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(2)
        else -> "${words[0].first()}${words[1].first()}"
    }.uppercase()
}

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
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(GaugeTrack, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gameInitials(game.label),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.label,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = game.profile.textOnDark, fontWeight = FontWeight.Bold)) {
                            append(game.profile.label)
                        }
                        append(gameSubtitle(game).removePrefix(game.profile.label))
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val buttonModifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (primary) Modifier.background(accent)
                    else Modifier.border(1.dp, OutlineButton, CircleShape)
                )
                .clickable(role = Role.Button, onClick = onLaunch)
            Box(modifier = buttonModifier, contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = GTIcons.Play,
                    contentDescription = "Main ${game.label}",
                    tint = if (primary) OnLime else TextPrimary,
                    modifier = Modifier.size(18.dp)
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
