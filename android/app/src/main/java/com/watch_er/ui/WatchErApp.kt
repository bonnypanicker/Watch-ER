package com.watch_er.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watch_er.data.Watcher

private enum class Screen {
    Home,
    NewWatcher,
    Detail,
    Settings
}

@Composable
fun WatchErApp(viewModel: WatchErViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    AppContent(
        state = state,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(state: WatchErUiState, viewModel: WatchErViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Watch-ER", fontWeight = FontWeight.SemiBold)
                        Text("Quiet web watching", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    TextButton(onClick = { screen = Screen.Settings }) {
                        Text("Settings")
                    }
                }
            )
        },
        bottomBar = {
            BottomComposer(
                onNewWatcher = { screen = Screen.NewWatcher },
                onHome = { screen = Screen.Home }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    state = state,
                    onWatcherClick = {
                        viewModel.selectWatcher(it)
                        screen = Screen.Detail
                    },
                    onCreate = { screen = Screen.NewWatcher }
                )
                Screen.NewWatcher -> NewWatcherScreen(
                    state = state,
                    onIntentChange = viewModel::updateIntent,
                    onModeChange = viewModel::updateMode,
                    onThresholdChange = viewModel::updateThreshold,
                    onGenerateStrategy = viewModel::generateStrategy,
                    onCreate = { viewModel.createWatcher { screen = Screen.Detail } }
                )
                Screen.Detail -> WatcherDetailScreen(
                    watcher = state.selectedWatcher,
                    onBack = { screen = Screen.Home }
                )
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: WatchErUiState,
    onWatcherClick: (Watcher) -> Unit,
    onCreate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroPanel(onCreate)
        }
        if (state.error != null) {
            item { InlineNotice(state.error) }
        }
        item {
            SectionTitle("Active Watchers")
        }
        val active = state.watchers.filter { it.status != "completed" }
        if (active.isEmpty()) {
            item { EmptyState() }
        } else {
            items(active, key = { it.id }) { watcher ->
                WatcherCard(watcher = watcher, onClick = { onWatcherClick(watcher) })
            }
        }
        item {
            SectionTitle("Completed")
        }
        items(state.watchers.filter { it.status == "completed" }, key = { it.id }) { watcher ->
            WatcherCard(watcher = watcher, onClick = { onWatcherClick(watcher) })
        }
    }
}

@Composable
private fun HeroPanel(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Tell Watch-ER what to wait for.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "It checks public sources in the cloud and stays quiet until the condition is likely true.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onCreate,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("New Watcher")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NewWatcherScreen(
    state: WatchErUiState,
    onIntentChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onGenerateStrategy: () -> Unit,
    onCreate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("New Watcher", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            OutlinedTextField(
                value = state.draftIntent,
                onValueChange = onIntentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(8.dp),
                label = { Text("What should I watch for?") },
                placeholder = { Text("Tell me when a specific model appears on HuggingFace") }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = state.draftMode == "oneshot",
                    onClick = { onModeChange("oneshot") },
                    label = { Text("One-shot") }
                )
                FilterChip(
                    selected = state.draftMode == "persistent",
                    onClick = { onModeChange("persistent") },
                    label = { Text("Persistent") }
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confidence ${state.draftThreshold}%", fontWeight = FontWeight.Medium)
                Slider(
                    value = state.draftThreshold.toFloat(),
                    onValueChange = { onThresholdChange(it.toInt()) },
                    valueRange = 50f..100f
                )
            }
        }
        item {
            StrategyPreviewShell(state)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onGenerateStrategy,
                    enabled = state.draftIntent.length >= 8 && !state.isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (state.isLoading && state.draftStrategy == null) "Thinking..." else "Generate")
                }
                Button(
                    onClick = onCreate,
                    enabled = state.draftStrategy != null && !state.isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (state.isLoading && state.draftStrategy != null) "Starting..." else "Start")
                }
            }
        }
    }
}

@Composable
private fun StrategyPreviewShell(state: WatchErUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Strategy Preview", fontWeight = FontWeight.SemiBold)
        val strategy = state.draftStrategy
        Text(
            "Sources: ${strategy?.sources?.joinToString { it.type } ?: "public URL/API/search"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Mode: ${state.draftMode}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Cadence: ${strategy?.frequency ?: "Gemini decides from the intent"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (strategy != null) {
            Text(strategy.title, fontWeight = FontWeight.Medium)
            strategy.verificationRules.take(3).forEach {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WatcherDetailScreen(watcher: Watcher?, onBack: () -> Unit) {
    if (watcher == null) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("No watcher selected")
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text(watcher.strategy.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(watcher.intent, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            TimelineBlock(watcher)
        }
        item {
            SectionTitle("Verification Rules")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                watcher.strategy.verificationRules.forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            SectionTitle("Sources")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                watcher.strategy.sources.forEach { source ->
                    Text("${source.type.uppercase()} ${source.url ?: source.query ?: source.id}")
                }
            }
        }
    }
}

@Composable
private fun TimelineBlock(watcher: Watcher) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusRow("Status", watcher.status)
        StatusRow("Frequency", watcher.frequency)
        StatusRow("Next check", watcher.nextCheckAt)
        StatusRow("Confidence needed", "${watcher.confidenceThreshold}%")
        watcher.lastResult?.let {
            StatusRow("Last confidence", "${it.confidence}%")
            Text(it.reasoning, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WatcherCard(watcher: Watcher, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(watcher.status)
            Spacer(Modifier.width(10.dp))
            Text(
                watcher.strategy.title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("${watcher.confidenceThreshold}%")
        }
        Text(watcher.intent, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("Next check ${watcher.nextCheckAt}", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BottomComposer(onNewWatcher: () -> Unit, onHome: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onHome, modifier = Modifier.weight(1f)) { Text("Home") }
        Button(onClick = onNewWatcher, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
            Text("New")
        }
    }
}

@Composable
private fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            StatusRow("Notifications", "Firebase Cloud Messaging")
            StatusRow("Quiet hours", "23:00-07:00")
            StatusRow("Model routing", "Server-side Gemini")
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Nothing is being watched yet.", fontWeight = FontWeight.Medium)
        Text("Create a watcher and let the cloud keep checking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InlineNotice(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        color = MaterialTheme.colorScheme.onErrorContainer
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusDot(status: String) {
    val color = when (status) {
        "active" -> MaterialTheme.colorScheme.primary
        "checking" -> Color(0xFFDAA520)
        "completed" -> Color(0xFF111111)
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}
