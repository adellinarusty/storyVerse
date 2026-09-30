package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.ApiKeyManager
import com.example.config.StoryPromptConfig
import com.example.data.api.GeminiApiService
import com.example.data.db.Story
import com.example.data.db.StoryDatabase
import com.example.data.db.StoryMessage
import com.example.data.image.ImageGenerationService
import com.example.data.image.ImageProvider
import com.example.data.repository.StoryRepository
import com.example.media.BgmTrack
import com.example.media.MediaManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    STORY_LIST,
    STORY_DETAIL
}

enum class MainScreenTab(val title: String) {
    STORIES("Cerita"),
    GLOBAL_SETTINGS("Global Setting")
}

data class StoryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentScreen: AppScreen = AppScreen.STORY_LIST,
    val mainTab: MainScreenTab = MainScreenTab.STORIES,
    val activeStoryId: Long = 0L,
    val activeStoryTitle: String = "Panggung Cerita",
    // System Instruction khusus cerita aktif (pembeda cerita)
    val systemInstruction: String = StoryPromptConfig.DEFAULT_SYSTEM_INSTRUCTION,
    
    // Pengaturan Global
    val customApiKey: String = "",
    val selectedModel: String = StoryPromptConfig.DEFAULT_MODEL,
    val availableModels: List<String> = StoryPromptConfig.AVAILABLE_MODELS,
    val isFetchingModels: Boolean = false,
    val temperature: Float = StoryPromptConfig.DEFAULT_TEMPERATURE,
    val topP: Float = StoryPromptConfig.DEFAULT_TOP_P,
    val topK: Int = StoryPromptConfig.DEFAULT_TOP_K,
    val maxOutputTokens: Int = StoryPromptConfig.DEFAULT_MAX_OUTPUT_TOKENS,
    val imageProvider: ImageProvider = ImageProvider.POLLINATIONS,
    val hfToken: String = "",
    
    // Status visual & dialogs
    val isEditingStory: Boolean = false,
    val currentImageIndex: Int = 0,
    val generatingImageMessageIds: Set<Long> = emptySet(),
    val showStoryManagerDialog: Boolean = false,
    val showCreateStoryDialog: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("storyverse_config_v7", Context.MODE_PRIVATE)

    private val database = StoryDatabase.getDatabase(application)
    private val apiService = GeminiApiService.create()
    val repository = StoryRepository(database.storyDao(), apiService)
    val mediaManager = MediaManager(application)
    val imageService = ImageGenerationService(application)

    // Daftar semua cerita di database
    val allStories: StateFlow<List<Story>> = repository.allStoriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeStoryId = MutableStateFlow(0L)
    val activeStoryId: StateFlow<Long> = _activeStoryId.asStateFlow()

    // Pesan-pesan yang reaktif terhadap activeStoryId
    val storyMessages: StateFlow<List<StoryMessage>> = _activeStoryId
        .flatMapLatest { id ->
            if (id > 0) repository.getMessagesForStoryFlow(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mediaImages: StateFlow<List<String>> = mediaManager.imagesList
    val bgmPlaylist: StateFlow<List<BgmTrack>> = mediaManager.bgmPlaylist
    val currentTrackIndex: StateFlow<Int> = mediaManager.currentTrackIndex
    val bgmTitle: StateFlow<String> = mediaManager.bgmTitle
    val isBgmPlaying: StateFlow<Boolean> = mediaManager.isBgmPlaying
    val isBgmMuted: StateFlow<Boolean> = mediaManager.isBgmMuted

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private var slideshowJob: Job? = null

    init {
        loadSavedGlobalSettings()
        initStories()
        startSlideshowTimer()
        fetchRemoteModels()
    }

    fun fetchRemoteModels() {
        if (_uiState.value.isFetchingModels) return
        _uiState.value = _uiState.value.copy(isFetchingModels = true)
        viewModelScope.launch {
            val result = repository.fetchAvailableModels(_uiState.value.customApiKey)
            result.onSuccess { models ->
                val combined = (listOf(StoryPromptConfig.DEFAULT_MODEL) + models).distinct()
                _uiState.value = _uiState.value.copy(
                    availableModels = combined,
                    isFetchingModels = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isFetchingModels = false
                )
            }
        }
    }

    private fun loadSavedGlobalSettings() {
        val savedKey = prefs.getString("custom_api_key", "") ?: ""
        val model = prefs.getString("global_model", StoryPromptConfig.DEFAULT_MODEL) ?: StoryPromptConfig.DEFAULT_MODEL
        val temp = prefs.getFloat("global_temperature", StoryPromptConfig.DEFAULT_TEMPERATURE)
        val topP = prefs.getFloat("global_top_p", StoryPromptConfig.DEFAULT_TOP_P)
        val topK = prefs.getInt("global_top_k", StoryPromptConfig.DEFAULT_TOP_K)
        val maxTokens = prefs.getInt("global_max_tokens", StoryPromptConfig.DEFAULT_MAX_OUTPUT_TOKENS)
        val providerStr = prefs.getString("global_image_provider", ImageProvider.POLLINATIONS.name) ?: ImageProvider.POLLINATIONS.name
        val provider = runCatching { ImageProvider.valueOf(providerStr) }.getOrDefault(ImageProvider.POLLINATIONS)
        val hfToken = prefs.getString("global_hf_token", "") ?: ""

        _uiState.value = _uiState.value.copy(
            customApiKey = savedKey,
            selectedModel = model,
            temperature = temp,
            topP = topP,
            topK = topK,
            maxOutputTokens = maxTokens,
            imageProvider = provider,
            hfToken = hfToken
        )
    }

    fun saveGlobalSettings(
        apiKey: String,
        model: String,
        temperature: Float,
        topP: Float,
        topK: Int,
        maxTokens: Int,
        imageProvider: ImageProvider,
        hfToken: String
    ) {
        prefs.edit()
            .putString("custom_api_key", apiKey.trim())
            .putString("global_model", model)
            .putFloat("global_temperature", temperature)
            .putFloat("global_top_p", topP)
            .putInt("global_top_k", topK)
            .putInt("global_max_tokens", maxTokens)
            .putString("global_image_provider", imageProvider.name)
            .putString("global_hf_token", hfToken.trim())
            .apply()

        _uiState.value = _uiState.value.copy(
            customApiKey = apiKey.trim(),
            selectedModel = model,
            temperature = temperature,
            topP = topP,
            topK = topK,
            maxOutputTokens = maxTokens,
            imageProvider = imageProvider,
            hfToken = hfToken.trim()
        )
        fetchRemoteModels()
    }

    private fun initStories() {
        _activeStoryId.value = 0L
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.STORY_LIST,
            mainTab = MainScreenTab.STORIES,
            activeStoryId = 0L
        )
    }

    fun setMainTab(tab: MainScreenTab) {
        _uiState.value = _uiState.value.copy(mainTab = tab)
    }

    /**
     * Membuka cerita tertentu dari daftar cerita dan beralih ke Mode Chat bersih
     */
    fun openStory(storyId: Long) {
        viewModelScope.launch {
            val story = repository.getStoryById(storyId) ?: return@launch
            _activeStoryId.value = story.id
            prefs.edit().putLong("last_active_story_id", story.id).apply()

            // Muat media khusus cerita ini
            mediaManager.loadMediaForStory(story.customImagesJson, story.customBgmsJson)

            _uiState.value = _uiState.value.copy(
                currentScreen = AppScreen.STORY_DETAIL,
                activeStoryId = story.id,
                activeStoryTitle = story.title,
                systemInstruction = story.systemInstruction,
                currentImageIndex = 0,
                isEditingStory = false
            )

            // Putar audio cerita jika ada
            mediaManager.playBgm()
        }
    }

    fun switchToStory(storyId: Long) = openStory(storyId)

    /**
     * Kembali dari mode chat ke layar utama
     */
    fun closeStoryToHome() {
        mediaManager.pauseBgm()
        _activeStoryId.value = 0L
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.STORY_LIST,
            activeStoryId = 0L,
            isEditingStory = false
        )
    }

    /**
     * Membuat cerita baru dengan wadah bersih
     */
    fun createNewStory(
        title: String,
        description: String,
        systemInstruction: String
    ) {
        viewModelScope.launch {
            val cleanTitle = title.trim().ifBlank { "Cerita Baru" }
            val instruction = systemInstruction.trim().ifBlank { StoryPromptConfig.DEFAULT_SYSTEM_INSTRUCTION }

            val newStory = Story(
                title = cleanTitle,
                description = description.trim(),
                systemInstruction = instruction,
                selectedModel = _uiState.value.selectedModel,
                temperature = _uiState.value.temperature,
                topP = _uiState.value.topP,
                topK = _uiState.value.topK,
                maxOutputTokens = _uiState.value.maxOutputTokens,
                imageProvider = _uiState.value.imageProvider.name,
                hfToken = _uiState.value.hfToken,
                customImagesJson = "[]",
                customBgmsJson = "[]"
            )
            val newId = repository.insertStory(newStory)
            openStory(newId)

            val created = repository.getStoryById(newId) ?: newStory.copy(id = newId)
            startOpeningForStory(created)
        }
    }

    fun updateStoryMetadata(storyId: Long, newTitle: String, newDescription: String) {
        viewModelScope.launch {
            val story = repository.getStoryById(storyId) ?: return@launch
            val updated = story.copy(
                title = newTitle.trim().ifBlank { story.title },
                description = newDescription.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateStory(updated)
            if (_activeStoryId.value == storyId) {
                _uiState.value = _uiState.value.copy(activeStoryTitle = updated.title)
            }
        }
    }

    /**
     * Membuka pengaturan & editor cerita tertentu (judul, instruksi, dan foto sampul)
     */
    fun openStoryForEditing(storyId: Long) {
        viewModelScope.launch {
            val story = repository.getStoryById(storyId) ?: return@launch
            _activeStoryId.value = story.id
            mediaManager.loadMediaForStory(story.customImagesJson, story.customBgmsJson)
            _uiState.value = _uiState.value.copy(
                activeStoryId = story.id,
                activeStoryTitle = story.title,
                systemInstruction = story.systemInstruction,
                isEditingStory = true
            )
        }
    }

    /**
     * Memperbarui detail cerita aktif (judul, deskripsi, dan system instruction unik)
     */
    fun updateActiveStoryDetails(newTitle: String, newDescription: String, newInstruction: String) {
        val storyId = _activeStoryId.value
        if (storyId <= 0) return
        val instruction = newInstruction.trim().ifBlank { StoryPromptConfig.DEFAULT_SYSTEM_INSTRUCTION }
        val title = newTitle.trim().ifBlank { _uiState.value.activeStoryTitle }
        val desc = newDescription.trim()
        _uiState.value = _uiState.value.copy(
            activeStoryTitle = title,
            systemInstruction = instruction
        )
        viewModelScope.launch {
            val current = repository.getStoryById(storyId) ?: return@launch
            repository.updateStory(current.copy(
                title = title,
                description = if (desc.isNotBlank()) desc else current.description,
                systemInstruction = instruction,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun updateActiveStoryInstruction(newInstruction: String) {
        val storyId = _activeStoryId.value
        if (storyId <= 0) return
        val instruction = newInstruction.trim().ifBlank { StoryPromptConfig.DEFAULT_SYSTEM_INSTRUCTION }
        _uiState.value = _uiState.value.copy(systemInstruction = instruction)
        viewModelScope.launch {
            val current = repository.getStoryById(storyId) ?: return@launch
            repository.updateStory(current.copy(
                systemInstruction = instruction,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun setEditingStory(isEditing: Boolean) {
        _uiState.value = _uiState.value.copy(isEditingStory = isEditing)
    }

    fun deleteStory(storyId: Long) {
        viewModelScope.launch {
            repository.deleteStory(storyId)
            if (_activeStoryId.value == storyId) {
                closeStoryToHome()
            }
        }
    }

    fun startOpeningForStory(story: Story) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.startNewSessionWithOpening(
                storyId = story.id,
                systemInstruction = story.systemInstruction,
                modelName = _uiState.value.selectedModel,
                customApiKey = _uiState.value.customApiKey,
                temperature = _uiState.value.temperature,
                topP = _uiState.value.topP,
                topK = _uiState.value.topK,
                maxOutputTokens = _uiState.value.maxOutputTokens
            )
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.localizedMessage ?: "Gagal membangkitkan respon pembuka cerita."
                )
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun startSlideshowTimer() {
        slideshowJob?.cancel()
        slideshowJob = viewModelScope.launch {
            while (true) {
                delay(12000L)
                val images = mediaImages.value
                if (images.isNotEmpty()) {
                    val nextIndex = (_uiState.value.currentImageIndex + 1) % images.size
                    _uiState.value = _uiState.value.copy(currentImageIndex = nextIndex)
                }
            }
        }
    }

    fun setCreateStoryDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showCreateStoryDialog = visible)
    }

    fun setStoryManagerDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showStoryManagerDialog = visible)
    }

    fun onResumeScreen() {
        if (_uiState.value.currentScreen == AppScreen.STORY_DETAIL && !_uiState.value.isEditingStory) {
            mediaManager.playBgm()
        }
    }

    fun onPauseScreen() {
        mediaManager.pauseBgm()
    }

    /**
     * Kirim input user ke model pada cerita aktif.
     */
    fun sendUserAction(userInput: String) {
        val storyId = _activeStoryId.value
        if (userInput.isBlank() || _uiState.value.isLoading || storyId <= 0) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val state = _uiState.value
            val result = repository.sendUserAction(
                storyId = storyId,
                userInput = userInput,
                systemInstruction = state.systemInstruction,
                modelName = state.selectedModel,
                customApiKey = state.customApiKey,
                temperature = state.temperature,
                topP = state.topP,
                topK = state.topK,
                maxOutputTokens = state.maxOutputTokens
            )
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.localizedMessage ?: "Gagal memproses pesan."
                )
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    /**
     * Tombol Regenerate pada respon AI terakhir di cerita aktif.
     */
    fun regenerateLastResponse() {
        val storyId = _activeStoryId.value
        if (_uiState.value.isLoading || storyId <= 0) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val state = _uiState.value
            val result = repository.regenerateLastResponse(
                storyId = storyId,
                systemInstruction = state.systemInstruction,
                modelName = state.selectedModel,
                customApiKey = state.customApiKey,
                temperature = state.temperature,
                topP = state.topP,
                topK = state.topK,
                maxOutputTokens = state.maxOutputTokens
            )
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.localizedMessage ?: "Gagal meregenerasi respon AI."
                )
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    /**
     * Edit Pesan User di cerita aktif.
     */
    fun editUserMessage(messageId: Long, newContent: String) {
        val storyId = _activeStoryId.value
        if (newContent.isBlank() || _uiState.value.isLoading || storyId <= 0) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val state = _uiState.value
            val result = repository.editUserActionAndRegenerate(
                storyId = storyId,
                messageId = messageId,
                newContent = newContent,
                systemInstruction = state.systemInstruction,
                modelName = state.selectedModel,
                customApiKey = state.customApiKey,
                temperature = state.temperature,
                topP = state.topP,
                topK = state.topK,
                maxOutputTokens = state.maxOutputTokens
            )
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.localizedMessage ?: "Gagal memperbarui pesan."
                )
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    /**
     * Fitur Generate Gambar Manual per Pesan Model.
     */
    fun generateImageForMessage(messageId: Long, rawText: String) {
        if (_uiState.value.generatingImageMessageIds.contains(messageId)) return

        _uiState.value = _uiState.value.copy(
            generatingImageMessageIds = _uiState.value.generatingImageMessageIds + messageId
        )

        viewModelScope.launch {
            val state = _uiState.value
            val apiKey = ApiKeyManager.getNextApiKey(state.customApiKey)
            val result = imageService.generateImage(
                messageId = messageId,
                rawText = rawText,
                provider = state.imageProvider,
                geminiApiKey = apiKey,
                hfToken = state.hfToken
            )

            result.onSuccess { imagePathOrUrl ->
                repository.updateMessageImageUrl(messageId, imagePathOrUrl)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Gagal membuat gambar (${state.imageProvider.title}): ${error.localizedMessage}"
                )
            }

            _uiState.value = _uiState.value.copy(
                generatingImageMessageIds = _uiState.value.generatingImageMessageIds - messageId
            )
        }
    }

    fun addGeneratedImageToBackground(imagePath: String) {
        mediaManager.addImagePath(imagePath)
        syncMediaToActiveStory()
    }

    fun restartActiveStory() {
        viewModelScope.launch {
            val currentStory = repository.getStoryById(_activeStoryId.value) ?: return@launch
            startOpeningForStory(currentStory)
        }
    }

    private fun syncMediaToActiveStory() {
        viewModelScope.launch {
            val storyId = _activeStoryId.value
            if (storyId <= 0) return@launch
            val current = repository.getStoryById(storyId) ?: return@launch
            val updated = current.copy(
                customImagesJson = mediaManager.exportImagesJson(),
                customBgmsJson = mediaManager.exportBgmsJson(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateStory(updated)
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // === MEDIA GAMBAR & PENGURUTAN (REORDERING) ===

    fun addImages(uris: List<Uri>) {
        mediaManager.addImagesFromUris(uris)
        syncMediaToActiveStory()
    }

    fun removeImage(path: String) {
        mediaManager.removeImage(path)
        syncMediaToActiveStory()
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        mediaManager.moveImage(fromIndex, toIndex)
        syncMediaToActiveStory()
    }

    fun moveImageToFirst(index: Int) {
        mediaManager.moveImageToFirst(index)
        syncMediaToActiveStory()
    }

    fun resetDefaultImages() {
        mediaManager.resetDefaultImages()
        syncMediaToActiveStory()
    }

    // === MEDIA PLAYLIST MUSIK ===

    fun addBgmTracks(tracks: List<Pair<Uri, String>>) {
        mediaManager.addBgmTracksFromUris(tracks)
        syncMediaToActiveStory()
    }

    fun removeBgmTrack(trackId: String) {
        mediaManager.removeBgmTrack(trackId)
        syncMediaToActiveStory()
    }

    fun resetBgmPlaylist() {
        mediaManager.resetBgmPlaylist()
        syncMediaToActiveStory()
    }

    fun playNextBgm() {
        mediaManager.playNextTrack()
    }

    fun playPreviousBgm() {
        mediaManager.playPreviousTrack()
    }

    fun selectBgmTrack(index: Int) {
        mediaManager.selectTrack(index)
    }

    fun toggleBgmMute() {
        mediaManager.toggleMute()
    }

    override fun onCleared() {
        super.onCleared()
        slideshowJob?.cancel()
        mediaManager.release()
    }
}
