package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalQuizzMeColors
import com.example.ui.theme.subjectAccentColor

enum class FolderSortOrder(val label: String) {
    RECENT("Recent"),
    TITLE_ASC("A–Z")
}

data class DrawerFolderItem(
    val id: String,
    val title: String,
    val subject: String,
    val term: String,
    val updatedAt: Long = 0L
)

@Composable
fun TermFolderDrawerContent(
    headerTitle: String,
    items: List<DrawerFolderItem>,
    selectedTerm: String?,
    selectedItemId: String?,
    sortOrder: FolderSortOrder,
    onSortOrderChange: (FolderSortOrder) -> Unit,
    onSelectAll: () -> Unit,
    onSelectTerm: (String) -> Unit,
    onSelectItem: (DrawerFolderItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current

    ModalDrawerSheet(
        drawerContainerColor = ext.card,
        modifier = modifier.width(310.dp)
    ) {
        TermFolderPanelBody(
            headerTitle = headerTitle,
            items = items,
            selectedTerm = selectedTerm,
            selectedItemId = selectedItemId,
            sortOrder = sortOrder,
            onSortOrderChange = onSortOrderChange,
            onSelectAll = onSelectAll,
            onSelectTerm = onSelectTerm,
            onSelectItem = onSelectItem,
            onClose = onClose,
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
        )
    }
}

@Composable
fun TermFolderSidebar(
    headerTitle: String,
    items: List<DrawerFolderItem>,
    selectedTerm: String?,
    selectedItemId: String?,
    sortOrder: FolderSortOrder,
    onSortOrderChange: (FolderSortOrder) -> Unit,
    onSelectAll: () -> Unit,
    onSelectTerm: (String) -> Unit,
    onSelectItem: (DrawerFolderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        color = ext.card,
        modifier = modifier
            .width(288.dp)
            .fillMaxHeight()
    ) {
        TermFolderPanelBody(
            headerTitle = headerTitle,
            items = items,
            selectedTerm = selectedTerm,
            selectedItemId = selectedItemId,
            sortOrder = sortOrder,
            onSortOrderChange = onSortOrderChange,
            onSelectAll = onSelectAll,
            onSelectTerm = onSelectTerm,
            onSelectItem = onSelectItem,
            onClose = null,
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun TermFolderPanelBody(
    headerTitle: String,
    items: List<DrawerFolderItem>,
    selectedTerm: String?,
    selectedItemId: String?,
    sortOrder: FolderSortOrder,
    onSortOrderChange: (FolderSortOrder) -> Unit,
    onSelectAll: () -> Unit,
    onSelectTerm: (String) -> Unit,
    onSelectItem: (DrawerFolderItem) -> Unit,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current

    val groupedByTerm = remember(items, sortOrder) {
        val grouped = items.groupBy { item ->
            item.term.trim().ifBlank { "Uncategorized" }
        }
        val sortedTerms = grouped.keys.sortedWith(
            compareBy<String> { it == "Uncategorized" }.thenBy { it.lowercase() }
        )
        sortedTerms.map { term ->
            val termItems = grouped[term].orEmpty()
            val ordered = when (sortOrder) {
                FolderSortOrder.RECENT -> termItems.sortedByDescending { it.updatedAt }
                FolderSortOrder.TITLE_ASC -> termItems.sortedBy { it.title.lowercase() }
            }
            term to ordered
        }
    }

    val expandedState = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier) {
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = ext.accent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = ext.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("drawer_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close folder drawer",
                        tint = ext.subtext
                    )
                }
            }
        }

        // Sort Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortToggleChip(
                label = "Recent",
                icon = Icons.Rounded.Schedule,
                selected = sortOrder == FolderSortOrder.RECENT,
                onClick = { onSortOrderChange(FolderSortOrder.RECENT) },
                tag = "drawer_sort_recent",
                modifier = Modifier.weight(1f)
            )
            SortToggleChip(
                label = "A–Z",
                icon = Icons.Rounded.SortByAlpha,
                selected = sortOrder == FolderSortOrder.TITLE_ASC,
                onClick = { onSortOrderChange(FolderSortOrder.TITLE_ASC) },
                tag = "drawer_sort_alpha",
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = ext.border)

        // Root "All" Entry
        val allSelected = selectedTerm == null && selectedItemId == null
        Surface(
            onClick = onSelectAll,
            shape = RoundedCornerShape(12.dp),
            color = if (allSelected) ext.accentSoft else Color.Transparent,
            border = if (allSelected) BorderStroke(1.dp, ext.accent) else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .minimumInteractiveComponentSize()
                .testTag("drawer_all_terms_item")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AllInclusive,
                        contentDescription = null,
                        tint = if (allSelected) ext.accent else ext.subtext,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "All Terms",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (allSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (allSelected) ext.accent else ext.title
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (allSelected) ext.accent else ext.background
                ) {
                    Text(
                        text = items.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (allSelected) Color.White else ext.subtext,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = ext.border, modifier = Modifier.padding(horizontal = 16.dp))

        if (groupedByTerm.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No folders yet. Add notes or past papers to browse by term.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.subtext
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(groupedByTerm, key = { it.first }) { (term, termItems) ->
                    val isExpanded = expandedState[term] ?: (selectedTerm == term || groupedByTerm.size == 1)
                    val isTermSelected = selectedTerm == term && selectedItemId == null
                    val formattedTerm = formatTermFolderLabel(term)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isTermSelected) ext.accentSoft else Color.Transparent,
                            border = if (isTermSelected) BorderStroke(1.dp, ext.accent) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    expandedState[term] = true
                                    onSelectTerm(term)
                                }
                                .testTag("drawer_term_folder_$term")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = ext.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = formattedTerm,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isTermSelected) ext.accent else ext.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ext.background
                                ) {
                                    Text(
                                        text = termItems.size.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.subtext,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { expandedState[term] = !isExpanded },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse folder" else "Expand folder",
                                        tint = ext.subtext,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .padding(start = 19.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .fillMaxHeight()
                                        .background(ext.border)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    termItems.forEach { item ->
                                        val itemSelected = selectedItemId == item.id
                                        val dotColor = subjectAccentColor(item.subject)
                                        Surface(
                                            onClick = { onSelectItem(item) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (itemSelected) ext.accentSoft else Color.Transparent,
                                            border = if (itemSelected) BorderStroke(1.dp, ext.accent) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .minimumInteractiveComponentSize()
                                                .testTag("drawer_title_item_${item.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.Description,
                                                    contentDescription = null,
                                                    tint = if (itemSelected) ext.accent else ext.subtext,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (itemSelected) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (itemSelected) ext.accent else ext.title,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (item.subject.isNotBlank()) {
                                                        Text(
                                                            text = item.subject,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ext.subtext,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortToggleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalQuizzMeColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) ext.accentSoft else ext.background,
        border = BorderStroke(1.dp, if (selected) ext.accent else ext.border),
        modifier = modifier
            .height(40.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ext.accent else ext.subtext,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) ext.accent else ext.title
            )
        }
    }
}

private fun formatTermFolderLabel(term: String): String {
    val trimmed = term.trim()
    if (trimmed.equals("Uncategorized", ignoreCase = true)) return trimmed
    return if (trimmed.startsWith("Term", ignoreCase = true)) trimmed else "Term $trimmed"
}
