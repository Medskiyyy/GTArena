package com.gtarena.ui.overlay

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.data.AppItem
import com.gtarena.data.GameRepository
import com.gtarena.ui.theme.AppIcon
import com.gtarena.ui.theme.ChamferShape
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.TextSoft

private val DoneShape = ChamferShape(6.dp)

@Composable
internal fun AppPicker(
    selected: List<String>,
    accent: Color,
    onToggle: (String, Boolean) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val apps by produceState<List<AppItem>?>(null) { value = GameRepository(context).allApps() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Label("App melayang", color = TextPrimary, fontSize = 12.sp)
                Text("${selected.size} dipilih", color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
            }
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 36.dp)
                    .clip(DoneShape)
                    .background(accent)
                    .clickable(role = Role.Button, onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Label("OK", color = OnLime, fontSize = 12.sp)
            }
        }
        val list = apps
        if (list == null) {
            Text("Lagi nyari app...", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.height(PICKER_HEIGHT_DP.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PICKER_HEIGHT_DP.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(list, key = { it.pkg }) { app ->
                    val on = app.pkg in selected
                    Column(
                        modifier = Modifier.toggleable(value = on, role = Role.Checkbox) { onToggle(app.pkg, it) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppIcon(
                            pkg = app.pkg,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(AppShape)
                                .then(if (on) Modifier.border(2.dp, accent, AppShape) else Modifier)
                                .alpha(if (on) 1f else 0.45f)
                        )
                        Text(
                            text = app.label,
                            color = if (on) accent else TextSoft,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(56.dp)
                        )
                    }
                }
            }
        }
    }
}

private const val PICKER_HEIGHT_DP = 200
