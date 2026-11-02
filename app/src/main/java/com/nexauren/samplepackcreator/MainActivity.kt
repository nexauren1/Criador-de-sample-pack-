package com.nexauren.samplepackcreator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun saveCategories(updated: List<SampleCategory>) {
        categories = updated
        categoryStore.save(updated)
    }

    if (showPackCreator) {
        PackCreatorScreen(
            categories = categories,
            onBack = { showPackCreator = false }
        )
        return
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF110E1D), contentColor = TextMain) {
                val labels = listOf("Estúdio", "Categorias", "Biblioteca", "Definições")
                val icons = listOf(Icons.Default.Home, Icons.Default.Category, Icons.Default.LibraryMusic, Icons.Default.Settings)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    categoryCount = categories.size,
                    onCreatePack = { showPackCreator = true },
                    onManageCategories = { selectedTab = 1 }
                )
                1 -> CategoriesScreen(
                    categories = categories,
                    onAdd = {
                        editingCategory = null
                        showCategoryEditor = true
                    },
                    onEdit = {
                        editingCategory = it
                        showCategoryEditor = true
                    },
                    onDelete = { categoryToDelete = it }
                )
                2 -> LibraryScreen()
                else -> SettingsScreen()
            }
        }
    }

    if (showCategoryEditor) {
        CategoryEditorDialog(
            initial = editingCategory,
            onDismiss = { showCategoryEditor = false },
            onSave = { category ->
                val updated = if (editingCategory == null) {
                    categories + category
                } else {
                    categories.map { if (it.id == category.id) category else it }
                }
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
            text = { Text("A categoria " + category.name + " será removida da tua lista. Os ficheiros já exportados não serão apagados.") },
            confirmButton = {
                TextButton(onClick = {
                    saveCategories(categories.filterNot { it.id == category.id })
                    categoryToDelete = null
                }) { Text("Eliminar", color = Color(0xFFFF7F91)) }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Cancelar") }
            }
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
    onCreatePack: () -> Unit,
    onManageCategories: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SAMPLE PACK", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Creator", color = TextMain, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text("O teu estúdio privado de sound design", color = TextMuted, fontSize = 12.sp)
            }
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Accent, Color(0xFF4E8BFF)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceOne)
        ) {
            Box(
                Modifier.background(Brush.linearGradient(listOf(Color(0xFF30205B), Color(0xFF17172C), Color(0xFF102A3B))))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, tint = AccentBlue, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SOUND DESIGN WORKSPACE", color = Color(0xFFD7D1FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                    Text("Transforma sons.
Cria a tua identidade.", color = Color.White, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Importa samples, cria variações com efeitos e organiza tudo num pack profissional.", color = Color(0xFFD1CBE4), fontSize = 13.sp, lineHeight = 19.sp)
                    Button(
                        onClick = onCreatePack,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Criar novo Sample Pack", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Categorias", categoryCount.toString(), Icons.Default.Category, Modifier.weight(1f))
            StatCard("Processamento", "Local", Icons.Default.Lock, Modifier.weight(1f))
            StatCard("Exportação", "ZIP", Icons.Default.Folder, Modifier.weight(1f))
        }

        ScreenHeader("O teu estúdio", "Ferramentas principais")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard("Categorias", "Cria e personaliza", "🎛️", onManageCategories, Modifier.weight(1f))
            FeatureCard("Variações", "8 efeitos WAV", "〰️", onCreatePack, Modifier.weight(1f))
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
    Card(
        modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceOne),
        shape = RoundedCornerShape(18.dp)
    ) {
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
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(45.dp).clip(RoundedCornerShape(14.dp)).background(Color(category.color).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
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
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFFF8093),
                            modifier = Modifier.size(19.dp).clickable { onDelete(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    initial: SampleCategory?,
    onDismiss: () -> Unit,
    onSave: (SampleCategory) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var selectedIcon by remember(initial?.id) { mutableStateOf(initial?.icon ?: "🥁") }
    var selectedColor by remember(initial?.id) { mutableIntStateOf(initial?.color ?: 0xFF9673FF.toInt()) }
    val icons = listOf("🥁", "👏", "🎚️", "🪘", "🎹", "🎤", "🔊", "✨", "🎸", "🌊", "💥", "🎛️")
    val colors = listOf(
        0xFF9673FF.toInt(), 0xFF26C6DA.toInt(), 0xFFFF9F68.toInt(),
        0xFF59DDAA.toInt(), 0xFFFFC857.toInt(), 0xFF6A9CFF.toInt(),
        0xFFE58CFF.toInt(), 0xFFFF7396.toInt(), 0xFF9EADB9.toInt()
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceOne,
        title = { Text(if (initial == null) "Nova categoria" else "Editar categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da categoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Escolher ícone", color = TextMuted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    icons.forEach { icon ->
                        Box(
                            Modifier.size(39.dp).clip(RoundedCornerShape(11.dp))
                                .background(if (selectedIcon == icon) Accent.copy(alpha = 0.30f) else SurfaceTwo)
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) { Text(icon, fontSize = 19.sp) }
                    }
                }
                Text("Cor da categoria", color = TextMuted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { color ->
                        Box(
                            Modifier.size(29.dp).clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(SampleCategory(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        icon = selectedIcon,
                        color = selectedColor
                    ))
                }
            ) { Text("Guardar", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun PackCreatorScreen(categories: List<SampleCategory>, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var packName by rememberSaveable { mutableStateOf("Amapiano Deep House") }
    var genre by rememberSaveable { mutableStateOf("Amapiano / Deep House") }
    var pendingCategoryId by remember { mutableStateOf<String?>(null) }
    val selectedFiles = remember { mutableStateMapOf<String, List<Uri>>() }
    val quantities = remember { mutableStateMapOf<String, String>() }
    val filePrefixes = remember { mutableStateMapOf<String, String>() }
    var creating by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultError by remember { mutableStateOf(false) }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val categoryId = pendingCategoryId
        if (categoryId != null && uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                    // Some document providers do not offer persistable grants.
                }
            }
            selectedFiles[categoryId] = uris
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextMain, modifier = Modifier.size(24.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Novo Sample Pack", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Configura o teu próximo produto", color = TextMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionCard("Informações do produto", "Os nomes exportados serão em inglês.") {
            OutlinedTextField(value = packName, onValueChange = { packName = it }, label = { Text("Nome do pack") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Género musical") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(14.dp))
        SectionCard("Adicionar áudio", "Seleciona os ficheiros de origem para cada categoria.") {
            categories.forEach { category ->
                val files = selectedFiles[category.id] ?: emptyList()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(39.dp).clip(RoundedCornerShape(12.dp)).background(Color(category.color).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Text(category.icon, fontSize = 19.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(category.name, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(if (files.isEmpty()) "Nenhum ficheiro selecionado" else files.size.toString() + " ficheiro(s) selecionado(s)", color = TextMuted, fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            pendingCategoryId = category.id
                            audioPicker.launch(arrayOf("audio/*"))
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Selecionar", fontSize = 11.sp)
                    }
                }
                if (files.isNotEmpty()) {
                    Text(
                        files.take(2).joinToString(" • ") { displayName(context, it) } + if (files.size > 2) " • +" + (files.size - 2) else "",
                        color = AccentBlue,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedTextField(
                    value = quantities[category.id] ?: "10",
                    onValueChange = { value -> quantities[category.id] = value.filter { it.isDigit() }.take(3) },
                    label = { Text("Quantidade pretendida de samples") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filePrefixes[category.id] ?: category.name,
                    onValueChange = { filePrefixes[category.id] = it },
                    label = { Text("Nome base dos ficheiros (em inglês)") },
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
                Text("Os ficheiros WAV PCM de 16 bits podem receber oito tratamentos diferentes. Formatos não suportados são preservados como originais, sem falsas variações.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val chosen = selectedFiles.values.sumOf { it.size }
                if (packName.isBlank()) {
                    resultMessage = "Indica o nome do pack."
                    resultError = true
                } else if (chosen == 0) {
                    resultMessage = "Seleciona pelo menos um ficheiro de áudio."
                    resultError = true
                } else {
                    creating = true
                    resultMessage = null
                    val fileMap = selectedFiles.toMap()
                    val quantityMap = categories.associate { category ->
                        category.id to (quantities[category.id]?.toIntOrNull() ?: 10).coerceIn(1, 250)
                    }
                    val prefixMap = categories.associate { category ->
                        category.id to (filePrefixes[category.id] ?: category.name)
                    }
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                PackExporter.create(
                                    context = context,
                                    packName = packName,
                                    genre = genre,
                                    categories = categories,
                                    selectedFiles = fileMap,
                                    quantities = quantityMap,
                                    samplePrefixes = prefixMap
                                )
                            }
                            resultMessage = "Pack criado: " + result.fileName + "\n" + result.totalSamples + " sample(s) exportado(s).\nLocalização: " + result.destination
                            resultError = false
                        } catch (exception: Exception) {
                            resultMessage = "Não foi possível criar o pack: " + (exception.message ?: "erro desconhecido")
                            resultError = true
                        } finally {
                            creating = false
                        }
                    }
                }
            },
            enabled = !creating,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            if (creating) {
                Text("A criar o teu Sample Pack…", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Criar e exportar ZIP", fontWeight = FontWeight.Bold)
            }
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
private fun SectionCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
            content()
        }
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String {
    var name = "audio"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) name = it.getString(index) ?: name
    }
    return name
}

@Composable
private fun LibraryScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        ScreenHeader("Biblioteca", "Os teus produtos exportados")
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(68.dp).clip(RoundedCornerShape(20.dp)).background(Accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Accent, modifier = Modifier.size(34.dp))
                }
                Text("A tua biblioteca começa aqui", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Depois de criares um pack, o ficheiro ZIP aparece na pasta Downloads / Sample Pack Creator.", color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceOne), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, tint = Color(0xFF53D6A5), contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Os projetos e ficheiros permanecem no teu dispositivo.", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("Definições", "Configuração do teu estúdio")
        SettingRow(Icons.Default.Lock, "Modo privado", "Sem contas, sincronização ou envio de áudio.")
        SettingRow(Icons.Default.Palette, "Interface", "Português, tema escuro e estilo futurista.")
        SettingRow(Icons.Default.Tune, "Processamento", "Tratamentos locais para WAV PCM de 16 bits.")
        SettingRow(Icons.Default.Folder, "Exportação", "ZIP guardado em Downloads / Sample Pack Creator.")
        Text("Sample Pack Creator · versão 1.0.0", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp))
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
