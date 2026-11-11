package com.gtarena.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.PanelLo
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.color
import com.gtarena.ui.theme.rememberAppIcon
import com.gtarena.ui.theme.textOnDark

private val CardShape = ChamferShape(16.dp)
private val IconShape = ChamferShape(9.dp)
private val EditShape = ChamferShape(5.dp)
private val PlayShape = ChamferShape(10.dp, topStart = false, bottomEnd = false, bottomStart = true)
private val Scrim = Brush.verticalGradient(listOf(Color.Transparent, Ground.copy(alpha = 0.9f)))

@Composable
fun GameCard(game: GameEntry, onLaunch: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val tint = rememberAppIcon(game.pkg)?.tint
    Box(
        modifier = modifier
            .size(width = 132.dp, height = 168.dp)
            .clip(CardShape)
            .background(PanelLo)
            .drawBehind {
                if (tint != null) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(tint.copy(alpha = 0.8f), Color.Transparent),
                            center = Offset(size.width * 0.3f, size.height * 0.18f),
                            radius = size.height
                        )
                    )
                }
            }
            .border(1.dp, BevelBrush, CardShape)
            .clickable(role = Role.Button, onClickLabel = "Main ${game.label}", onClick = onLaunch)
    ) {
        Box(Modifier.fillMaxSize().background(Scrim))
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(34.dp)
                .clip(PlayShape)
                .background(game.profile.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GTIcons.Play,
                contentDescription = null,
                tint = OnLime,
                modifier = Modifier.size(14.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            AppIcon(
                pkg = game.pkg,
                modifier = Modifier
                    .size(54.dp)
                    .clip(IconShape)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = game.label,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp)
                    .height(30.dp)
                    .clip(EditShape)
                    .background(Ground.copy(alpha = 0.6f))
                    .border(1.dp, BevelBrush, EditShape)
                    .clickable(role = Role.Button, onClickLabel = "Atur ${game.label}", onClick = onEdit)
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Label(
                    text = "${game.profile.label} · ${game.refreshHz}",
                    color = game.profile.textOnDark,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = GTIcons.Settings,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
