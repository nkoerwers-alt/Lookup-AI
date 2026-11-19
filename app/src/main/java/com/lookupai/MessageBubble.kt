
package com.lookupai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MessagePart(
    val content: String,
    val isCode: Boolean,
    val language: String = ""
)

private fun splitMessage(text: String): List<MessagePart> {
    val regex = Regex("```([^\\n`]*)\\n([\\s\\S]*?)```")
    val parts = mutableListOf<MessagePart>()
    var position = 0

    for (match in regex.findAll(text)) {
        if (match.range.first > position) {
            parts.add(MessagePart(text.substring(position, match.range.first), false))
        }

        parts.add(
            MessagePart(
                match.groupValues[2].trimEnd(),
                true,
                match.groupValues[1].trim().ifBlank { "CODE" }
            )
        )
        position = match.range.last + 1
    }

    if (position < text.length) {
        parts.add(MessagePart(text.substring(position), false))
    }

    if (parts.isEmpty()) parts.add(MessagePart(text, false))
    return parts
}

private fun formatInline(text: String): AnnotatedString {
    val result = buildAnnotatedString {
        val regex = Regex("""(\*\*(.+?)\*\*|__(.+?)__|\*(.+?)\*|_(.+?)_)""")
        var position = 0

        for (match in regex.findAll(text)) {
            append(text.substring(position, match.range.first))

            val bold = match.groupValues[2].ifEmpty { match.groupValues[3] }
            val italic = match.groupValues[4].ifEmpty { match.groupValues[5] }

            when {
                bold.isNotEmpty() -> withStyle(
                    SpanStyle(fontWeight = FontWeight.Bold)
                ) { append(bold) }

                italic.isNotEmpty() -> withStyle(
                    SpanStyle(fontStyle = FontStyle.Italic)
                ) { append(italic) }
            }

            position = match.range.last + 1
        }

        append(text.substring(position))
    }

    return result
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val userColor = Color(0xFF7650D9)
    val aiColor = Color(0xFF191922)
    val textColor = Color(0xFFF4F2FA)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .background(Color(0xFF9565FF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("F", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(
                    color = if (message.isUser) userColor else aiColor,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 5.dp,
                        bottomEnd = if (message.isUser) 5.dp else 18.dp
                    )
                )
                .padding(horizontal = 15.dp, vertical = 12.dp)
        ) {
            if (!message.isUser) {
                Text(
                    "Fenn AI",
                    color = Color(0xFFB99CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
            }

            splitMessage(message.text).forEach { part ->
                if (part.isCode) {
                    CodeBlockView(part.content, part.language)
                } else {
                    FormattedText(part.content, textColor)
                }
            }
        }
    }
}

@Composable
private fun FormattedText(text: String, color: Color) {
    val lines = text.lines()

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        lines.forEach { line ->
            val heading = Regex("""^(#{1,3})\s+(.+)$""").find(line)
            val bullet = Regex("""^\s*[-*+]\s+(.+)$""").find(line)
            val numbered = Regex("""^\s*\d+[.)]\s+(.+)$""").find(line)

            when {
                heading != null -> {
                    val level = heading.groupValues[1].length
                    Text(
                        text = formatInline(heading.groupValues[2]),
                        color = color,
                        fontSize = when (level) {
                            1 -> 24.sp
                            2 -> 21.sp
                            else -> 18.sp
                        },
                        fontWeight = FontWeight.Bold,
                        lineHeight = 29.sp
                    )
                }

                bullet != null -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•  ", color = Color(0xFFB99CFF), fontSize = 15.sp)
                        Text(
                            formatInline(bullet.groupValues[1]),
                            color = color,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }

                numbered != null -> {
                    Text(
                        formatInline(line.trim()),
                        color = color,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }

                line.isBlank() -> Spacer(Modifier.height(5.dp))

                else -> Text(
                    formatInline(line),
                    color = color,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun CodeBlockView(code: String, language: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .background(Color(0xFF0D0D13), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                language.uppercase(),
                color = Color(0xFFB99CFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                if (copied) "Copied!" else "Copy code",
                color = Color(0xFFB99CFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                code,
                color = Color(0xFFE5E1F0),
                fontSize = 12.sp,
                lineHeight = 19.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
