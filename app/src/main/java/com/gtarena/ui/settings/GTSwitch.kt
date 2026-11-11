package com.gtarena.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.ui.theme.GaugeTick
import com.gtarena.ui.theme.GaugeTrack
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.SlantShape
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary

private val SwitchShape = SlantShape(6.dp)
private val ThumbShape = SlantShape(4.dp)

@Composable
fun GTSwitch(checked: Boolean, accent: Color, modifier: Modifier = Modifier) {
    val thumbX = animateDpAsState(if (checked) 23.dp else 5.dp, label = "thumb")
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 24.dp)
            .clip(SwitchShape)
            .background(if (checked) accent else GaugeTrack)
            .then(if (checked) Modifier else Modifier.border(1.dp, GaugeTick, SwitchShape))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbX.value)
                .size(18.dp)
                .clip(ThumbShape)
                .background(if (checked) OnLime else TextSecondary)
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(
                    if (enabled) Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                    else Modifier.semantics { disabled() }
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(if (enabled) 1f else 0.4f)
                )
                if (caption != null) {
                    Text(text = caption, color = TextSecondary, fontSize = 13.sp)
                }
            }
            GTSwitch(checked = checked, accent = accent, modifier = Modifier.alpha(if (enabled) 1f else 0.4f))
        }
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(GaugeTrack))
        }
    }
}
