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
    var mode by remember { mutableStateOf("quiz") } // quiz, learn
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
                    viewModel = viewModel,
                    onCatSelect = { 
                        cat = it
                        if (viewModel.isChallengeMode.value) {
                             viewModel.startChallengeQuiz(viewModel.challengeDay.value, it)
                        } else {
                             viewModel.startQuiz(it, 0)
                        }
                        scope.launch { drawerState.close() }
                    },
                    onModeSelect = {
                        mode = it
                        if (it == "quiz" && viewModel.isChallengeMode.value) {
                             viewModel.startChallengeQuiz(viewModel.challengeDay.value, cat)
                        }
                        scope.launch { drawerState.close() }
                    },
                    onChallengeClick = {
                        mode = "quiz"
                        viewModel.setChallengeMode(true, cat)
                        scope.launch { drawerState.close() }
                    },
                    onWeakListClick = {
                        viewModel.startQuiz("weak", -1, isWeakMode = true)
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
        val searchQuery by viewModel.searchQuery

        Scaffold(
            topBar = {
                CustomTopBar(
                    streak = state.streak,
                    title = if (isChallenge) "75 DAY CHALLENGE" else "JT VOCAB QUIZ",
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (searchQuery.isNotEmpty()) {
                    SearchResultsSection(viewModel)
                } else {
                    if (isChallenge) {
                    CategoryTabs(cat) { 
                        cat = it
                        viewModel.startChallengeQuiz(viewModel.challengeDay.value, it)
                    }
                    ChallengeStrategyCard(viewModel, cat)
                } else {
                    ModeSelector(mode) { mode = it }
                    
                    SetSelector(
                        currentCat = cat, 
                        currentSet = viewModel.currentSetIndex.value,
                        completedSets = when(cat) {
                            "ow" -> state.completedOWS
                            "sy" -> state.completedSY
                            "id" -> state.completedID
                            "ph" -> state.completedPH
                            else -> emptySet()
                        }
                    ) { setIndex ->
                        viewModel.startQuiz(cat, setIndex)
                    }
                }
                
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        when (mode) {
                            "quiz" -> QuizSection(viewModel)
                            "learn" -> LearnSection(viewModel, cat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsSection(viewModel: VocabViewModel) {
    val results by viewModel.searchResults
    val searchQuery by viewModel.searchQuery

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SEARCH RESULTS (${results.size})",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                letterSpacing = 1.sp
            )
            Text(
                "CLEAR",
                modifier = Modifier.clickable { viewModel.clearSearch() },
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No results found for \"$searchQuery\"",
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                itemsIndexed(results) { index, item ->
                    LearnCard(index + 1, VocabViewModel.QuizItem(item, emptyList()), viewModel)
                }
            }
        }
    }
}

@Composable
fun ChallengeStrategyCard(viewModel: VocabViewModel, currentCat: String) {
    val day by viewModel.challengeDay
    val state by viewModel.state
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
            Text("Task: Master ${currentCat.uppercase()} for Day $day", fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(end = 16.dp)) {
                items(75) { index ->
                    val d = index + 1
                    val isCompleted = state.completedChallengeDays.contains(d)
                    Box(
                        modifier = Modifier
                            .size(if (isCompleted) 34.dp else 30.dp)
                            .clip(CircleShape)
                            .background(
                                if (day == d) MaterialTheme.colorScheme.primary 
                                else if (isCompleted) Color(0xFF10B981).copy(0.2f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .then(if (isCompleted && day != d) Modifier.border(1.dp, Color(0xFF10B981).copy(0.5f), CircleShape) else Modifier)
                            .clickable { viewModel.setChallengeDay(d, currentCat) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted && day != d) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                        } else {
                            Text(d.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (day == d) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: AppState,
    currentCat: String,
    currentMode: String,
    isChallengeMode: Boolean,
    viewModel: VocabViewModel,
    onCatSelect: (String) -> Unit,
    onModeSelect: (String) -> Unit,
    onChallengeClick: () -> Unit,
    onWeakListClick: () -> Unit,
    onSettingsOpen: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
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

        Spacer(Modifier.height(32.dp))

        // Search Bar in Drawer
        OutlinedTextField(
            value = viewModel.searchQuery.value,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search words or meanings...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.3f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.3f)) },
            trailingIcon = {
                if (viewModel.searchQuery.value.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
        )

        Spacer(Modifier.height(32.dp))

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
                onWeakListClick()
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
fun SetSelector(currentCat: String, currentSet: Int, completedSets: Set<Int>, onSelect: (Int) -> Unit) {
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
            val isCompleted = completedSets.contains(index)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary 
                        else if (isCompleted) MaterialTheme.colorScheme.primary.copy(0.1f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .then(if (isCompleted && !isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(12.dp)) else Modifier)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "SET $setNum",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else if (isCompleted) MaterialTheme.colorScheme.primary.copy(0.8f)
                                else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                    )
                    if (isCompleted) {
                        Icon(
                            Icons.Filled.CheckCircle, 
                            null, 
                            modifier = Modifier.size(12.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
        listOf("ow" to "OWS", "sy" to "SYNO", "id" to "IDIOM", "ph" to "PHRASAL").forEach { (id, label) ->
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
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
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            itemsIndexed(quizItems, key = { _, q -> q.item.id + (q.selectedOption == q.item.a) }) { index, quiz ->
                val setItemsCount = when(quiz.item.cat) {
                    "ow" -> 50
                    "sy" -> 25
                    "id" -> 25
                    "ph" -> 10
                    else -> 25
                }
                val globalSerial = if (currentSetIndex >= 0) (currentSetIndex * setItemsCount) + index + 1 else index + 1

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
                QuizCard(globalSerial, quiz, viewModel) { viewModel.submitAnswer(index, it) }
            }
            
            if (quizItems.isNotEmpty()) {
                item {
                    Button(
                        onClick = { viewModel.finishSession() },
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("FINISH SESSION", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ChallengeCard removed as requested

@OptIn(ExperimentalMaterial3Api::class)
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
                    Text("#$index", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
                androidx.compose.material3.IconButton(
                    onClick = { showHint = !showHint },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Info, 
                        null, 
                        tint = if (showHint) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(18.dp)
                    )
                }
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
            val showInsight = isShowingThisInsight && insight != null
            AnimatedVisibility(visible = showInsight) {
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
        verticalArrangement = Arrangement.spacedBy(16.dp), 
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(items = quizItems) { index, quizItem ->
            val setItemsCount = when(quizItem.item.cat) {
                "ow" -> 50
                "sy" -> 25
                "id" -> 25
                "ph" -> 10
                else -> 25
            }
            val globalSerial = if (currentSetIndex >= 0) (currentSetIndex * setItemsCount) + index + 1 else index + 1

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
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 8.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            }
            
            LearnCard(globalSerial, quizItem, viewModel)
        }

        if (quizItems.isNotEmpty()) {
            item {
                Button(
                    onClick = { viewModel.finishSession() },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("MARK ALL AS LEARNED", fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnCard(index: Int, quiz: VocabViewModel.QuizItem, viewModel: VocabViewModel) {
    val insight by viewModel.currentInsight
    val loadingAI by viewModel.loadingAI
    val item = quiz.item
    val isShowingThisInsight = insight?.word == (if (item.cat == "ow") item.a else item.w)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    (if (item.cat == "id") item.w else item.a) ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "#$index",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.3f)
                )
            }

            Spacer(Modifier.height(6.dp))
            
            Text(
                (if (item.cat == "id") item.a else item.w) ?: "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(14.dp))

            // AI Button
            Surface(
                onClick = { 
                    if (isShowingThisInsight) viewModel.clearInsight() 
                    else {
                        val wordToFetch = if (item.cat == "ow") item.a else item.w
                        viewModel.fetchInsight(wordToFetch, item.cat)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(0.02f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.06f))
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (loadingAI && isShowingThisInsight) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("AI CONTEXT & MNEMONIC", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
            }

            // AI Content
            AnimatedVisibility(visible = isShowingThisInsight && insight != null) {
                insight?.let { ins ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(0.04f))
                            .padding(10.dp)
                    ) {
                        Text("MNEMONIC", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(ins.mnemonic, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                        Spacer(Modifier.height(6.dp))
                        Text("EXAM CONTEXT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(ins.context, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f))
            Spacer(Modifier.height(10.dp))

            // Hindi Meaning Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary.copy(0.5f))
                Spacer(Modifier.width(8.dp))
                Text(
                    item.h,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(0.8f)
                )
            }
        }
    }
}

