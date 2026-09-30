package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.config.StoryPromptConfig
import com.example.viewmodel.StoryUiState
import com.example.viewmodel.StoryViewModel

/**
 * Dialog Pengaturan & Editor Cerita Mandiri:
 * - Mengatur Judul, Deskripsi, dan System Instruction (pembeda cerita yang unik)
 * - Mengatur Galeri Foto (urutan foto, hold & drag ke #1 sebagai ikon sampul list & background)
 * - Mengatur Musik Latar
 */
@Composable
fun StoryDetailEditorDialog(
    viewModel: StoryViewModel,
    uiState: StoryUiState,
    images: List<String>,
    bgmTitle: String,
    isBgmPlaying: Boolean,
    isBgmMuted: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var titleText by remember(uiState.activeStoryTitle) {
        mutableStateOf(uiState.activeStoryTitle)
    }
    var instructionText by remember(uiState.systemInstruction) {
        mutableStateOf(uiState.systemInstruction)
    }
    var showSavedNotice by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF130D24)),
            border = BorderStroke(1.dp, Color(0x44B388FF)),
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .testTag("story_detail_editor_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pengaturan Cerita",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFFF3EFFF)
                        )
                        Text(
                            text = uiState.activeStoryTitle,
                            fontSize = 12.sp,
                            color = Color(0xFFFFD54F),
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("editor_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFFE2D7F5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Tab Bar Pengaturan Cerita
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0x337A4BCF),
                    contentColor = Color(0xFFFFD54F)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Instruksi & Info",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Foto (#1=Sampul) & BGM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (selectedTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Judul Cerita",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEDE7F6)
                            )
                            OutlinedTextField(
                                value = titleText,
                                onValueChange = {
                                    titleText = it
                                    showSavedNotice = false
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_story_title_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFB388FF),
                                    unfocusedBorderColor = Color(0x66B388FF),
                                    focusedTextColor = Color(0xFFEDE7F6),
                                    unfocusedTextColor = Color(0xFFEDE7F6)
                                )
                            )

                            Text(
                                text = "System Instruction Khusus Cerita Ini:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEDE7F6)
                            )
                            Text(
                                text = "Setiap cerita memiliki System Instruction mandiri sebagai pembeda identitas karakter, tata aturan dialog, dan plot.",
                                fontSize = 11.sp,
                                color = Color(0xFFC7B9E5)
                            )
                            OutlinedTextField(
                                value = instructionText,
                                onValueChange = {
                                    instructionText = it
                                    showSavedNotice = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .testTag("editor_system_instruction_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFB388FF),
                                    unfocusedBorderColor = Color(0x66B388FF),
                                    focusedTextColor = Color(0xFFEDE7F6),
                                    unfocusedTextColor = Color(0xFFEDE7F6)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateActiveStoryDetails(
                                            newTitle = titleText,
                                            newDescription = "",
                                            newInstruction = instructionText
                                        )
                                        showSavedNotice = true
                                    },
                                    enabled = titleText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF7A4BCF),
                                        contentColor = Color(0xFFFFD54F)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_story_details_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        instructionText = StoryPromptConfig.DEFAULT_SYSTEM_INSTRUCTION
                                        showSavedNotice = false
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD4C2F8))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Template", fontSize = 12.sp)
                                }
                            }

                            if (showSavedNotice) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF69F0AE),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Perubahan cerita berhasil disimpan!",
                                        fontSize = 12.sp,
                                        color = Color(0xFF69F0AE),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        // Tab 1: Galeri Foto (#1 = Sampul) & Musik Latar
                        MediaScreen(
                            viewModel = viewModel,
                            images = images,
                            bgmTitle = bgmTitle,
                            isBgmPlaying = isBgmPlaying,
                            isBgmMuted = isBgmMuted
                        )
                    }
                }
            }
        }
    }
}
