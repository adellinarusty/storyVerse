package com.example.ui.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Parser Teks Chat & Cerita:
 * - Teks yang diapit tanda pagar (#teks#) dirender menjadi cetak miring (italic).
 * - Teks yang diapit tanda bintang (*teks* atau _teks_) juga didukung menjadi cetak miring (italic).
 * - Ukuran dan model font sama untuk semua teks agar bersih dan seragam.
 */
object VisualNovelTextRenderer {

    fun parseChatText(
        rawText: String,
        baseColor: Color = Color(0xFFF3EDF7)
    ): AnnotatedString {
        return buildAnnotatedString {
            val clean = rawText
                .replace(Regex("""\[(?:SCENE_ART|IMAGE_PROMPT|IMAGE):\s*.*?]""", RegexOption.IGNORE_CASE), "")
                .trim()
                .replace("\r\n", "\n")
            var i = 0
            val len = clean.length

            while (i < len) {
                // 1. Cek tanda pagar #...# (WAJIB cetak miring)
                if (clean[i] == '#' && i + 1 < len) {
                    val endHash = clean.indexOf('#', i + 1)
                    if (endHash != -1) {
                        val italicContent = clean.substring(i + 1, endHash)
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = baseColor
                            )
                        ) {
                            append(italicContent)
                        }
                        i = endHash + 1
                        continue
                    }
                }

                // 2. Cek tanda bintang *...* atau garis bawah _..._ (cetak miring)
                if ((clean[i] == '*' || clean[i] == '_') && i + 1 < len && clean[i + 1] != '*' && clean[i + 1] != '_') {
                    val delim = clean[i]
                    val endMark = clean.indexOf(delim, i + 1)
                    if (endMark != -1 && endMark > i + 1) {
                        val italicContent = clean.substring(i + 1, endMark)
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = baseColor
                            )
                        ) {
                            append(italicContent)
                        }
                        i = endMark + 1
                        continue
                    }
                }

                // 3. Karakter normal
                append(clean[i])
                i++
            }
        }
    }
}

@Composable
fun FormattedChatMessageText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFFF3EDF7),
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 22.sp
) {
    val annotatedString = remember(text, textColor) {
        VisualNovelTextRenderer.parseChatText(text, textColor)
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = lineHeight,
        style = MaterialTheme.typography.bodyLarge
    )
}
