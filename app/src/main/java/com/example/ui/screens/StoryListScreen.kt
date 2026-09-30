package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.config.StoryPromptConfig
import com.example.data.db.Story
import com.example.viewmodel.StoryViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryListScreen(
    viewModel: StoryViewModel,
    onOpenStory: (Long) -> Unit,
    onNavigateToGlobalSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.allStories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val images by viewModel.mediaImages.collectAsStateWithLifecycle()
    val bgmTitle by viewModel.bgmTitle.collectAsStateWithLifecycle()
    val isBgmPlaying by viewModel.isBgmPlaying.collectAsStateWithLifecycle()
    val isBgmMuted by viewModel.isBgmMuted.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var storyToDelete by remember { mutableStateOf<Story?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0C0717),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x337A4BCF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "StoryVerse",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFFF4EEFF)
                            )
                            Text(
                                text = "Pustaka Visual Novel Interaktif",
                                fontSize = 11.sp,
                                color = Color(0xFFB388FF)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xF00F091E)
                ),
                actions = {
                    // Tombol Pintasan ke Global Setting (Roda Gigi)
                    IconButton(
                        onClick = onNavigateToGlobalSettings,
                        modifier = Modifier.testTag("topbar_global_setting_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Global Setting",
                            tint = Color(0xFFE2D7F5)
                        )
                    }

                    // Tombol Tambah Cerita Baru
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("topbar_add_story_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Buat Cerita Baru",
                            tint = Color(0xFFFFD54F)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF7A4BCF),
                contentColor = Color(0xFFFFD54F),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("fab_create_story")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buat Cerita",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (stories.isEmpty()) {
                // TAMPILAN KOSONG BERSIH (EMPTY STATE)
                EmptyStoriesView(
                    onCreateStoryClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // DAFTAR CERITA
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Cerita Anda",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFEDE7F6)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x337A4BCF)
                            ) {
                                Text(
                                    text = "${stories.size} Cerita",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFD54F),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    items(stories, key = { it.id }) { story ->
                        StoryCardItem(
                            story = story,
                            onClick = { onOpenStory(story.id) },
                            onEdit = { viewModel.openStoryForEditing(story.id) },
                            onDelete = { storyToDelete = story }
                        )
                    }
                }
            }
        }
    }

    // Dialog Buat Cerita Baru
    if (showCreateDialog) {
        CreateStoryDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, instruction ->
                viewModel.createNewStory(
                    title = title,
                    description = desc,
                    systemInstruction = instruction
                )
                showCreateDialog = false
            }
        )
    }

    // Modal Pengaturan & Editor Cerita Mandiri (Judul, System Instruction & Foto Sampul #1)
    if (uiState.isEditingStory) {
        StoryDetailEditorDialog(
            viewModel = viewModel,
            uiState = uiState,
            images = images,
            bgmTitle = bgmTitle,
            isBgmPlaying = isBgmPlaying,
            isBgmMuted = isBgmMuted,
            onDismiss = { viewModel.setEditingStory(false) }
        )
    }

    // Dialog Konfirmasi Hapus
    storyToDelete?.let { story ->
        AlertDialog(
            onDismissRequest = { storyToDelete = null },
            title = {
                Text(
                    text = "Hapus Cerita?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEDE7F6)
                )
            },
            text = {
                Text(
                    text = "Cerita \"${story.title}\" beserta seluruh obrolan dan gambar terkait akan dihapus secara permanen.",
                    color = Color(0xFFC7B9E5),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStory(story.id)
                        storyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToDelete = null }) {
                    Text("Batal", color = Color(0xFFC7B9E5))
                }
            },
            containerColor = Color(0xFF1E1435)
        )
    }
}

/**
 * Tampilan kosong saat belum ada cerita yang dibuat
 */
