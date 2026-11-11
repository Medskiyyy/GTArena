package com.gtarena.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.Profile
import com.gtarena.model.description
import com.gtarena.model.label
import com.gtarena.ui.intro.IntroTiming
import com.gtarena.ui.intro.introEnter
import com.gtarena.ui.theme.SlantTab
import com.gtarena.ui.theme.TextSoft
import com.gtarena.ui.theme.color

@Composable
fun ProfileSelector(
    selected: Profile,
    onSelected: (Profile) -> Unit,
    modifier: Modifier = Modifier,
    showDescription: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Profile.values().forEachIndexed { index, profile ->
                SlantTab(
                    text = profile.label,
                    active = profile == selected,
                    color = profile.color,
                    onClick = { onSelected(profile) },
                    modifier = Modifier
                        .weight(1f)
                        .introEnter(
                            IntroTiming.PROFILE_START + IntroTiming.PROFILE_STEP * index,
                            IntroTiming.PROFILE_DUR,
                            dy = 16.dp
                        )
                )
            }
        }
        if (showDescription) {
            Text(
                text = selected.description,
                color = TextSoft,
                fontSize = 13.sp,
                modifier = Modifier.heightIn(min = 38.dp)
            )
        }
    }
}
