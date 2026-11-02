package com.nexauren.samplepackcreator

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Background = Color(0xFF0B0914)
private val SurfaceOne = Color(0xFF171326)
private val SurfaceTwo = Color(0xFF211A35)
private val Accent = Color(0xFF9A78FF)
private val AccentBlue = Color(0xFF35C8D8)
private val TextMain = Color(0xFFF6F2FF)
private val TextMuted = Color(0xFFAAA2BD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 9, 20)
        window.navigationBarColor = android.graphics.Color.rgb(11, 9, 20)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                secondary = AccentBlue,
                background = Background,
                surface = SurfaceOne,
                onPrimary = Color.White,
                onBackground = TextMain,
                onSurface = TextMain
            )) {
                SamplePackCreatorApp()
            }
        }
    }
}

@Composable
private fun SamplePackCreatorApp() {
    val context = LocalContext.current
    val categoryStore = remember { CategoryStore(context) }
    var categories by remember { mutableStateOf(categoryStore.load()) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showPackCreator by rememberSaveable { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<SampleCategory?>(null) }
    var showCategoryEditor by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<SampleCategory?>(null) }
    var activePreview by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember(context) { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) isPlaying = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val preview: (Uri, String) -> Unit = { uri, label ->
        if (activePreview == label && player.isPlaying) {
            player.pause()
        } else if (activePreview == label && player.currentMediaItem != null) {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        } else {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
            activePreview = label
        }
    }

    fun saveCategories(updated: List<SampleCategory>) {
        categories = updated
        categoryStore.save(updated)
    }

    if (showPackCreator) {
        PackCreatorScreen(
            categories = categories,
            activePreview = activePreview,
            isPlaying = isPlaying,
            onPreview = preview,
            onBack = { showPackCreator = false }
        )
        return
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF110E1D), contentColor = TextMain) {
                val labels = listOf("Estúdio", "Categorias", "Síntese", "Biblioteca", "Definições")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.Category,
                    Icons.Default.AutoAwesome,
                    Icons.Default.LibraryMusic,
                    Icons.Default.Settings
                )
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label, fontSize = 9.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    categoryCount = categories.size,
                    projectCount = StudioLibraryStore.projects(context).size,
                    onCreatePack = { showPackCreator = true },
                    onManageCategories = { selectedTab = 1 },
                    onSynthesis = { selectedTab = 2 }
                )
                1 -> CategoriesScreen(
                    categories = categories,
                    onAdd = { editingCategory = null; showCategoryEditor = true },
                    onEdit = { editingCategory = it; showCategoryEditor = true },
                    onDelete = { categoryToDelete = it }
                )
                2 -> SynthesisScreen(categories, activePreview, isPlaying, preview)
                3 -> LibraryScreen(activePreview, isPlaying, preview)
                else -> SettingsScreen()
            }
        }
    }

    if (showCategoryEditor) {
        CategoryEditorDialog(
            initial = editingCategory,
            onDismiss = { showCategoryEditor = false },
            onSave = { category ->
                val updated = if (editingCategory == null) categories + category
                else categories.map { if (it.id == category.id) category else it }
                saveCategories(updated)
                showCategoryEditor = false
            }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = SurfaceOne,
            title = { Text("Eliminar categoria?") },
            text = { Text("A categoria " + category.name + " será removida da tua lista. Os projetos já criados não serão alterados.") },
            confirmButton = {
                TextButton(onClick = {
                    saveCategories(categories.filterNot { it.id == category.id })
                    categoryToDelete = null
                }) { Text("Eliminar", color = Color(0xFFFF7F91)) }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        if (action != null) action()
    }
}

@Composable
private fun HomeScreen(
    categoryCount: Int,
    projectCount: Int,
    onCreatePack: () -> Unit,
    onManageCategories: () -> Unit,
    onSynthesis: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CRIADOR DE PACOTES DE SAMPLES", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("CRIADOR", color = TextMain, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text("O teu estúdio privado de criação sonora", color = TextMuted, fontSize = 12.sp)
            }
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Accent, Color(0xFF4E8BFF)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        Card(shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = SurfaceOne)) {
            Box(
                Modifier.background(Brush.linearGradient(listOf(Color(0xFF30205B), Color(0xFF17172C), Color(0xFF102A3B))))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, tint = AccentBlue, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ESTÚDIO DE CRIAÇÃO SONORA", color = Color(0xFFD7D1FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                    Text("Cria sons.\nTransforma ideias.", color = Color.White, fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold)
                    Text("Sintetiza samples do zero, processa áudio importado e organiza produtos prontos para exportar.", color = Color(0xFFD1CBE4), fontSize = 13.sp, lineHeight = 19.sp)
                    Button(
                        onClick = onCreatePack,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Criar novo pacote de samples", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Categorias", categoryCount.toString(), Icons.Default.Category, Modifier.weight(1f))
            StatCard("Projetos", projectCount.toString(), Icons.Default.Folder, Modifier.weight(1f))
            StatCard("Motor", "Local", Icons.Default.Lock, Modifier.weight(1f))
        }

        ScreenHeader("Ferramentas do estúdio", "Criação e organização")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard("Síntese", "Sons do zero", "〰️", onSynthesis, Modifier.weight(1f))
            FeatureCard("Categorias", "Nome, ícone e cor", "🎛️", onManageCategories, Modifier.weight(1f))
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, tint = Color(0xFF53D6A5), contentDescription = null)
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("Privacidade em primeiro lugar", color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Sem contas, sem nuvem e sem envio de áudio para servidores.", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            Text(value, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(label, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(emoji, fontSize = 25.sp)
            Text(title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CategoriesScreen(
    categories: List<SampleCategory>,
    onAdd: () -> Unit,
    onEdit: (SampleCategory) -> Unit,
    onDelete: (SampleCategory) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp)) {
        ScreenHeader(
            "Categorias",
            "Organiza o teu próprio sistema de samples",
            action = {
                Button(onClick = onAdd, shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Nova")
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Text("Cada categoria pode ter nome, ícone e cor próprios.", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(categories, key = { it.id }) { category ->
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(17.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(45.dp).clip(RoundedCornerShape(14.dp)).background(Color(category.color).copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Text(category.icon, fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(category.name, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Categoria personalizada", color = TextMuted, fontSize = 10.sp)
                        }
                        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(category.color)))
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onEdit(category) }) { Text("Editar", color = Accent, fontSize = 12.sp) }
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF8093), modifier = Modifier.size(19.dp).clickable { onDelete(category) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(initial: SampleCategory?, onDismiss: () -> Unit, onSave: (SampleCategory) -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var selectedIcon by remember(initial?.id) { mutableStateOf(initial?.icon ?: "🥁") }
    var selectedColor by remember(initial?.id) { mutableIntStateOf(initial?.color ?: 0xFF9673FF.toInt()) }
    val icons = listOf("🥁", "👏", "🎚️", "🪘", "🎹", "🎤", "🔊", "✨", "🎸", "🌊", "💥", "🎛️")
    val colors = listOf(0xFF9673FF.toInt(), 0xFF26C6DA.toInt(), 0xFFFF9F68.toInt(), 0xFF59DDAA.toInt(), 0xFFFFC857.toInt(), 0xFF6A9CFF.toInt(), 0xFFE58CFF.toInt(), 0xFFFF7396.toInt(), 0xFF9EADB9.toInt())
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceOne,
        title = { Text(if (initial == null) "Nova categoria" else "Editar categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da categoria (em inglês)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Escolher ícone", color = TextMuted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    icons.forEach { icon ->
                        Box(Modifier.size(39.dp).clip(RoundedCornerShape(11.dp)).background(if (selectedIcon == icon) Accent.copy(alpha = 0.30f) else SurfaceTwo).clickable { selectedIcon = icon }, contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 19.sp)
                        }
                    }
                }
                Text("Cor da categoria", color = TextMuted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { color ->
                        Box(Modifier.size(29.dp).clip(CircleShape).background(Color(color)).clickable { selectedColor = color }, contentAlignment = Alignment.Center) {
                            if (selectedColor == color) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(SampleCategory(initial?.id ?: UUID.randomUUID().toString(), name.trim(), selectedIcon, selectedColor))
            }) { Text("Guardar", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SynthesisScreen(categories: List<SampleCategory>, activePreview: String?, isPlaying: Boolean, onPreview: (Uri, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategoryId by rememberSaveable { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var selectedType by rememberSaveable { mutableStateOf("Kick") }
    var quantity by rememberSaveable { mutableStateOf("10") }
    var duration by rememberSaveable { mutableStateOf("650") }
    var prefix by rememberSaveable { mutableStateOf("Kick") }
    var pitchSemitones by rememberSaveable { mutableStateOf(0f) }
    var decay by rememberSaveable { mutableStateOf(1f) }
    var brightness by rememberSaveable { mutableStateOf(1f) }
    var drive by rememberSaveable { mutableStateOf(1f) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var generated by remember { mutableStateOf(StudioLibraryStore.generatedSamples(context)) }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.firstOrNull()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("Síntese de sons", "Cria áudio original sem importar samples")
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. Categoria de destino", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category.id == selectedCategory?.id,
                            onClick = { selectedCategoryId = category.id; if (prefix == "Kick" || prefix == selectedCategory?.name) prefix = category.name },
                            label = { Text(category.name) },
                            leadingIcon = { Text(category.icon) }
                        )
                    }
                }
                Text("2. Tipo de som", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Cada tipo usa osciladores, envelopes, ruído e modelação próprios.", color = TextMuted, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    AudioSynthesizer.soundTypes.chunked(5).forEach { rowTypes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            rowTypes.forEach { type ->
                                FilterChip(selected = selectedType == type, onClick = { selectedType = type; prefix = type }, label = { Text(type, fontSize = 10.sp) })
                            }
                        }
                    }
                }
                OutlinedTextField(value = prefix, onValueChange = { prefix = it }, label = { Text("Nome base dos ficheiros (inglês)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter(Char::isDigit).take(2) }, label = { Text("Quantidade (1–30)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = duration, onValueChange = { duration = it.filter(Char::isDigit).take(4) }, label = { Text("Duração (ms)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Divider(color = Color(0xFF332B48))
                Text("3. Controlos de criação sonora", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Afinação: " + pitchSemitones.toInt() + " semitons", color = TextMuted, fontSize = 11.sp)
                Slider(value = pitchSemitones, onValueChange = { pitchSemitones = it }, valueRange = -12f..12f, steps = 23)
                Text("Decaimento: " + "%.2f".format(Locale.US, decay) + "x", color = TextMuted, fontSize = 11.sp)
                Slider(value = decay, onValueChange = { decay = it }, valueRange = 0.5f..3f, steps = 9)
                Text("Brilho: " + "%.2f".format(Locale.US, brightness), color = TextMuted, fontSize = 11.sp)
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.25f..2f, steps = 6)
                Text("Saturação: " + "%.2f".format(Locale.US, drive) + "x", color = TextMuted, fontSize = 11.sp)
                Slider(value = drive, onValueChange = { drive = it }, valueRange = 0.5f..4f, steps = 13)
                Button(
                    onClick = {
                        val category = selectedCategory
                        val number = quantity.toIntOrNull()
                        val milliseconds = duration.toIntOrNull()
                        if (category == null) message = "Cria pelo menos uma categoria primeiro."
                        else if (number == null || number !in 1..30) message = "A quantidade deve estar entre 1 e 30."
                        else if (milliseconds == null || milliseconds !in 80..6000) message = "A duração deve estar entre 80 e 6000 ms."
                        else if (prefix.isBlank()) message = "Indica um nome em inglês para os ficheiros."
                        else {
                            busy = true
                            message = null
                            val stableType = selectedType
                            val stablePrefix = prefix
                            val stablePitch = pitchSemitones
                            val stableDecay = decay
                            val stableBrightness = brightness
                            val stableDrive = drive
                            scope.launch {
                                try {
                                    val newFiles = withContext(Dispatchers.Default) {
                                        val directory = File(context.filesDir, "sample-pack-studio/generated/" + safeFilePart(category.name))
                                        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Não foi possível criar a pasta local.")
                                        val labels = listOf("Deep", "Punchy", "Tight", "Dark", "Bright", "Warm", "Dirty", "Crisp", "Raw", "Wide")
                                        buildList {
                                            repeat(number) { index ->
                                                val id = StudioLibraryStore.newId()
                                                val sampleName = safeFilePart(stablePrefix) + "_" + (index + 1).toString().padStart(3, '0') + "_" + labels[index % labels.size] + ".wav"
                                                val file = File(directory, id + ".wav")
                                                file.writeBytes(AudioSynthesizer.generate(
                                                    soundType = stableType,
                                                    durationMs = milliseconds,
                                                    variation = index,
                                                    seed = System.nanoTime() + index * 997L,
                                                    pitchSemitones = stablePitch,
                                                    decay = stableDecay,
                                                    brightness = stableBrightness,
                                                    drive = stableDrive
                                                ))
                                                val sample = GeneratedSample(id, category.id, category.name, stableType, sampleName, file.absolutePath, System.currentTimeMillis())
                                                StudioLibraryStore.addGenerated(context, sample)
                                                add(sample)
                                            }
                                        }
                                    }
                                    generated = StudioLibraryStore.generatedSamples(context)
                                    message = newFiles.size.toString() + " amostras criadas e guardadas na biblioteca local."
                                } catch (exception: Exception) {
                                    message = "Falha na síntese: " + (exception.message ?: "erro desconhecido")
                                } finally { busy = false }
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "A sintetizar áudio…" else "Gerar amostras do zero", fontWeight = FontWeight.Bold)
                }
                message?.let { Text(it, color = if (it.startsWith("Falha")) Color(0xFFFF8495) else AccentBlue, fontSize = 12.sp) }
            }
        }

        ScreenHeader("Amostras sintetizadas", generated.size.toString() + " ficheiro(s) guardado(s)")
        if (generated.isEmpty()) {
            EmptyCard("Ainda não existem samples sintetizados.", "Escolhe um tipo de som e gera a tua primeira coleção.")
        } else {
            generated.take(20).forEach { sample ->
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(15.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, tint = AccentBlue, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sample.name, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sample.categoryName + " · " + sample.soundType, color = TextMuted, fontSize = 10.sp)
                        }
                        AudioPreviewButton(Uri.fromFile(File(sample.path)), sample.id, activePreview, isPlaying, onPreview)
                    }
                }
            }
            if (generated.size > 20) Text("A mostrar os 20 mais recentes. Todos os ficheiros continuam guardados.", color = TextMuted, fontSize = 10.sp)
        }
        Text("Os ficheiros são WAV PCM de 16 bits, gerados localmente. A síntese não depende de Internet.", color = TextMuted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun PackCreatorScreen(
    categories: List<SampleCategory>,
    activePreview: String?,
    isPlaying: Boolean,
    onPreview: (Uri, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var packName by rememberSaveable { mutableStateOf("Amapiano Deep House") }
    var genre by rememberSaveable { mutableStateOf("Amapiano / Deep House") }
    var pendingCategoryId by remember { mutableStateOf<String?>(null) }
    val selectedFiles = remember { mutableStateMapOf<String, List<Uri>>() }
    val quantities = remember { mutableStateMapOf<String, String>() }
    val filePrefixes = remember { mutableStateMapOf<String, String>() }
    val selectedGeneratedIds = remember { mutableStateMapOf<String, Boolean>() }
    val generated = remember { StudioLibraryStore.generatedSamples(context) }
    var creating by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultError by remember { mutableStateOf(false) }
    var coverUri by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewReady by rememberSaveable { mutableStateOf(false) }
    var effectsExpanded by rememberSaveable { mutableStateOf(false) }
    var progressPercent by remember { mutableIntStateOf(0) }
    var progressStage by remember { mutableStateOf("À espera da análise") }
    var createdProject by remember { mutableStateOf<SavedProject?>(null) }
    var createdShareUri by remember { mutableStateOf<String?>(null) }
    var createdDestination by remember { mutableStateOf<String?>(null) }
    var createdHasCover by remember { mutableStateOf(false) }
    val effectSettings = remember { mutableStateMapOf<String, EffectSetting>().apply { EffectCatalog.defaults().forEach { put(it.id, it) } } }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val categoryId = pendingCategoryId
        if (categoryId != null && uris.isNotEmpty()) {
            uris.forEach { uri ->
                try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                catch (_: Exception) { /* Provider may not expose persistable grants. */ }
            }
            selectedFiles[categoryId] = (selectedFiles[categoryId].orEmpty() + uris).distinctBy { it.toString() }
            reviewReady = false
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: Exception) { /* Some image providers expose a temporary read grant only. */ }
            coverUri = uri.toString()
            reviewReady = false
        }
    }
    val coverBitmap = remember(coverUri) {
        coverUri?.let { raw -> runCatching {
            context.contentResolver.openInputStream(Uri.parse(raw))?.use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        }.getOrNull() }
    }

    createdProject?.let { completed ->
        PackSuccessScreen(
            project = completed,
            destination = createdDestination.orEmpty(),
            shareUri = createdShareUri,
            hasCover = createdHasCover,
            onBack = onBack
        )
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextMain, modifier = Modifier.size(24.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Novo pacote de samples", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Importa, sintetiza e organiza áudio", color = TextMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionCard("Informações do produto", "A interface fica em português; os nomes exportados ficam em inglês.") {
            OutlinedTextField(value = packName, onValueChange = { packName = it; reviewReady = false }, label = { Text("Nome do pack (em inglês)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = genre, onValueChange = { genre = it; reviewReady = false }, label = { Text("Género musical") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (coverBitmap != null) {
                    Image(bitmap = coverBitmap, contentDescription = "Capa do sample pack", modifier = Modifier.size(76.dp).clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(76.dp).clip(RoundedCornerShape(13.dp)).background(SurfaceTwo), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Accent, modifier = Modifier.size(30.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(if (coverUri == null) "Capa do produto" else "Capa selecionada", color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("A imagem será incluída no ZIP como cover.", color = TextMuted, fontSize = 10.sp)
                    OutlinedButton(onClick = { coverPicker.launch(arrayOf("image/*")) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(if (coverUri == null) "Carregar capa" else "Alterar capa", fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SectionCard("Adicionar áudio", "Importa vários ficheiros por categoria. Podes adicionar mais ficheiros sem perder os anteriores.") {
            categories.forEach { category ->
                val files = selectedFiles[category.id] ?: emptyList()
                val availableSynth = generated.filter { it.categoryId == category.id }
                val synthFiles = availableSynth.filter { selectedGeneratedIds[it.id] == true }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(39.dp).clip(RoundedCornerShape(12.dp)).background(Color(category.color).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(category.icon, fontSize = 19.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(category.name, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text((files.size + synthFiles.size).toString() + " fonte(s) selecionada(s)", color = TextMuted, fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = { pendingCategoryId = category.id; audioPicker.launch(arrayOf("audio/*")) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Importar", fontSize = 11.sp)
                    }
                }
                if (files.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            files.take(2).joinToString(" · ") { displayName(context, it) } + if (files.size > 2) " · +" + (files.size - 2) else "",
                            modifier = Modifier.weight(1f),
                            color = AccentBlue,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AudioPreviewButton(files.first(), "import_" + category.id, activePreview, isPlaying, onPreview)
                    }
                }
                if (availableSynth.isNotEmpty()) {
                    Text("Amostras sintetizadas disponíveis", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    availableSynth.take(8).forEach { sample ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selectedGeneratedIds[sample.id] == true, onCheckedChange = { selectedGeneratedIds[sample.id] = it; reviewReady = false })
                            Column(Modifier.weight(1f)) {
                                Text(sample.name, color = TextMain, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(sample.soundType, color = TextMuted, fontSize = 9.sp)
                            }
                            AudioPreviewButton(Uri.fromFile(File(sample.path)), sample.id, activePreview, isPlaying, onPreview)
                        }
                    }
                    if (availableSynth.size > 8) Text("A mostrar 8. Os restantes continuam na biblioteca de síntese.", color = TextMuted, fontSize = 9.sp)
                }
                OutlinedTextField(
                    value = quantities[category.id] ?: "10",
                    onValueChange = { value -> quantities[category.id] = value.filter(Char::isDigit).take(3); reviewReady = false },
                    label = { Text("Quantidade final nesta categoria (1–250)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filePrefixes[category.id] ?: category.name,
                    onValueChange = { filePrefixes[category.id] = it; reviewReady = false },
                    label = { Text("Nome base dos ficheiros (inglês)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (category != categories.last()) Divider(color = Color(0xFF332B48))
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171F2E)), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, tint = AccentBlue, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("O Android tenta descodificar WAV, MP3, AAC/M4A, OGG, FLAC e outros formatos conforme os codecs instalados no dispositivo.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        SectionCard("Efeitos de áudio", "65 efeitos individuais. Ativa apenas os que queres incluir e regula a intensidade de cada um.") {
            val enabledEffectCount = effectSettings.values.count { it.enabled && it.intensity > 0f }
            Text("$enabledEffectCount efeito(s) ativo(s) de ${EffectCatalog.all.size}", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            OutlinedButton(onClick = { effectsExpanded = !effectsExpanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (effectsExpanded) "Ocultar controlos dos efeitos" else "Abrir os 65 efeitos e intensidades")
            }
            if (effectsExpanded) {
                EffectCatalog.all.groupBy { it.group }.forEach { (group, definitions) ->
                    Text(group.uppercase(Locale.ROOT), color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    definitions.forEach { definition ->
                        val setting = effectSettings[definition.id] ?: EffectSetting(definition.id)
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = setting.enabled, onCheckedChange = { checked -> effectSettings[definition.id] = setting.copy(enabled = checked); reviewReady = false })
                                Column(Modifier.weight(1f)) {
                                    Text(definition.name, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(definition.description, color = TextMuted, fontSize = 9.sp)
                                }
                                Text(if (setting.enabled) "${setting.intensity.toInt()}%" else "OFF", color = if (setting.enabled) AccentBlue else TextMuted, fontSize = 10.sp)
                            }
                            if (setting.enabled) {
                                Slider(value = setting.intensity, onValueChange = { value -> effectSettings[definition.id] = setting.copy(intensity = value); reviewReady = false }, valueRange = 0f..100f)
                            }
                        }
                    }
                    Divider(color = Color(0xFF332B48))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        val selectedGeneratedNow = selectedGeneratedIds.filterValues { it }.keys
        val plannedCategoryCounts = categories.mapNotNull { category ->
            val sourceCount = selectedFiles[category.id].orEmpty().size + generated.count { it.categoryId == category.id && it.id in selectedGeneratedNow }
            val targetCount = quantities[category.id]?.toIntOrNull() ?: 0
            if (sourceCount > 0) category.name to targetCount else null
        }
        val plannedSamples = plannedCategoryCounts.sumOf { it.second }
        val activeEffects = EffectCatalog.all.mapNotNull { definition ->
            effectSettings[definition.id]?.takeIf { it.enabled && it.intensity > 0f }
        }
        Button(
            onClick = {
                val sourceCount = selectedFiles.values.sumOf { it.size } + selectedGeneratedNow.size
                val invalidQuantity = plannedCategoryCounts.any { it.second !in 1..250 }
                when {
                    packName.isBlank() -> { resultMessage = "Indica o nome do pack."; resultError = true; reviewReady = false }
                    sourceCount == 0 -> { resultMessage = "Importa áudio ou seleciona samples sintetizados."; resultError = true; reviewReady = false }
                    invalidQuantity -> { resultMessage = "Revê as quantidades: cada categoria deve ter entre 1 e 250 samples."; resultError = true; reviewReady = false }
                    else -> { reviewReady = true; resultMessage = null; resultError = false; progressStage = "Análise concluída · pronto para criar"; progressPercent = 0 }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceTwo)
        ) { Text("Analisar e rever o pacote", fontWeight = FontWeight.Bold) }
        if (reviewReady) {
            SectionCard("Revisão antes da criação", "Confirma a estrutura, as quantidades e o processamento.") {
                Text("Samples pedidos: $plannedSamples", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                plannedCategoryCounts.forEach { (name, count) -> Text("$name  ·  $count samples", color = TextMuted, fontSize = 11.sp) }
                Text("Efeitos ativos: ${activeEffects.size} / ${EffectCatalog.all.size}", color = AccentBlue, fontSize = 11.sp)
                Text("Capa: ${if (coverUri == null) "não selecionada" else "incluída"}", color = TextMuted, fontSize = 11.sp)
                Text("A criação inclui análise dos ficheiros, geração de variações, verificação e compressão.", color = TextMuted, fontSize = 10.sp)
            }
        }
        if (creating) {
            SectionCard("A criar o teu sample pack", "O estúdio está a processar, verificar e organizar os ficheiros.") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(122.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = Accent,
                            trackColor = SurfaceTwo,
                            strokeWidth = 9.dp
                        )
                        Text("$progressPercent%", color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text(progressStage, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("A gerar samples, organizar categorias e verificar o ZIP. Não feches o aplicativo durante o processamento.", color = TextMuted, fontSize = 10.sp, lineHeight = 15.sp)
                Row(
                    Modifier.fillMaxWidth().height(28.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(25) { index ->
                        Box(
                            Modifier.padding(horizontal = 2.dp).width(3.dp)
                                .height((7 + (index * 11 % 20)).dp)
                                .clip(CircleShape)
                                .background(if (index % 2 == 0) Accent else AccentBlue)
                        )
                    }
                }
            }
        }
        if (reviewReady) Button(
            onClick = {
                val selectedGenerated = selectedGeneratedIds.filterValues { it }.keys
                val chosen = selectedFiles.values.sumOf { it.size } + selectedGenerated.size
                if (packName.isBlank()) {
                    resultMessage = "Indica o nome do pack."
                    resultError = true
                } else if (chosen == 0) {
                    resultMessage = "Importa áudio ou seleciona samples sintetizados."
                    resultError = true
                } else {
                    creating = true
                    progressPercent = 1
                    progressStage = "A preparar o motor de áudio…"
                    resultMessage = null
                    val fileMap = selectedFiles.toMap()
                    val generatedMap = categories.associate { category ->
                        category.id to generated.filter { it.categoryId == category.id && it.id in selectedGenerated }.map { File(it.path) }
                    }
                    val quantityMap = categories.associate { category ->
                        category.id to (quantities[category.id]?.toIntOrNull() ?: 10).coerceIn(1, 250)
                    }
                    val prefixMap = categories.associate { category -> category.id to (filePrefixes[category.id] ?: category.name) }
                    val stablePackName = packName
                    val stableGenre = genre
                    val categorySnapshot = categories.toList()
                    val effectSnapshot = activeEffects.toList()
                    val stableCoverUri = coverUri?.let { Uri.parse(it) }
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                PackExporter.create(
                                    context = context,
                                    packName = stablePackName,
                                    genre = stableGenre,
                                    categories = categorySnapshot,
                                    selectedFiles = fileMap,
                                    generatedFiles = generatedMap,
                                    quantities = quantityMap,
                                    samplePrefixes = prefixMap,
                                    effects = effectSnapshot,
                                    coverUri = stableCoverUri,
                                    onProgress = { stage, percent ->
                                        scope.launch { progressStage = stage; progressPercent = percent }
                                    }
                                )
                            }
                            progressStage = "Verificação final concluída"
                            progressPercent = 100
                            progressStage = "Verificação final concluída"
                            progressPercent = 100
                            resultMessage = "Pack criado: " + result.fileName + "\n" + result.totalSamples +
                                " amostra(s) exportada(s).\nLocalização: " + result.destination +
                                "\nProjeto guardado na Biblioteca."
                            resultError = false
                            createdShareUri = result.shareUri
                            createdDestination = result.destination
                            createdHasCover = stableCoverUri != null
                            createdProject = StudioLibraryStore.projects(context).firstOrNull { it.id == result.projectId }
                        } catch (exception: Exception) {
                            resultMessage = "Não foi possível criar o pack: " + (exception.message ?: "erro desconhecido")
                            resultError = true
                        } finally { creating = false }
                    }
                }
            },
            enabled = !creating,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text(if (creating) "A criar e verificar…" else "Criar, verificar e exportar ZIP", fontWeight = FontWeight.Bold)
        }
        resultMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = if (resultError) Color(0xFF3A1B2A) else Color(0xFF17362E)), shape = RoundedCornerShape(14.dp)) {
                Text(message, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PackSuccessScreen(
    project: SavedProject,
    destination: String,
    shareUri: String?,
    hasCover: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val categoryCounts = project.samples.groupingBy { it.category }.eachCount().toSortedMap()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextMain, modifier = Modifier.size(23.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(10.dp))
            Text("Resultado do sample pack", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceOne),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF173D36), Color(0xFF171326), Color(0xFF25204B)))
                ).padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF30D68B), modifier = Modifier.size(58.dp))
                    Text("Pack pronto!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(project.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${project.totalSamples} samples criados e verificados", color = Color(0xFF5FE2A4), fontSize = 12.sp)
                    Text("ZIP · ${categoryCounts.size} categorias", color = TextMuted, fontSize = 10.sp)
                }
            }
        }
        Button(
            onClick = {
                val uri = shareUri?.let(Uri::parse)
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/zip")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    runCatching { context.startActivity(Intent.createChooser(intent, "Abrir sample pack")) }
                        .onFailure { android.widget.Toast.makeText(context, "Não foi encontrada uma aplicação para abrir ZIP.", android.widget.Toast.LENGTH_LONG).show() }
                }
            },
            enabled = !shareUri.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Abrir ZIP")
        }
        OutlinedButton(
            onClick = {
                val uri = shareUri?.let(Uri::parse)
                if (uri != null) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "Partilhar sample pack")) }
                        .onFailure { android.widget.Toast.makeText(context, "Não foi possível partilhar o ZIP.", android.widget.Toast.LENGTH_LONG).show() }
                }
            },
            enabled = !shareUri.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            Icon(Icons.Default.GraphicEq, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Partilhar sample pack")
        }
        SectionCard("Estrutura de pastas", "Organização criada dentro do ZIP.") {
            Text(project.title + "/", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            categoryCounts.forEach { (category, count) ->
                Row(Modifier.fillMaxWidth().padding(start = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("$category/   ·   $count samples", color = TextMuted, fontSize = 11.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AudioFile, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("README.txt  ·  informações do produto", color = TextMuted, fontSize = 11.sp)
            }
            if (hasCover) {
                Row(Modifier.fillMaxWidth().padding(start = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("cover.*  ·  capa do produto", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
        SectionCard("Ficheiro guardado", "O sample pack está disponível localmente no teu dispositivo.") {
            Text(project.title + ".zip", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(destination, color = AccentBlue, fontSize = 11.sp)
            Text("O projeto também foi guardado na Biblioteca do aplicativo.", color = TextMuted, fontSize = 10.sp)
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar ao estúdio") }
    }
}

@Composable
private fun AudioPreviewButton(
    uri: Uri,
    label: String,
    activePreview: String?,
    isPlaying: Boolean,
    onPreview: (Uri, String) -> Unit
) {
    val active = activePreview == label
    TextButton(onClick = { onPreview(uri, label) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)) {
        Icon(if (active && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (active && isPlaying) "Pausar" else "Ouvir", tint = if (active) AccentBlue else Accent, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
            content()
        }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = Accent, modifier = Modifier.size(34.dp))
            Text(title, color = TextMain, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LibraryScreen(activePreview: String?, isPlaying: Boolean, onPreview: (Uri, String) -> Unit) {
    val context = LocalContext.current
    var projects by remember { mutableStateOf(StudioLibraryStore.projects(context)) }
    var selectedProject by remember { mutableStateOf<SavedProject?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedProject?>(null) }
    var search by remember { mutableStateOf("") }

    if (selectedProject != null) {
        val project = selectedProject!!
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextMain, modifier = Modifier.size(23.dp).clickable { selectedProject = null })
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(project.title, color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(project.genre, color = TextMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Amostras", project.totalSamples.toString(), Icons.Default.AudioFile, Modifier.weight(1f))
                StatCard("Categorias", project.samples.map { it.category }.distinct().size.toString(), Icons.Default.Category, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            ScreenHeader("Conteúdo do projeto", "Toca em qualquer ficheiro para ouvir")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(project.samples, key = { it.path }) { sample ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(13.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AudioFile, tint = AccentBlue, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sample.name, color = TextMain, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(sample.category + " · " + formatBytes(sample.bytes), color = TextMuted, fontSize = 9.sp)
                            }
                            AudioPreviewButton(Uri.fromFile(File(sample.path)), sample.path, activePreview, isPlaying, onPreview)
                        }
                    }
                }
            }
            Button(onClick = { deleteTarget = project }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF482331)), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Eliminar projeto e ficheiros locais")
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp)) {
            ScreenHeader("Biblioteca", "Projetos guardados neste dispositivo")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Pesquisar projetos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            if (projects.isEmpty()) {
                EmptyCard("Ainda não existem projetos.", "Cria um Sample Pack para o guardar automaticamente nesta biblioteca.")
            } else {
                val filtered = projects.filter { it.title.contains(search, true) || it.genre.contains(search, true) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(filtered, key = { it.id }) { project ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedProject = project },
                            colors = CardDefaults.cardColors(containerColor = SurfaceOne),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = Accent, modifier = Modifier.size(25.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(project.title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(project.genre + " · " + project.totalSamples + " amostras", color = TextMuted, fontSize = 10.sp)
                                    Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(project.createdAt)), color = TextMuted, fontSize = 9.sp)
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Abrir projeto", tint = AccentBlue)
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceOne,
            title = { Text("Eliminar projeto?") },
            text = { Text("O projeto, o ZIP interno e as amostras guardadas na biblioteca serão eliminados. O ZIP já exportado para Downloads não será apagado.") },
            confirmButton = {
                TextButton(onClick = {
                    StudioLibraryStore.deleteProject(context, project)
                    projects = StudioLibraryStore.projects(context)
                    selectedProject = null
                    deleteTarget = null
                }) { Text("Eliminar", color = Color(0xFFFF8093)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("Definições", "Configuração do teu estúdio")
        SettingRow(Icons.Default.Lock, "Modo privado", "Sem contas, sincronização ou envio de áudio.")
        SettingRow(Icons.Default.Palette, "Interface", "Português, tema escuro e estilo futurista.")
        SettingRow(Icons.Default.Tune, "Processamento", "WAV PCM, com descodificação através dos codecs Android disponíveis.")
        SettingRow(Icons.Default.GraphicEq, "Síntese", "Osciladores, envelopes, ruído, variação de pitch e modelação.")
        SettingRow(Icons.Default.Folder, "Biblioteca", "Projetos, ficheiros de áudio e ZIP guardados localmente.")
        Text("Criador de Sample Packs · versão 1.2.0", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String {
    var name = "audio"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) name = cursor.getString(index) ?: name
    }
    return name
}

private fun safeFilePart(value: String): String =
    value.trim().replace(Regex("[^A-Za-z0-9 _.-]"), "")
        .replace(Regex("\\s+"), "_")
        .replace(Regex("_+"), "_")
        .trim('_', '.', ' ').take(48).ifBlank { "Sample" }

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    else -> bytes.toString() + " B"
}
