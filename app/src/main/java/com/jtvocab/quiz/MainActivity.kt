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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        "Nordic" -> Color(0xFF030712)
        else -> Color(0xFF030712)
    }

    val surfaceColor = when (state.theme) {
        "Deepsea" -> Color(0xFF111827)
        "Evergreen" -> Color(0xFF065F46)
        "Parchment" -> Color(0xFFF5F5DC)
        "Nordic" -> Color(0xFF111827)
        else -> Color(0xFF111827)
    }

    val isParchment = state.theme == "Parchment"
    val onSurface = if (isParchment) Color(0xFF1C1917) else Color.White
    val onBackground = if (isParchment) Color(0xFF1C1917) else Color.White

    val colorScheme = if (isParchment) {
        lightColorScheme(
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
                    onCatSelect = { 
                        cat = it
                        viewModel.startQuiz(it, 0)
                        scope.launch { drawerState.close() }
                    },
                    onModeSelect = {
                        mode = it
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
        Scaffold(
            topBar = {
                CustomTopBar(
                    streak = state.streak,
                    title = "JT VOCAB QUIZ",
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                ModeSelector(mode) { mode = it }
                
                if (mode != "comp") {
                    SetSelector(currentSet = viewModel.currentSetIndex.value) { setIndex ->
                        viewModel.startQuiz(cat, setIndex)
                    }
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
                fontSize = 18.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "0%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        StreakBadge(streak)
    }
}

@Composable
fun Header(streak: Int, achievementIcon: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "JT VOCAB",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "SSC EXAM 2026",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(achievementIcon)
            StreakBadge(streak)
        }
    }
}

@Composable
fun Badge(icon: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 20.sp)
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
    onCatSelect: (String) -> Unit,
    onModeSelect: (String) -> Unit,
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
                fontSize = 20.sp,
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
            icon = Icons.Default.Menu,
            title = "VOCAB PRACTICE",
            subtitle = "OW / Set 1",
            isSelected = currentMode == "quiz",
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

        DrawerItem(
            icon = Icons.Default.Info,
            title = "WEAK LIST",
            subtitle = "0 terms to revise",
            isSelected = false,
            color = MaterialTheme.colorScheme.error,
            onClick = {}
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
fun SetSelector(currentSet: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(10) { index ->
            val setNum = index + 1
            val isSelected = currentSet == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SET $setNum",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
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
                    Text("SETTINGS", fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
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
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
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
    
    if (quizItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a category to start", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        itemsIndexed(quizItems, key = { _, q -> q.item.id + q.isCorrect }) { index, quiz ->
            QuizCard(index, quiz, viewModel) { viewModel.submitAnswer(index, it) }
        }
    }
}

// ChallengeCard removed as requested

@Composable
fun QuizCard(index: Int, quiz: VocabViewModel.QuizItem, viewModel: VocabViewModel, onAnswer: (String) -> Unit) {
    val insight by viewModel.currentInsight
    val loadingAI by viewModel.loadingAI
    val isShowingThisInsight = insight?.word == quiz.item.w

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f))
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("#${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                quiz.item.w,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                lineHeight = 36.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

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
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(
                        option,
                        modifier = Modifier.padding(20.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (quiz.isAnswered && isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SESSION MASTERY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        Text("0/50", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = 0f,
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)
                    )
                }
                
                Spacer(Modifier.width(24.dp))
                Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                Spacer(Modifier.width(24.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    Text("0", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
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
                        else viewModel.fetchInsight(quiz.item.a, quiz.item.cat)
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
    
    if (quizItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a category to learn", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items = quizItems) { quizItem ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(quizItem.item.a.take(1).uppercase(), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(quizItem.item.a, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(quizItem.item.w, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), lineHeight = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(quizItem.item.h, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(0.7f))
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
            Text("PQRS JUMBLE ${index + 1}/$total", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(
                progress = (index + 1).toFloat() / total,
                modifier = Modifier.width(100.dp).height(4.dp).clip(CircleShape),
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
                        Column(modifier = Modifier.padding(bottom = 24.dp)) {
                            Text("Q${i+1}: ${q.q}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(16.dp))
                            q.options.forEach { option ->
                                var isSelected by remember { mutableStateOf(false) }
                                Surface(
                                    onClick = { isSelected = !isSelected },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.05f)),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.05f) else Color.Transparent
                                ) {
                                    Text(option, modifier = Modifier.padding(18.dp), fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
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
            
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                cloze.blanks.forEach { blank ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(
                                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(blank.index.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(16.dp))
                            Text("SELECT WORD FOR BLANK (${blank.index})", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), letterSpacing = 1.sp)
                        }
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 8.dp
                        ) {
                            blank.options.forEach { option ->
                                var isSelected by remember { mutableStateOf(false) }
                                Surface(
                                    onClick = { isSelected = !isSelected },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(0.3f),
                                    border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(0.1f)),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        option, 
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), 
                                        fontWeight = FontWeight.Black, 
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                }
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
        
        // Answer Chips at bottom
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val options = listOf(pqrs.correctSequence, "PSQR", "RQPS", "QPSR").distinct().take(3)
            options.forEach { seq ->
                var isSelected by remember { mutableStateOf(false) }
                Surface(
                    onClick = { isSelected = !isSelected },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(0.1f))
                ) {
                    Text(
                        seq, 
                        modifier = Modifier.padding(vertical = 16.dp), 
                        textAlign = TextAlign.Center, 
                        fontWeight = FontWeight.Black, 
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
