package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.config.ApiKeyManager
import com.example.data.db.Story
import com.example.data.db.StoryDatabase
import com.example.data.db.StoryMessage
import com.example.data.image.ImageGenerationService
import com.example.media.MediaManager
import com.example.ui.markdown.VisualNovelTextRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StoryVerse", appName)
  }

  @Test
  fun `verify chat text parser handles hash symbols and markdown italics`() {
    val sampleText = "#Hujan mulai turun.# \"Kita harus bergegas!\" seru Elian."
    val parsed = VisualNovelTextRenderer.parseChatText(sampleText)
    assertTrue(parsed.text.contains("Hujan mulai turun."))
    assertTrue(parsed.text.contains("\"Kita harus bergegas!\""))
  }

  @Test
  fun `verify chat text parser strips scene art tags from reader view`() {
    val sampleText = "Karakter tersenyum tipis. [SCENE_ART: character in bedroom lighting]"
    val parsed = VisualNovelTextRenderer.parseChatText(sampleText)
    assertTrue(parsed.text.contains("Karakter tersenyum tipis."))
    assertFalse(parsed.text.contains("[SCENE_ART"))
  }

  @Test
  fun `verify image prompt extraction`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = ImageGenerationService(context)
    val textWithTag = "Karakter menatap cermin. [SCENE_ART: mysterious traveler looking at mirror, warm candle light]"
    val prompt = service.extractScenePrompt(textWithTag)
    assertTrue(prompt.contains("mysterious traveler looking at mirror"))
  }

  @Test
  fun `verify multi key parsing and rolling round robin`() {
    val rawKeys = """
      mock_gemini_key_one
      mock_gemini_key_two, mock_gemini_key_three
      
      mock_gemini_key_one
    """.trimIndent()
    val parsed = ApiKeyManager.parseKeys(rawKeys)
    assertEquals(3, parsed.size)
    assertEquals("mock_gemini_key_one", parsed[0])
    assertEquals("mock_gemini_key_two", parsed[1])
    assertEquals("mock_gemini_key_three", parsed[2])

    val key1 = ApiKeyManager.getNextApiKey(rawKeys)
    val key2 = ApiKeyManager.getNextApiKey(rawKeys)
    val key3 = ApiKeyManager.getNextApiKey(rawKeys)
    val key4 = ApiKeyManager.getNextApiKey(rawKeys)

    assertTrue(listOf("mock_gemini_key_one", "mock_gemini_key_two", "mock_gemini_key_three").contains(key1))
    assertTrue(listOf("mock_gemini_key_one", "mock_gemini_key_two", "mock_gemini_key_three").contains(key2))
    assertTrue(listOf("mock_gemini_key_one", "mock_gemini_key_two", "mock_gemini_key_three").contains(key3))
    assertEquals(key1, key4)
  }

  @Test
  fun `verify story isolation in database`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, StoryDatabase::class.java).build()
    val dao = db.storyDao()

    val story1Id = dao.insertStory(Story(title = "Cerita 1", systemInstruction = "Instruction 1"))
    val story2Id = dao.insertStory(Story(title = "Cerita 2", systemInstruction = "Instruction 2"))

    dao.insertMessage(StoryMessage(storyId = story1Id, role = "user", content = "Pesan Cerita 1"))
    dao.insertMessage(StoryMessage(storyId = story2Id, role = "user", content = "Pesan Cerita 2"))

    val msgs1 = dao.getMessagesForStory(story1Id)
    val msgs2 = dao.getMessagesForStory(story2Id)

    assertEquals(1, msgs1.size)
    assertEquals("Pesan Cerita 1", msgs1[0].content)

    assertEquals(1, msgs2.size)
    assertEquals("Pesan Cerita 2", msgs2[0].content)

    db.close()
  }

  @Test
  fun `verify first image extraction for story list icon`() {
    val storyWithoutImages = Story(title = "Cerita Baru", customImagesJson = "[]")
    assertNull(storyWithoutImages.getFirstImage())
    assertEquals(0, storyWithoutImages.getImagesCount())

    val storyWithImages = Story(
      title = "Cerita Kastil",
      customImagesJson = """["/data/image_one.jpg", "/data/image_two.jpg", "/data/image_three.jpg"]"""
    )
    assertEquals("/data/image_one.jpg", storyWithImages.getFirstImage())
    assertEquals(3, storyWithImages.getImagesCount())
    assertEquals(listOf("/data/image_one.jpg", "/data/image_two.jpg", "/data/image_three.jpg"), storyWithImages.getAllImages())
  }

  @Test
  fun `verify image reordering to position one in MediaManager`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val mediaManager = MediaManager(context)
    mediaManager.loadMediaForStory(
      imagesJson = """["/data/img_a.jpg", "/data/img_b.jpg", "/data/img_c.jpg"]""",
      bgmsJson = "[]"
    )

    assertEquals(3, mediaManager.imagesList.value.size)
    assertEquals("/data/img_a.jpg", mediaManager.imagesList.value[0])

    // Pindahkan foto di index 2 (/data/img_c.jpg) ke posisi pertama (#1)
    mediaManager.moveImageToFirst(2)
    assertEquals("/data/img_c.jpg", mediaManager.imagesList.value[0])
    assertEquals("/data/img_a.jpg", mediaManager.imagesList.value[1])
    assertEquals("/data/img_b.jpg", mediaManager.imagesList.value[2])
  }
}
