package com.gtarena.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.Profile
import com.gtarena.model.description
import com.gtarena.model.label
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.Lime
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.Panel
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.TextSoft

private val InactiveShape = RoundedCornerShape(3.dp)
private val ActiveShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)

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
            color = TextSecondary,
            fontSize = 13.sp
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
    val shape = if (active) ActiveShape else InactiveShape
    val base = modifier
        .height(52.dp)
        .clip(shape)
    val styled = if (active) {
        base.background(Lime)
    } else {
        base
            .background(Panel)
            .border(1.dp, Hairline, shape)
    }
    Box(
        modifier = styled.selectable(
            selected = active,
            role = Role.RadioButton,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = profile.label,
            color = if (active) OnLime else TextSoft,
            fontSize = 16.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
