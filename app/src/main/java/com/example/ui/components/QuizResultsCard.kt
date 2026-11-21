package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalQuizzMeColors
import kotlin.math.roundToInt

@Composable
fun QuizResultsCard(
    runningScore: Int,
    maxPossibleScore: Int,
    actionLabel: String = "Start New Session",
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    val accuracy = if (maxPossibleScore > 0) {
        ((runningScore.toFloat() / maxPossibleScore.toFloat()) * 100f).roundToInt()
    } else 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quiz_results_card"),
        shape = RoundedCornerShape(28.dp),
        color = ext.card,
        border = BorderStroke(1.dp, ext.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ext.successSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "Quiz complete trophy",
                    tint = ext.success,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Quiz Complete!",
                style = MaterialTheme.typography.headlineMedium,
                color = ext.title
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "You have successfully reviewed this section.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.subtext,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = ext.background
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL MARKS EARNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.subtext
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$runningScore / $maxPossibleScore",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = ext.accent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Accuracy Rate: $accuracy%",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.subtext
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("quiz_results_action_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ext.accent,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
