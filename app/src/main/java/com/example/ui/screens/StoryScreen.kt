package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.config.StoryPromptConfig
import com.example.data.db.Story
import com.example.data.db.StoryMessage
import com.example.ui.markdown.FormattedChatMessageText
import com.example.viewmodel.StoryUiState
import com.example.viewmodel.StoryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StoryScreen(
    viewModel: StoryViewModel,
    uiState: StoryUiState,
    messages: List<StoryMessage>,
    images: List<String>,
    bgmTitle: String,
    isBgmPlaying: Boolean,
    isBgmMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val allStories by viewModel.allStories.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var storyToEdit by remember { mutableStateOf<Story?>(null) }
    var storyToDelete by remember { mutableStateOf<Story?>(null) }
    var editingMessageId by remember { mutableStateOf<Long?>(null) }
    var editingMessageText by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var showStoryEditModal by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, uiState.isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Slideshow background (12 detik crossfade) menimpa seluruh layar
        SlideshowBackground(
            images = images,
            currentIndex = uiState.currentImageIndex,
            modifier = Modifier.fillMaxSize()
        )

        // Scrim gradien agar teks terbaca nyaman
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xA60A0612),
                            Color(0x8C0A0612),
                            Color(0xD9080410)
                        )
                    )
                )
        )

        // 2. Konten Chat / Cerita
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Header Top Bar
            StoryTopBar(
                activeStoryTitle = uiState.activeStoryTitle,
                bgmTitle = bgmTitle,
                isBgmPlaying = isBgmPlaying,
                isBgmMuted = isBgmMuted,
                onBackToHome = { viewModel.closeStoryToHome() },
                onEditStory = { showStoryEditModal = true },
                onOpenStoryManager = { viewModel.setStoryManagerDialogVisible(true) },
                onNewStory = { showCreateStoryDialog = true },
                onToggleMute = { viewModel.toggleBgmMute() },
                onResetStory = { showResetDialog = true }
            )

            // Feed Chat Kontinu (Bisa di-scroll dan dibaca selalu)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && !uiState.isLoading) {
                    EmptyChatPlaceholder(onStart = { viewModel.restartActiveStory() })
                } else {
                    ChatMessageStream(
                        messages = messages,
                        isLoading = uiState.isLoading,
                        listState = listState,
                        generatingImageMessageIds = uiState.generatingImageMessageIds,
                        onRegenerate = { viewModel.regenerateLastResponse() },
                        onEditUserMessage = { id, text ->
                            editingMessageId = id
                            editingMessageText = text
                        },
                        onGenerateImage = { id, text -> viewModel.generateImageForMessage(id, text) },
                        onImageClick = { url -> previewImageUrl = url },
                        onAddToBackground = { url -> viewModel.addGeneratedImageToBackground(url) }
                    )
                }
            }

            // Input Bar Pengguna di bagian bawah
            ChatBottomInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        viewModel.sendUserAction(text)
                    }
                },
                enabled = !uiState.isLoading
            )
        }

        // Pesan Error jika ada
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE4A1525)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFFFB4AB),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearErrorMessage() }) {
                        Text("Tutup", color = Color(0xFFFFDAD6))
                    }
                }
            }
        }
    }

    // Modal Kelola & Pilih Cerita (Multi-Story Manager)
    if (uiState.showStoryManagerDialog) {
        Dialog(onDismissRequest = { viewModel.setStoryManagerDialogVisible(false) }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF130D24)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daftar Cerita Anda",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF3EFFF)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setStoryManagerDialogVisible(false) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color(0xFFE2D7F5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.setStoryManagerDialogVisible(false)
                            showCreateStoryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4BCF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Buat Cerita Baru",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allStories, key = { it.id }) { story ->
                            val isActive = (story.id == uiState.activeStoryId)
                            val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                .format(Date(story.updatedAt))

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) Color(0x557A4BCF) else Color(0x33281A4C)
                                ),
                                border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.switchToStory(story.id)
                                        viewModel.setStoryManagerDialogVisible(false)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = story.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isActive) Color(0xFFFFD54F) else Color(0xFFF3EFFF),
                                                maxLines = 1
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF2E7D32)
                                                ) {
                                                    Text(
                                                        text = "Aktif",
                                                        fontSize = 10.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (story.description.isNotBlank()) {
                                            Text(
                                                text = story.description,
                                                fontSize = 11.sp,
                                                color = Color(0xFFC7B9E5),
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = "Diperbarui: $dateStr",
                                            fontSize = 10.sp,
                                            color = Color(0xFF9E8DBA)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { storyToEdit = story },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Judul",
                                                tint = Color(0xFFC7B9E5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { storyToDelete = story },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Cerita",
                                                tint = Color(0xFFFF8A80),
                                                modifier = Modifier.size(16.dp)
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
    }

    // Dialog Buat Cerita Baru
    if (showCreateStoryDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateStoryDialog = false },
            title = {
                Text(
                    text = "Buat Cerita Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Setiap cerita memiliki alur, memori obrolan, gambar, dan musik latarnya sendiri.",
                        fontSize = 12.sp,
                        color = Color(0xFFC7B9E5)
                    )

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("Judul Cerita Baru") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFB388FF),
                            unfocusedBorderColor = Color(0x66B388FF)
                        )
                    )

                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        placeholder = { Text("Deskripsi Singkat (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFB388FF),
                            unfocusedBorderColor = Color(0x66B388FF)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewStory(newTitle, newDesc, StoryPromptConfig.DEFAULT_SYSTEM_INSTRUCTION)
                        showCreateStoryDialog = false
                    },
                    enabled = newTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4BCF))
                ) {
                    Text("Buat Cerita")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateStoryDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Edit Metadata Cerita
    storyToEdit?.let { story ->
        var editTitle by remember(story.id) { mutableStateOf(story.title) }
        var editDesc by remember(story.id) { mutableStateOf(story.description) }

        AlertDialog(
            onDismissRequest = { storyToEdit = null },
            title = { Text("Edit Judul Cerita") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        placeholder = { Text("Judul Cerita") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        placeholder = { Text("Deskripsi Singkat") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStoryMetadata(story.id, editTitle, editDesc)
                        storyToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4BCF))
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Hapus Cerita
    storyToDelete?.let { story ->
        AlertDialog(
            onDismissRequest = { storyToDelete = null },
            title = { Text("Hapus Cerita?") },
            text = {
                Text("Cerita \"${story.title}\" beserta seluruh memori obrolan dan gambarnya akan dihapus secara permanen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStory(story.id)
                        storyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Reset Cerita Aktif
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Mulai Ulang Cerita Ini?") },
            text = { Text("Riwayat pada cerita ini akan dihapus dan chat pembuka baru akan dihasilkan sesuai System Instruction.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.restartActiveStory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4BCF))
                ) {
                    Text("Mulai Ulang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Edit Pesan User
    if (editingMessageId != null) {
        AlertDialog(
            onDismissRequest = { editingMessageId = null },
            title = {
                Text(
                    text = "Edit Pesan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Model akan merespons ulang pesan yang telah diedit dan menimpa respon lama.",
                        fontSize = 12.sp,
                        color = Color(0xFFC7B9E5)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editingMessageText,
                        onValueChange = { editingMessageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFB388FF),
                            unfocusedBorderColor = Color(0x66B388FF),
                            focusedTextColor = Color(0xFFFFFFFF),
                            unfocusedTextColor = Color(0xFFEDE7F6),
                            cursorColor = Color(0xFFFFD54F)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = editingMessageId
                        val newText = editingMessageText
                        editingMessageId = null
                        if (id != null && newText.isNotBlank()) {
                            viewModel.editUserMessage(id, newText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4BCF))
                ) {
                    Text("Kirim Ulang")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessageId = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Tinjau Gambar Penuh (Fullscreen Modal)
    if (previewImageUrl != null) {
        val currentUrl = previewImageUrl!!
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161124)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ilustrasi Adegan Cerita",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF3EFFF)
                        )
                        IconButton(
                            onClick = { previewImageUrl = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color(0xFFE2D7F5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AsyncImage(
                        model = if (currentUrl.startsWith("http")) currentUrl else File(currentUrl),
                        contentDescription = "Ilustrasi Adegan Penuh",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.addGeneratedImageToBackground(currentUrl)
                            previewImageUrl = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4BCF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Jadikan Latar Belakang Cerita",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // Modal Edit Pengaturan Cerita (System Instruction Mandiri & Media)
    if (showStoryEditModal) {
        StoryDetailEditorDialog(
            viewModel = viewModel,
            uiState = uiState,
            images = images,
            bgmTitle = bgmTitle,
            isBgmPlaying = isBgmPlaying,
            isBgmMuted = isBgmMuted,
            onDismiss = { showStoryEditModal = false }
        )
    }
}

@Composable
fun ChatMessageStream(
    messages: List<StoryMessage>,
    isLoading: Boolean,
    listState: LazyListState,
    generatingImageMessageIds: Set<Long>,
    onRegenerate: () -> Unit,
    onEditUserMessage: (Long, String) -> Unit,
    onGenerateImage: (Long, String) -> Unit,
    onImageClick: (String) -> Unit,
    onAddToBackground: (String) -> Unit
) {
    val lastModelIndex = messages.indexOfLast { it.role == "model" }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = messages,
            key = { _, item -> item.id }
        ) { index, msg ->
            val isUser = (msg.role == "user")
            val isLastModelResponse = (!isUser && index == lastModelIndex)
            val isGeneratingThisImage = generatingImageMessageIds.contains(msg.id)

            ChatMessageBubble(
                text = msg.content,
                imageUrl = msg.imageUrl,
                isUser = isUser,
                isLastModelResponse = isLastModelResponse,
                isLoading = isLoading,
                isGeneratingImage = isGeneratingThisImage,
                onRegenerate = onRegenerate,
                onEdit = { onEditUserMessage(msg.id, msg.content) },
                onGenerateImage = { onGenerateImage(msg.id, msg.content) },
                onImageClick = onImageClick,
                onAddToBackground = onAddToBackground
            )
        }

        if (isLoading) {
            item {
                LoadingBubble()
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    text: String,
    imageUrl: String?,
    isUser: Boolean,
    isLastModelResponse: Boolean,
    isLoading: Boolean,
    isGeneratingImage: Boolean,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onGenerateImage: () -> Unit,
    onImageClick: (String) -> Unit,
    onAddToBackground: (String) -> Unit
) {
    val bgColor = if (isUser) Color(0xDE2B1B4A) else Color(0xE6161124)
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.88f else 0.95f)
                .testTag(if (isUser) "user_message_bubble" else "model_message_bubble"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = null
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                FormattedChatMessageText(
                    text = text,
                    modifier = Modifier.fillMaxWidth(),
                    textColor = if (isUser) Color(0xFFF7F2FA) else Color(0xFFEFE9F5),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                // Kartu Gambar Adegan di bawah respon teks
                if (!isUser && !imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x44000000)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(imageUrl) }
                            .testTag("scene_image_card")
                    ) {
                        Column {
                            AsyncImage(
                                model = if (imageUrl.startsWith("http")) imageUrl else File(imageUrl),
                                contentDescription = "Ilustrasi Adegan",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ilustrasi Adegan",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC7B9E5)
                                )
                                TextButton(
                                    onClick = { onAddToBackground(imageUrl) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Collections,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFFFFD54F)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Jadikan Latar",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFD54F)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tombol aksi
                if (isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x33B388FF),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !isLoading) { onEdit() }
                                .testTag("edit_message_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Pesan",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x33B388FF),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !isGeneratingImage && !isLoading) { onGenerateImage() }
                                .testTag("generate_image_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isGeneratingImage) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = if (imageUrl == null) "Hasilkan Gambar Adegan" else "Buat Ulang Gambar",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        if (isLastModelResponse) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0x33B388FF),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !isLoading) { onRegenerate() }
                                    .testTag("regenerate_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(16.dp)
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
fun LoadingBubble() {
    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC161124)),
        border = null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = Color(0xFFFFD54F),
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Sedang merespons...",
                fontStyle = FontStyle.Italic,
                color = Color(0xFFC7B9E5),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ChatBottomInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        color = Color(0xE60D081B),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        "Ketik pesan atau tindakan...",
                        color = Color(0x88D4C2F8),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color(0xFFFFFFFF),
                    unfocusedTextColor = Color(0xFFF3EDF7),
                    cursorColor = Color(0xFFFFD54F)
                ),
                maxLines = 4,
                enabled = enabled
            )

            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank() && enabled) Color(0xFF7A4BCF) else Color(0x337A4BCF))
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    tint = if (value.isNotBlank() && enabled) Color(0xFFFFD54F) else Color(0x88FFFFFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StoryTopBar(
    activeStoryTitle: String,
    bgmTitle: String,
    isBgmPlaying: Boolean,
    isBgmMuted: Boolean,
    onBackToHome: () -> Unit,
    onEditStory: () -> Unit,
    onOpenStoryManager: () -> Unit,
    onNewStory: () -> Unit,
    onToggleMute: () -> Unit,
    onResetStory: () -> Unit
) {
    Surface(
        color = Color(0xB30C071A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tombol Kembali ke Daftar Cerita
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("story_back_to_list_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Daftar Cerita",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Pemilih Cerita Aktif (Clickable Dropdown Pill)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x3D9C74E8),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenStoryManager() }
                        .testTag("story_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeStoryTitle.take(16),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFF3EFFF),
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Cerita",
                            tint = Color(0xFFC7B9E5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tombol Pengaturan / Edit Cerita Ini (System Instruction & Media)
                IconButton(
                    onClick = onEditStory,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("story_topbar_edit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Pengaturan Cerita Ini",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Tombol BGM Indicator
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x33A084E8),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isBgmMuted) Icons.Default.MusicOff else Icons.Default.MusicNote,
                                contentDescription = "BGM",
                                tint = if (isBgmMuted) Color(0xFF9E9E9E) else Color(0xFFFFD54F),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (isBgmMuted) "Mute" else bgmTitle.take(10),
                            fontSize = 10.sp,
                            color = Color(0xFFE2D7F5),
                            maxLines = 1
                        )
                    }
                }

                // Tombol Reset Cerita Ini
                IconButton(
                    onClick = onResetStory,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Mulai Ulang",
                        tint = Color(0xFFC7B9E5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Tombol Tambah Cerita Baru
                IconButton(
                    onClick = onNewStory,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Buat Cerita Baru",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SlideshowBackground(
    images: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0717), Color(0xFF1E0F36), Color(0xFF0A0514))
                )
            )
        )
        return
    }

    val safeIndex = currentIndex % images.size
    val currentImage = images[safeIndex]

    Crossfade(
        targetState = currentImage,
        animationSpec = tween(durationMillis = 1500),
        label = "BackgroundSlideshowFade",
        modifier = modifier
    ) { imgPath ->
        when {
            imgPath == "res:bg_academy_sunset" -> {
                Image(
                    painter = painterResource(id = R.drawable.bg_academy_sunset),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            imgPath == "res:bg_mystic_library" -> {
                Image(
                    painter = painterResource(id = R.drawable.bg_mystic_library),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            imgPath.startsWith("http") -> {
                AsyncImage(
                    model = imgPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                AsyncImage(
                    model = File(imgPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun EmptyChatPlaceholder(onStart: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Panggung Cerita Siap Dimulai",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFFF3EFFF)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tekan tombol di bawah untuk menghasilkan adegan pembuka.",
                fontSize = 12.sp,
                color = Color(0xFFC7B9E5)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4BCF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Mulai Adegan Cerita")
            }
        }
    }
}
