package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class BgmTrack(
    val id: String,
    val title: String,
    val path: String
)

class MediaManager(private val context: Context) {

    private val _imagesList = MutableStateFlow<List<String>>(emptyList())
    val imagesList: StateFlow<List<String>> = _imagesList.asStateFlow()

    private val _bgmPlaylist = MutableStateFlow<List<BgmTrack>>(emptyList())
    val bgmPlaylist: StateFlow<List<BgmTrack>> = _bgmPlaylist.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _bgmTitle = MutableStateFlow("Alunan Malam Lembut")
    val bgmTitle: StateFlow<String> = _bgmTitle.asStateFlow()

    private val _isBgmPlaying = MutableStateFlow(false)
    val isBgmPlaying: StateFlow<Boolean> = _isBgmPlaying.asStateFlow()

    private val _isBgmMuted = MutableStateFlow(false)
    val isBgmMuted: StateFlow<Boolean> = _isBgmMuted.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        loadDefaultMedia()
    }

    private fun getOrCreateDefaultBgmFile(): File {
        val file = File(context.filesDir, "default_ambient_bgm.wav")
        if (file.exists() && file.length() > 2000) return file

        try {
            val sampleRate = 22050
            val durationSeconds = 6
            val numSamples = sampleRate * durationSeconds
            val pcmData = ShortArray(numSamples)

            val chordFreqs = doubleArrayOf(220.0, 261.63, 329.63, 392.0)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = Math.sin(Math.PI * (i.toDouble() / numSamples))
                var sample = 0.0
                for ((idx, freq) in chordFreqs.withIndex()) {
                    val mod = 1.0 + 0.15 * Math.sin(2.0 * Math.PI * 0.5 * t + idx)
                    sample += Math.sin(2.0 * Math.PI * freq * t) * mod
                }
                sample = (sample / chordFreqs.size) * envelope * 0.35 * Short.MAX_VALUE
                pcmData[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            FileOutputStream(file).use { out ->
                val totalAudioLen = (numSamples * 2).toLong()
                val totalDataLen = totalAudioLen + 36
                val byteRate = (sampleRate * 2).toLong()

                out.write("RIFF".toByteArray())
                out.write(intToByteArray(totalDataLen.toInt()))
                out.write("WAVE".toByteArray())
                out.write("fmt ".toByteArray())
                out.write(intToByteArray(16))
                out.write(shortToByteArray(1.toShort()))
                out.write(shortToByteArray(1.toShort()))
                out.write(intToByteArray(sampleRate))
                out.write(intToByteArray(byteRate.toInt()))
                out.write(shortToByteArray(2.toShort()))
                out.write(shortToByteArray(16.toShort()))
                out.write("data".toByteArray())
                out.write(intToByteArray(totalAudioLen.toInt()))

                val byteBuf = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in pcmData) {
                    byteBuf.putShort(s)
                }
                out.write(byteBuf.array())
            }
        } catch (e: Exception) {
            Log.e("MediaManager", "Gagal membuat default WAV BGM", e)
        }
        return file
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xff).toByte(),
            ((value.toInt() shr 8) and 0xff).toByte()
        )
    }

    /**
     * Memuat media default jika cerita belum memiliki konfigurasi khusus
     */
    private fun loadDefaultMedia() {
        val defaultAudio = getOrCreateDefaultBgmFile()
        val defaultPlaylist = listOf(
            BgmTrack(
                id = "default_1",
                title = "Alunan Malam Lembut",
                path = defaultAudio.absolutePath
            )
        )
        _imagesList.value = emptyList()
        _bgmPlaylist.value = defaultPlaylist
        _bgmTitle.value = defaultPlaylist.first().title
    }

    /**
     * Sinkronisasi data media dari Cerita yang sedang aktif
     */
    fun loadMediaForStory(imagesJson: String?, bgmsJson: String?) {
        // 1. Gambar
        val images = mutableListOf<String>()
        if (!imagesJson.isNullOrBlank() && imagesJson != "[]") {
            try {
                val array = JSONArray(imagesJson)
                for (i in 0 until array.length()) {
                    images.add(array.getString(i))
                }
            } catch (_: Exception) {}
        }
        _imagesList.value = images

        // 2. Playlist BGM
        val playlist = mutableListOf<BgmTrack>()
        if (!bgmsJson.isNullOrBlank() && bgmsJson != "[]") {
            try {
                val array = JSONArray(bgmsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    playlist.add(
                        BgmTrack(
                            id = obj.optString("id", System.currentTimeMillis().toString()),
                            title = obj.optString("title", "Musik Latar"),
                            path = obj.optString("path", getOrCreateDefaultBgmFile().absolutePath)
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (playlist.isEmpty()) {
            playlist.add(
                BgmTrack(
                    id = "default_1",
                    title = "Alunan Malam Lembut",
                    path = getOrCreateDefaultBgmFile().absolutePath
                )
            )
        }
        _bgmPlaylist.value = playlist
        _currentTrackIndex.value = 0
        _bgmTitle.value = playlist[0].title

        // Reset dan mulai player dengan playlist cerita ini
        stopPlayer()
        setupPlayerForCurrentTrack()
    }

    fun exportImagesJson(): String {
        val array = JSONArray()
        _imagesList.value.forEach { array.put(it) }
        return array.toString()
    }

    fun exportBgmsJson(): String {
        val array = JSONArray()
        _bgmPlaylist.value.forEach { track ->
            val obj = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("path", track.path)
            }
            array.put(obj)
        }
        return array.toString()
    }

    // === OPERASI GAMBAR & REORDERING ===

    fun addImagesFromUris(uris: List<Uri>) {
        val current = _imagesList.value.toMutableList()
        val dir = File(context.filesDir, "story_images").apply { mkdirs() }

        for (uri in uris) {
            try {
                val filename = "slide_${System.currentTimeMillis()}_${(100..999).random()}.jpg"
                val destFile = File(dir, filename)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                current.add(destFile.absolutePath)
            } catch (e: Exception) {
                Log.e("MediaManager", "Gagal menyalin gambar dari uri: $uri", e)
            }
        }
        _imagesList.value = current
    }

    fun addImagePath(path: String) {
        val current = _imagesList.value.toMutableList()
        if (!current.contains(path)) {
            current.add(0, path)
            _imagesList.value = current
        }
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        val current = _imagesList.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _imagesList.value = current
        }
    }

    fun moveImageToFirst(index: Int) {
        moveImage(index, 0)
    }

    fun removeImage(imagePath: String) {
        val current = _imagesList.value.toMutableList()
        current.remove(imagePath)
        if (!imagePath.startsWith("res:")) {
            runCatching { File(imagePath).delete() }
        }
        _imagesList.value = current
    }

    fun resetDefaultImages() {
        _imagesList.value = emptyList()
    }

    // === OPERASI PLAYLIST BGM MULTIPLE BERULANG ===

    /**
     * Menambahkan banyak lagu sekaligus ke daftar putar cerita
     */
    fun addBgmTracksFromUris(items: List<Pair<Uri, String>>) {
        val current = _bgmPlaylist.value.toMutableList()
        val dir = File(context.filesDir, "story_bgms").apply { mkdirs() }

        for ((uri, originalName) in items) {
            try {
                val cleanName = originalName.substringBeforeLast(".")
                val safeFilename = "bgm_${System.currentTimeMillis()}_${(100..999).random()}.mp3"
                val destFile = File(dir, safeFilename)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                current.add(
                    BgmTrack(
                        id = "track_${System.currentTimeMillis()}_${(1000..9999).random()}",
                        title = cleanName.ifBlank { "Musik Pengguna" },
                        path = destFile.absolutePath
                    )
                )
            } catch (e: Exception) {
                Log.e("MediaManager", "Gagal menyalin audio: $uri", e)
            }
        }

        _bgmPlaylist.value = current
        if (!_isBgmPlaying.value && current.isNotEmpty()) {
            setupPlayerForCurrentTrack()
        }
    }

    fun removeBgmTrack(trackId: String) {
        val current = _bgmPlaylist.value.toMutableList()
        val trackToRemove = current.find { it.id == trackId } ?: return
        val wasPlaying = (current.getOrNull(_currentTrackIndex.value)?.id == trackId)

        current.remove(trackToRemove)
        if (!trackToRemove.path.startsWith("res:")) {
            runCatching { File(trackToRemove.path).delete() }
        }

        if (current.isEmpty()) {
            current.add(
                BgmTrack(
                    id = "default_1",
                    title = "Alunan Malam Lembut",
                    path = getOrCreateDefaultBgmFile().absolutePath
                )
            )
        }

        _bgmPlaylist.value = current
        _currentTrackIndex.value = _currentTrackIndex.value.coerceIn(0, current.size - 1)
        _bgmTitle.value = current[_currentTrackIndex.value].title

        if (wasPlaying) {
            stopPlayer()
            setupPlayerForCurrentTrack()
            playBgm()
        }
    }

    fun resetBgmPlaylist() {
        stopPlayer()
        val defaultList = listOf(
            BgmTrack(
                id = "default_1",
                title = "Alunan Malam Lembut",
                path = getOrCreateDefaultBgmFile().absolutePath
            )
        )
        _bgmPlaylist.value = defaultList
        _currentTrackIndex.value = 0
        _bgmTitle.value = defaultList[0].title
        setupPlayerForCurrentTrack()
    }

    private fun setupPlayerForCurrentTrack() {
        val playlist = _bgmPlaylist.value
        if (playlist.isEmpty()) return

        val index = _currentTrackIndex.value.coerceIn(0, playlist.size - 1)
        val track = playlist[index]
        _bgmTitle.value = track.title

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                val file = File(track.path)
                if (file.exists() && file.length() > 0) {
                    setDataSource(track.path)
                } else {
                    val defaultFile = getOrCreateDefaultBgmFile()
                    setDataSource(defaultFile.absolutePath)
                }

                // Saat sebuah lagu selesai, langsung putar lagu berikutnya di playlist (Loop playlist kontinu)
                setOnCompletionListener {
                    playNextTrack()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("MediaManager", "MediaPlayer error: what=$what extra=$extra")
                    playNextTrack()
                    true
                }

                prepare()
                val vol = if (_isBgmMuted.value) 0f else 0.45f
                setVolume(vol, vol)
            }
        } catch (e: Exception) {
            Log.e("MediaManager", "Gagal inisialisasi media player untuk ${track.title}", e)
        }
    }

    fun playBgm() {
        val player = mediaPlayer ?: run {
            setupPlayerForCurrentTrack()
            mediaPlayer
        } ?: return

        try {
            if (!player.isPlaying) {
                player.start()
            }
            _isBgmPlaying.value = true
        } catch (e: Exception) {
            Log.e("MediaManager", "Gagal memulai pemutaran lagu", e)
        }
    }

    fun pauseBgm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        } catch (_: Exception) {}
        _isBgmPlaying.value = false
    }

    fun playNextTrack() {
        val playlist = _bgmPlaylist.value
        if (playlist.isEmpty()) return
        val wasPlaying = _isBgmPlaying.value

        stopPlayer()
        _currentTrackIndex.value = (_currentTrackIndex.value + 1) % playlist.size
        setupPlayerForCurrentTrack()
        if (wasPlaying) {
            playBgm()
        }
    }

    fun playPreviousTrack() {
        val playlist = _bgmPlaylist.value
        if (playlist.isEmpty()) return
        val wasPlaying = _isBgmPlaying.value

        stopPlayer()
        val nextIdx = _currentTrackIndex.value - 1
        _currentTrackIndex.value = if (nextIdx < 0) playlist.size - 1 else nextIdx
        setupPlayerForCurrentTrack()
        if (wasPlaying) {
            playBgm()
        }
    }

    fun selectTrack(index: Int) {
        val playlist = _bgmPlaylist.value
        if (index !in playlist.indices) return

        stopPlayer()
        _currentTrackIndex.value = index
        setupPlayerForCurrentTrack()
        playBgm()
    }

    fun toggleMute() {
        val newMuted = !_isBgmMuted.value
        _isBgmMuted.value = newMuted
        val vol = if (newMuted) 0f else 0.45f
        mediaPlayer?.setVolume(vol, vol)
    }

    private fun stopPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isBgmPlaying.value = false
    }

    fun release() {
        stopPlayer()
    }
}
