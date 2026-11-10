package com.gtarena.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.Profile
import com.gtarena.model.description
import com.gtarena.model.label
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.Panel
import com.gtarena.ui.theme.TextInactive
import com.gtarena.ui.theme.TextSoft
import com.gtarena.ui.theme.color

internal object CutBottomEndShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cut = with(density) { 12.dp.toPx() }
        return Outline.Generic(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - cut)
                lineTo(size.width - cut, size.height)
                lineTo(0f, size.height)
                close()
            }
        )
    }
}

@Composable
fun ProfileSelector(
    selected: Profile,
    onSelected: (Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Profile.values().forEach { profile ->
                ProfileButton(
                    profile = profile,
                    active = profile == selected,
                    onClick = { onSelected(profile) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            text = selected.description,
            color = TextSoft,
            fontSize = 13.sp,
            modifier = Modifier.heightIn(min = 38.dp)
        )
    }
}

@Composable
private fun ProfileButton(
    profile: Profile,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(54.dp)
            .clip(if (active) CutBottomEndShape else RectangleShape)
            .background(if (active) profile.color else Panel)
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(profile.color)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile.label,
                color = if (active) OnLime else TextInactive,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
