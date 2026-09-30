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
 * - Dialog Karakter: Ditampilkan dengan warna putih jernih terang (#FFFFFF) berbobot tegas (SemiBold untuk kutipan "..."), agar sangat kontras dan tidak bentrok dengan background cerita.
 * - Tindakan & Deskripsi Suasana (#teks# atau *teks* atau _teks_): Dicetak miring (italic) dengan warna lavender lembut (#C7B9E5).
 */
object VisualNovelTextRenderer {

    val DIALOG_BRIGHT_COLOR = Color(0xFFFFFFFF)
    val ACTION_SOFT_COLOR = Color(0xFFC7B9E5)

    fun parseChatText(
        rawText: String,
        baseColor: Color = DIALOG_BRIGHT_COLOR,
        actionColor: Color = ACTION_SOFT_COLOR
    ): AnnotatedString {
        return buildAnnotatedString {
            val clean = rawText
                .replace(Regex("""\[(?:SCENE_ART|IMAGE_PROMPT|IMAGE):\s*.*?]""", RegexOption.IGNORE_CASE), "")
                .trim()
                .replace("\r\n", "\n")
            var i = 0
            val len = clean.length

            while (i < len) {
                // 1. Cek tanda pagar #...# (WAJIB cetak miring - tindakan/suasana)
                if (clean[i] == '#' && i + 1 < len) {
                    val endHash = clean.indexOf('#', i + 1)
                    if (endHash != -1) {
                        val italicContent = clean.substring(i + 1, endHash)
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = actionColor
                            )
                        ) {
                            append(italicContent)
                        }
                        i = endHash + 1
                        continue
                    }
                }

                // 2. Cek tanda bintang *...* atau garis bawah _..._ (cetak miring - tindakan/suasana)
                if ((clean[i] == '*' || clean[i] == '_') && i + 1 < len && clean[i + 1] != '*' && clean[i + 1] != '_') {
                    val delim = clean[i]
                    val endMark = clean.indexOf(delim, i + 1)
                    if (endMark != -1 && endMark > i + 1) {
                        val italicContent = clean.substring(i + 1, endMark)
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = actionColor
                            )
                        ) {
                            append(italicContent)
                        }
                        i = endMark + 1
                        continue
                    }
                }

                // 3. Cek kutipan dialog "..." (dialog langsung karakter - putih terang mencolok & tebal)
                if (clean[i] == '"' && i + 1 < len) {
                    val endQuote = clean.indexOf('"', i + 1)
                    if (endQuote != -1) {
                        val quoteContent = clean.substring(i, endQuote + 1)
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFFFFFFF),
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = FontStyle.Normal
                            )
                        ) {
                            append(quoteContent)
                        }
                        i = endQuote + 1
                        continue
                    }
                }

                // 4. Karakter narasi/dialog lainnya (warna terang kontras tinggi)
                withStyle(
                    SpanStyle(
                        color = baseColor
                    )
                ) {
                    append(clean[i])
                }
                i++
            }
        }
    }
}

@Composable
fun FormattedChatMessageText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFFFFFFFF),
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 23.sp
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

