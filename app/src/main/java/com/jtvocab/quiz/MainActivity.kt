package com.jtvocab.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.jtvocab.quiz.viewmodel.VocabViewModel
import com.jtvocab.quiz.model.ClozeQuestion
import com.jtvocab.quiz.model.PQRSQuestion
import com.jtvocab.quiz.model.*

import com.jtvocab.quiz.data.VocabRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VocabRepository.init(this)
        setContent {
            VocabTheme {
                VocabApp()
            }
        }
    }
}

@Composable
fun VocabTheme(viewModel: VocabViewModel = viewModel(), content: @Composable () -> Unit) {
    val state by viewModel.state
    val accentColor = Color(state.accentColor)
    
    val backgroundColor = when (state.theme) {
        "Deepsea" -> Color(0xFF030712)
        "Evergreen" -> Color(0xFF064E3B)
        "Parchment" -> Color(0xFFFDFCF2)
        "Nordic" -> Color(0xFFE5E7EB)
        else -> Color(0xFF030712)
    }

    val surfaceColor = when (state.theme) {
        "Deepsea" -> Color(0xFF111827)
        "Evergreen" -> Color(0xFF065F46)
        "Parchment" -> Color(0xFFF5F5DC)
        "Nordic" -> Color(0xFFFFFFFF)
        else -> Color(0xFF111827)
    }

    val isParchment = state.theme == "Parchment" || state.theme == "Nordic"
    val onSurface = if (isParchment) Color(0xFF1F2937) else Color.White
    val onBackground = if (isParchment) Color(0xFF111827) else Color.White

    val colorScheme = if (isParchment) {
        lightColorScheme(
            primary = accentColor,
            background = backgroundColor,
            surface = surfaceColor,
            secondary = accentColor.copy(alpha = 0.7f),
            onPrimary = Color.White,
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF1F2937),
            surfaceVariant = surfaceColor.copy(alpha = 0.5f),
            onSurfaceVariant = Color(0xFF1F2937).copy(alpha = 0.7f)
        )
    } else {
        darkColorScheme(
            primary = accentColor,
            background = backgroundColor,
            surface = surfaceColor,
            secondary = accentColor.copy(alpha = 0.7f),
            onPrimary = Color.White,
            onBackground = onBackground,
            onSurface = onSurface,
            surfaceVariant = surfaceColor.copy(alpha = 0.5f),
            onSurfaceVariant = onSurface.copy(alpha = 0.7f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabApp(viewModel: VocabViewModel = viewModel()) {
    var mode by remember { mutableStateOf("quiz") } // quiz, learn, comp
    var cat by remember { mutableStateOf("ow") } // ow, sy, id
    val state by viewModel.state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startQuiz(cat, 0)
    }

    if (showSettings) {
        SettingsDialog(viewModel) { showSettings = false }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerContentColor = MaterialTheme.colorScheme.onBackground
            ) {
                DashboardContent(
                    state = state,
                    currentCat = cat,
                    currentMode = mode,
                    isChallengeMode = viewModel.isChallengeMode.value,
                    onCatSelect = { 
                        cat = it
                        viewModel.setChallengeMode(false)
                        viewModel.startQuiz(it, 0)
                        scope.launch { drawerState.close() }
                    },
                    onModeSelect = {
                        mode = it
                        viewModel.setChallengeMode(false)
                        scope.launch { drawerState.close() }
                    },
                    onChallengeClick = {
                        mode = "quiz"
                        viewModel.setChallengeMode(true)
                        scope.launch { drawerState.close() }
                    },
                    onSettingsOpen = {
                        showSettings = true
                        scope.launch { drawerState.close() }
                    },
                    onClose = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        },
        gesturesEnabled = true
    ) {
        val isChallenge = viewModel.isChallengeMode.value
        Scaffold(
            topBar = {
                CustomTopBar(
                    streak = state.streak,
                    title = if (isChallenge) "75 DAY CHALLENGE" else if (mode == "comp") "DAILY COMPREHENSION" else "JT VOCAB QUIZ",
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (!isChallenge) {
                    ModeSelector(mode) { mode = it; viewModel.setChallengeMode(false) }
                    
                    if (mode != "comp") {
                        SetSelector(currentCat = cat, currentSet = viewModel.currentSetIndex.value) { setIndex ->
                            viewModel.startQuiz(cat, setIndex)
                        }
                    }
                } else {
                    ChallengeStrategyCard(viewModel)
                }
                
                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    when (mode) {
                        "quiz" -> QuizSection(viewModel)
                        "learn" -> LearnSection(viewModel, cat)
                        "comp" -> CompSection(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeStrategyCard(viewModel: VocabViewModel) {
    val day by viewModel.challengeDay
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.1f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.2f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("DAY $day STRATEGY", fontWeight = FontWeight.Black, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Text("Plan 2026", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(12.dp))
            Text("Goal: Master 82 terms (OWS: 27, SY: 24, ID: 24, PH: 7)", fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(end = 16.dp)) {
                items(75) { index ->
                    val d = index + 1
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (day == d) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { viewModel.setChallengeDay(d) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(d.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (day == d) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTopBar(streak: Int, title: String, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "0%",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        StreakBadge(streak)
    }
}

@Composable
fun StreakBadge(streak: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text("STREAK 🔥", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(streak.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun DashboardContent(
    state: AppState,
    currentCat: String,
    currentMode: String,
    isChallengeMode: Boolean,
    onCatSelect: (String) -> Unit,
    onModeSelect: (String) -> Unit,
    onChallengeClick: () -> Unit,
    onSettingsOpen: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "JT DASHBOARD",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.Close, 
                null, 
                tint = MaterialTheme.colorScheme.onSurface.copy(0.5f), 
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClose() }
            )
        }

        Spacer(Modifier.height(40.dp))

        DrawerItem(
            icon = Icons.Default.Face,
            title = "AI COMPREHENSION",
            subtitle = "Generate Daily Tasks",
            isSelected = currentMode == "comp",
            onClick = { onModeSelect("comp") }
        )

        Spacer(Modifier.height(16.dp))

        DrawerItem(
            icon = Icons.Default.Star,
            title = "75 DAY CHALLENGE",
            subtitle = "Mastery Plan 2026",
            isSelected = isChallengeMode,
            onClick = { onChallengeClick() }
        )

        Spacer(Modifier.height(16.dp))

        DrawerItem(
            icon = Icons.Default.Menu,
            title = "VOCAB PRACTICE",
            subtitle = "Standard Sets",
            isSelected = currentMode == "quiz" && !isChallengeMode,
            onClick = { onModeSelect("quiz") }
        )

        Spacer(Modifier.height(24.dp))

        Text("VOCAB CATEGORIES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        Spacer(Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cats = listOf("ow" to "OWS", "sy" to "SYNO")
            cats.forEach { (id, label) ->
                CatButton(label, currentCat == id, modifier = Modifier.weight(1f)) { onCatSelect(id) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cats = listOf("id" to "IDIOM", "ph" to "PHRASAL")
            cats.forEach { (id, label) ->
                CatButton(label, currentCat == id, modifier = Modifier.weight(1f)) { onCatSelect(id) }
            }
        }

        Spacer(Modifier.height(32.dp))

        val weakCount = state.weakListOW.size + state.weakListSY.size + state.weakListID.size + state.weakListPH.size
        DrawerItem(
            icon = Icons.Default.Info,
            title = "WEAK LIST",
            subtitle = "$weakCount terms to revise",
            isSelected = false,
            color = MaterialTheme.colorScheme.error,
            onClick = { 
                viewModel.startQuiz("weak", -1, isWeakMode = true)
                onModeSelect("quiz") 
            }
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsOpen() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text("APP SETTINGS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    color: Color = Color(0xFF3B82F6),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) color else Color.Transparent)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.8f))
            Text(subtitle, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(0.7f) else MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }
}

@Composable
fun CatButton(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.6f))
    }
}

@Composable
fun SetSelector(currentCat: String, currentSet: Int, onSelect: (Int) -> Unit) {
    val size = when(currentCat) {
        "ow" -> 50
        "sy" -> 25
        "id" -> 25
        "ph" -> 10
        else -> 25
    }
    val listSize = when(currentCat) {
        "ow" -> VocabRepository.ows.size
        "sy" -> VocabRepository.synonyms.size
        "id" -> VocabRepository.idioms.size
        "ph" -> VocabRepository.phrasal.size
        else -> 0
    }
    val count = if (listSize > 0) (listSize + size - 1) / size else 10
    val displayCount = minOf(count, 50) // Limit to 50 sets for UI sanity

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(displayCount) { index ->
            val setNum = index + 1
            val isSelected = currentSet == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SET $setNum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(viewModel: VocabViewModel, onClose: () -> Unit) {
    val state by viewModel.state
    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SETTINGS", fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)) }
                }

                Spacer(Modifier.height(24.dp))
                Text("ACCENT COLOR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val colors = listOf(0xFF3B82F6, 0xFF06B6D4, 0xFF10B981, 0xFFF43F5E, 0xFFF59E0B, 0xFF8B5CF6)
                    colors.forEach { colorVal ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(if (state.accentColor == colorVal) 4.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { viewModel.setAccentColor(colorVal) }
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
                Text("APPEARANCE THEMES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                Spacer(Modifier.height(16.dp))
                
                val themes = listOf("Deepsea", "Evergreen", "Parchment", "Nordic")
                themes.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { themeName ->
                            ThemeButton(themeName, state.theme == themeName, modifier = Modifier.weight(1f)) { viewModel.setTheme(themeName) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(32.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f))
                Spacer(Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsActionButton(Icons.Default.KeyboardArrowDown, "EXPORT", modifier = Modifier.weight(1f))
                    SettingsActionButton(Icons.Default.KeyboardArrowUp, "IMPORT", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ThemeButton(name: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.1f), RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f))
    }
}

@Composable
fun SettingsActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, fontWeight = FontWeight.Black, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
    }
}

@Composable
fun ModeSelector(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        listOf("quiz" to "QUIZ MODE", "learn" to "LEARN MODE").forEach { (id, label) ->
            val isSelected = current == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
        }
    }
}

@Composable
fun CategoryTabs(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf("ow" to "OWS", "sy" to "Syno", "id" to "Idiom").forEach { (id, label) ->
            val isSelected = current == id
            TextButton(onClick = { onSelect(id) }) {
                Text(
                    label,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.4f),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun QuizSection(viewModel: VocabViewModel) {
    val quizItems by viewModel.currentQuizBatch
    val quizCount = quizItems.count { it.isAnswered }
    val totalCount = quizItems.size
    val currentSetIndex by viewModel.currentSetIndex
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(currentSetIndex, quizItems.size) {
        listState.scrollToItem(0)
    }
    
    if (quizItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a category to start", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (quizItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable { viewModel.setChallengeMode(false) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SESSION MASTERY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        Text("$quizCount/$totalCount", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = if (totalCount > 0) quizCount.toFloat() / totalCount else 0f,
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
                    )
                }
                
                Spacer(Modifier.width(24.dp))
                Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                Spacer(Modifier.width(24.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val score by viewModel.score
                    Text("SCORE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    Text(score.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            itemsIndexed(quizItems, key = { _, q -> q.item.id + (q.selectedOption == q.item.a) }) { index, quiz ->
                if (index == 0 || quizItems[index - 1].item.cat != quiz.item.cat) {
                    val label = when(quiz.item.cat) {
                        "ow" -> "ONE WORD SUBSTITUTION"
                        "sy" -> "SYNONYMS"
                        "id" -> "IDIOMS"
                        "ph" -> "PHRASAL VERBS"
                        else -> "VOCABULARY"
                    }
                    Text(
                        label,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                }
                QuizCard(index, quiz, viewModel) { viewModel.submitAnswer(index, it) }
            }
            
            if (quizItems.isNotEmpty()) {
                item {
                    Button(
                        onClick = { viewModel.setChallengeMode(false) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("FINISH SESSION", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ChallengeCard removed as requested

@Composable
fun QuizCard(index: Int, quiz: VocabViewModel.QuizItem, viewModel: VocabViewModel, onAnswer: (String) -> Unit) {
    val insight by viewModel.currentInsight
    val loadingAI by viewModel.loadingAI
    val isShowingThisInsight = insight?.word == quiz.item.w
    var showHint by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("#${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
                Icon(
                    Icons.Default.Info, 
                    null, 
                    tint = if (showHint) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(16.dp).clickable { showHint = !showHint }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showHint) {
                Text(
                    "Hint: ${quiz.item.h}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            Text(
                quiz.item.w,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(14.dp))

            quiz.options.forEach { option ->
                val isCorrect = option == quiz.item.a
                val isSelected = quiz.selectedOption == option
                val borderColor = when {
                    quiz.isAnswered && isCorrect -> Color(0xFF10B981)
                    isSelected && !isCorrect -> Color(0xFFEF4444)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(0.1f)
                }
                
                Surface(
                    onClick = { onAnswer(option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(
                        option,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (quiz.isAnswered && isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f), RoundedCornerShape(16.dp))
                    .clickable { 
                        if (isShowingThisInsight) viewModel.clearInsight() 
                        else {
                            val wordToFetch = if (quiz.item.cat == "ow") quiz.item.a else quiz.item.w
                            viewModel.fetchInsight(wordToFetch, quiz.item.cat)
                        }
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loadingAI && isShowingThisInsight) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI CONTEXT & MNEMONIC", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            }
 
            AnimatedVisibility(visible = isShowingThisInsight && insight != null) {
                insight?.let { res ->
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text("EXAM CONTEXT 2026", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(res.context, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                        Text("MNEMONIC (MEMORY TRICK)", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        Text(res.mnemonic, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun LearnSection(viewModel: VocabViewModel, cat: String) {
    val quizItems by viewModel.currentQuizBatch
    val currentSetIndex by viewModel.currentSetIndex
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(currentSetIndex, quizItems.size) {
        listState.scrollToItem(0)
    }
    
    if (quizItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a category to learn", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp), 
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(items = quizItems) { index, quizItem ->
            if (index == 0 || quizItems[index - 1].item.cat != quizItem.item.cat) {
                val label = when(quizItem.item.cat) {
                    "ow" -> "ONE WORD SUBSTITUTION"
                    "sy" -> "SYNONYMS"
                    "id" -> "IDIOMS"
                    "ph" -> "PHRASAL VERBS"
                    else -> "VOCABULARY"
                }
                Text(
                    label,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#${index + 1}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (quizItem.item.cat == "id") {
                            Text(quizItem.item.w, fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(quizItem.item.a, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f), lineHeight = 14.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(quizItem.item.h, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        } else {
                            Text(quizItem.item.a, fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(quizItem.item.w, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), lineHeight = 14.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(quizItem.item.h, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompSection(viewModel: VocabViewModel) {
    val compType by viewModel.currentCompType
    val pqrsList by viewModel.dailyPQRS
    val clozeList by viewModel.dailyCloze
    val rc by viewModel.dailyRC
    
    val pqrsIndex by viewModel.currentPQRSIndex
    val clozeIndex by viewModel.currentClozeIndex
    
    val isLoading by viewModel.loadingComp
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Architecting Daily Tasks...", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            // Header
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    "AI COMPREHENSION",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                Text(
                    "Last Mile Challenge (2026 Pattern)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Daily Pulse Card
            val pulse by viewModel.dailyPulse
            if (pulse != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.05f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.1f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("DAILY VOCABULARY PULSE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(pulse!!.optString("word", ""), fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(pulse!!.optString("insight", ""), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(0.5f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Text(pulse!!.optString("usage", ""), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Navigation Bar for Comp types
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("PQRS", "CLOZE", "RC").forEach { type ->
                        val isSelected = compType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) 
                                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                                    else 
                                        SolidColor(MaterialTheme.colorScheme.surface.copy(0.5f))
                                )
                                .clickable { viewModel.setCompType(type) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                type, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 11.sp, 
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                IconButton(
                    onClick = { viewModel.generateComp() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "AI Generate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (compType) {
                    "PQRS" -> {
                        val currentPQRS = pqrsList[pqrsIndex]
                        PQRSSingleView(
                            pqrs = currentPQRS,
                            index = pqrsIndex,
                            total = pqrsList.size,
                            onPrev = { viewModel.prevPQRS() },
                            onNext = { viewModel.nextPQRS() }
                        )
                    }
                    "CLOZE" -> {
                        val currentCloze = clozeList[clozeIndex]
                        ClozeSingleView(
                            cloze = currentCloze,
                            index = clozeIndex,
                            total = clozeList.size,
                            onPrev = { viewModel.prevCloze() },
                            onNext = { viewModel.nextCloze() }
                        )
                    }
                    "RC" -> {
                        RCCard(rc)
                    }
                }
            }
        }
    }
}

@Composable
fun PQRSSingleView(pqrs: PQRSQuestion, index: Int, total: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PQRS JUMBLE ${index + 1}/$total", fontWeight = FontWeight.Black, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(
                progress = (index + 1).toFloat() / total,
                modifier = Modifier.width(80.dp).height(3.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            item {
                PQRSCard(pqrs)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPrev,
                enabled = index > 0,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("PREVIOUS", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = onNext,
                enabled = index < total - 1,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("NEXT", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun ClozeSingleView(cloze: ClozeQuestion, index: Int, total: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
        Text("CLOZE PASSAGE ${index + 1}/$total", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            item {
                ClozeCard(cloze)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPrev, enabled = index > 0, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Text("PREV", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
            Button(onClick = onNext, enabled = index < total - 1, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(16.dp)) {
                Text("NEXT", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun RCCard(rc: com.jtvocab.quiz.model.RCQuestion) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.1f))
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("LEVEL: LAST MILE", fontWeight = FontWeight.Black, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(rc.passage, fontSize = 15.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                    
                    Spacer(Modifier.height(32.dp))
                    rc.questions.forEachIndexed { i, q ->
                        var selectedOption by remember { mutableStateOf<String?>(null) }
                        var isAnswered by remember { mutableStateOf(false) }

                        Column(modifier = Modifier.padding(bottom = 24.dp)) {
                            Text("Q${i+1}: ${q.q}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(12.dp))
                            q.options.forEach { option ->
                                val isSelected = selectedOption == option
                                val isCorrect = option == q.a
                                
                                val borderColor = when {
                                    isAnswered && isCorrect -> Color(0xFF10B981)
                                    isSelected && !isCorrect -> Color(0xFFEF4444)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(0.05f)
                                }

                                Surface(
                                    onClick = { 
                                        if (!isAnswered) {
                                            selectedOption = option
                                            isAnswered = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, borderColor),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.05f) else Color.Transparent
                                ) {
                                    Text(
                                        option, 
                                        modifier = Modifier.padding(16.dp), 
                                        fontSize = 13.sp, 
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, 
                                        color = when {
                                            isAnswered && isCorrect -> Color(0xFF10B981)
                                            isSelected && !isCorrect -> Color(0xFFEF4444)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                            
                            AnimatedVisibility(visible = isAnswered) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.05f))
                                ) {
                                    Text(q.explanation, modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("SUBMIT PASSAGE", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClozeCard(cloze: ClozeQuestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.1f))
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(cloze.passage, fontSize = 16.sp, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurface)
            
            Spacer(Modifier.height(40.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                cloze.blanks.forEach { blank ->
                    var selectedOption by remember { mutableStateOf<String?>(null) }
                    var isAnswered by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(
                                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(blank.index.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("SELECT WORD FOR BLANK (${blank.index})", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), letterSpacing = 1.sp)
                        }
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            blank.options.forEach { option ->
                                val isSelected = selectedOption == option
                                val isCorrect = option == blank.answer
                                
                                val borderColor = when {
                                    isAnswered && isCorrect -> Color(0xFF10B981)
                                    isSelected && !isCorrect -> Color(0xFFEF4444)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(0.1f)
                                }

                                Surface(
                                    onClick = { 
                                        if (!isAnswered) {
                                            selectedOption = option
                                            isAnswered = true
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.1f) else MaterialTheme.colorScheme.surface.copy(0.3f),
                                    border = BorderStroke(1.dp, borderColor),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        option, 
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), 
                                        fontWeight = FontWeight.Black, 
                                        color = when {
                                            isAnswered && isCorrect -> Color(0xFF10B981)
                                            isSelected && !isCorrect -> Color(0xFFEF4444)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isAnswered) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.05f))
                            ) {
                                Text(blank.explanation, modifier = Modifier.padding(10.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PQRSCard(pqrs: PQRSQuestion) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswered by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // S1 Label
        pqrs.s1?.let { s1Text ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text("STARTING ANCHOR (S1)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(s1Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 24.sp)
                }
            }
        }

        // P, Q, R, S Items
        pqrs.sentences.forEachIndexed { i, sentence ->
            val label = when(i) { 0 -> "P"; 1 -> "Q"; 2 -> "R"; else -> "S" }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        sentence.removePrefix("$label: ").removePrefix(label).removePrefix(": ").trim(), 
                        fontSize = 15.sp, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // S6 Label
        pqrs.s6?.let { s6Text ->
             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text("ENDING ANCHOR (S6)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(s6Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 24.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        
        Text("CHOOSE CORRECT SEQUENCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.padding(start = 8.dp))
        
        // Options
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = remember(pqrs.id) { 
                listOf(pqrs.correctSequence, "PSQR", "RQPS", "QPSR").shuffled().distinct().take(4) 
            }
            options.forEach { seq ->
                val isSelected = selectedOption == seq
                val isCorrect = seq == pqrs.correctSequence
                
                val borderColor = when {
                    isAnswered && isCorrect -> Color(0xFF10B981)
                    isSelected && !isCorrect -> Color(0xFFEF4444)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(0.1f)
                }

                Surface(
                    onClick = { 
                        if (!isAnswered) {
                            selectedOption = seq
                            isAnswered = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.1f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(
                        seq, 
                        modifier = Modifier.padding(vertical = 12.dp), 
                        textAlign = TextAlign.Center, 
                        fontWeight = FontWeight.Black, 
                        color = when {
                            isAnswered && isCorrect -> Color(0xFF10B981)
                            isSelected && !isCorrect -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }

        AnimatedVisibility(visible = isAnswered) {
            Column {
                if (pqrs.logicalConnectors.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("LOGICS TO SPOT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pqrs.logicalConnectors.forEach { connector ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(connector, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.05f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.1f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("STRATEGIC EXPLANATION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(pqrs.explanation, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f), lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
