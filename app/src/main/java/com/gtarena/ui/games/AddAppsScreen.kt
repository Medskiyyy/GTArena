package com.gtarena.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtarena.data.AppItem
import com.gtarena.data.SettingsStore
import com.gtarena.ui.ScreenTitle
import com.gtarena.ui.settings.GTSwitch
import com.gtarena.ui.tabInsets
import com.gtarena.ui.theme.GaugeTrack
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary

@Composable
fun AddAppsScreen(
    accent: Color,
    loadOtherApps: suspend () -> List<AppItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val extras by store.extraGames.collectAsStateWithLifecycle()
    val apps by produceState<List<AppItem>?>(initialValue = null) { value = loadOtherApps() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .tabInsets(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        item { BackLink(onBack) }
        item { ScreenTitle("TAMBAH APP", modifier = Modifier.padding(bottom = 14.dp)) }
        val list = apps
        if (list == null) {
            item { Text("Lagi nyari app...", color = TextSecondary, fontSize = 14.sp) }
        } else {
            items(list, key = { it.pkg }) { app ->
                val on = app.pkg in extras
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(value = on, role = Role.Switch, onValueChange = { store.setExtraGame(app.pkg, it) })
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.label,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    GTSwitch(checked = on, accent = accent)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(GaugeTrack))
            }
        }
    }
}
