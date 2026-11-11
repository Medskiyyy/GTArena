package com.gtarena.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.data.AppItem
import com.gtarena.model.GameEntry
import com.gtarena.ui.ScreenTitle
import com.gtarena.ui.home.components.GameRow
import com.gtarena.ui.tabInsets
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary

@Composable
fun GameTab(
    games: List<GameEntry>,
    accent: Color,
    editingPkg: String?,
    onEdit: (String?) -> Unit,
    adding: Boolean,
    onAdding: (Boolean) -> Unit,
    onLaunch: (GameEntry) -> Unit,
    loadOtherApps: suspend () -> List<AppItem>,
    modifier: Modifier = Modifier
) {
    val editing = games.firstOrNull { it.pkg == editingPkg }
    when {
        adding -> AddAppsScreen(accent = accent, loadOtherApps = loadOtherApps, onBack = { onAdding(false) }, modifier = modifier)
        editing != null -> GameEditor(game = editing, onBack = { onEdit(null) }, modifier = modifier)
        else -> GameList(
            games = games,
            accent = accent,
            onEdit = { onEdit(it.pkg) },
            onLaunch = onLaunch,
            onAdd = { onAdding(true) },
            modifier = modifier
        )
    }
}

@Composable
private fun GameList(
    games: List<GameEntry>,
    accent: Color,
    onEdit: (GameEntry) -> Unit,
    onLaunch: (GameEntry) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .tabInsets(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        item { ScreenTitle("GAME", modifier = Modifier.padding(bottom = 14.dp)) }
        if (games.isEmpty()) {
            item {
                Text(
                    text = "Belum ada game kedeteksi. Tambah manual di bawah.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }
        itemsIndexed(games, key = { _, game -> game.pkg }) { index, game ->
            GameRow(
                game = game,
                accent = accent,
                primary = false,
                showDivider = true,
                onLaunch = { onLaunch(game) },
                onClick = { onEdit(game) }
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onAdd)
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Tambah app lain", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "Masukin app biasa biar dihitung sebagai game", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
