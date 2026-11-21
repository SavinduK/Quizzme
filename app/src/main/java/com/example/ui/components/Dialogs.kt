package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalQuizzMeColors

@Composable
fun QuizzMeAlertDialog(
    title: String,
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ext.background,
            border = BorderStroke(1.dp, ext.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = if (isSuccess) ext.success else ext.danger,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 2.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = ext.title,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.subtext
                )

                Spacer(Modifier.height(22.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("alert_dismiss_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ext.accentSoft,
                        contentColor = ext.accent
                    ),
                    border = BorderStroke(1.dp, ext.accent)
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val ext = LocalQuizzMeColors.current
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.card,
            border = BorderStroke(1.dp, ext.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ext.dangerSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = ext.danger,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = ext.title
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.subtext,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("delete_cancel_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ext.border)
                    ) {
                        Text("Cancel", color = ext.title, style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("delete_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.danger,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
