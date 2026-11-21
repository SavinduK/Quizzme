package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalQuizzMeColors

@Composable
fun QuizzMeHeader(
    title: String,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    addButtonTag: String = "header_add_button"
) {
    val ext = LocalQuizzMeColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = ext.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (onAddClick != null) {
                Surface(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(10.dp),
                    color = ext.accentSoft,
                    border = BorderStroke(1.dp, ext.accent),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .testTag(addButtonTag)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add",
                        tint = ext.accent,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = ext.border, thickness = 1.dp)
    }
}