@Composable
private fun EmptyStoriesView(
    onCreateStoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0x227A4BCF),
            border = BorderStroke(1.dp, Color(0x44B388FF)),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Belum Ada Cerita",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFF3EFFF)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Aplikasi ini adalah wadah interaktif yang bersih dan siap diisi. Buat cerita visual novel pertamamu sekarang dengan alur, karakter, dan foto pilihanmu sendiri.",
            fontSize = 13.sp,
            color = Color(0xFFB9A9DC),
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateStoryClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7A4BCF),
                contentColor = Color(0xFFFFD54F)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("create_first_story_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Buat Cerita Pertama",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Item kartu cerita: menampilkan thumbnail (foto urutan #1 yang diunggah pengguna), judul, dan metadata.
 */
@Composable
private fun StoryCardItem(
    story: Story,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstImage = story.getFirstImage()
    val allImages = story.getAllImages()
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("story_card_${story.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0x3328154D)),
        border = BorderStroke(1.dp, Color(0x44B388FF)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ========================================================
            // IKON / THUMBNAIL CERITA (FOTO URUTAN PERTAMA #1)
            // ========================================================
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF381F66), Color(0xFF16092E))
                        )
                    )
            ) {
                if (!firstImage.isNullOrBlank()) {
                    val imageModel: Any = when {
                        firstImage.startsWith("http") -> firstImage
                        firstImage.startsWith("res:") -> firstImage
                        else -> File(firstImage)
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Thumbnail Cerita",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Badge penanda ikon utama / urutan 1
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                        color = Color(0xEE7A4BCF),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "★ #1",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                } else {
                    // Placeholder elegan jika pengguna belum mengunggah foto
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = Color(0x88FFD54F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tanpa Foto",
                            fontSize = 9.sp,
                            color = Color(0x88EDE7F6)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ========================================================
            // INFORMASI CERITA
            // ========================================================
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = story.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFFE082),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (story.description.isNotBlank()) story.description else "Klik untuk membuka panggung cerita...",
                    fontSize = 12.sp,
                    color = Color(0xFFC7B9E5),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Baris Info / Badge: Jumlah Foto & Model
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge Foto
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x22B388FF)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${allImages.size} Foto",
                                fontSize = 10.sp,
                                color = Color(0xFFEDE7F6),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Badge Model
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x227A4BCF)
                    ) {
                        Text(
                            text = story.selectedModel.removePrefix("gemini-"),
                            fontSize = 10.sp,
                            color = Color(0xFFD1C4E9),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // ========================================================
            // TOMBOL OPSI / MENU
            // ========================================================
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi Cerita",
                        tint = Color(0xFFC7B9E5),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E1435))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buka Cerita", color = Color(0xFFEDE7F6), fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pengaturan & Foto Cerita", color = Color(0xFFEDE7F6), fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hapus Cerita", color = Color(0xFFFF8A80), fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Dialog untuk membuat cerita baru dengan judul dan instruksi mandiri (pembeda cerita)
 */
@Composable
private fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, instruction: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf(StoryPromptConfig.DEFAULT_SYSTEM_INSTRUCTION) }
    var showInstructionDetail by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buat Cerita Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFFEDE7F6)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tiap cerita adalah wadah mandiri dengan System Instruction dan galeri fotonya sendiri.",
                    fontSize = 12.sp,
                    color = Color(0xFFC7B9E5)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Cerita *") },
                    placeholder = { Text("contoh: Petualangan Kastil Kuno") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_story_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFB388FF),
                        unfocusedBorderColor = Color(0x66B388FF),
                        focusedLabelColor = Color(0xFFFFD54F)
                    )
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi Cerita (Opsional)") },
                    placeholder = { Text("contoh: Percakapan interaktif tentang...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFB388FF),
                        unfocusedBorderColor = Color(0x66B388FF)
                    )
                )

                // System Instruction (Pembeda Cerita)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Instruction Khusus Cerita Ini:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEDE7F6)
                    )
                    TextButton(onClick = { showInstructionDetail = !showInstructionDetail }) {
                        Text(
                            text = if (showInstructionDetail) "Sembunyikan" else "Sesuaikan",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }

                if (showInstructionDetail) {
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = { instruction = it },
                        label = { Text("Instruksi Karakter Cerita Ini") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFB388FF),
                            unfocusedBorderColor = Color(0x66B388FF)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), desc.trim(), instruction.trim())
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7A4BCF),
                    contentColor = Color(0xFFFFD54F)
                ),
                modifier = Modifier.testTag("submit_create_story_button")
            ) {
                Text("Buat & Buka Cerita", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color(0xFFC7B9E5))
            }
        },
        containerColor = Color(0xFF1E1435)
    )
}


