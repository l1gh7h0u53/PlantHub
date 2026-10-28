package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data model for an item in a Material 3 Expressive Connected Button Group.
 */
data class ConnectedButtonItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val testTag: String? = null
)

/**
 * Material 3 Expressive Connected Button Group (Outlined style with morphing corners).
 *
 * Implements M3 Expressive guidelines for Connected Button Groups:
 * https://m3.material.io/components/button-groups/guidelines
 *
 * - Outlined connected container with subtle boundary border and inner dividers.
 * - Fixed, compact M3 Expressive height (40dp) with minimal whitespace.
 * - Pressed behavior: In a connected button group, only the shape of the pressed
 *   button dynamically morphs (without affecting adjacent button widths).
 * - Selected behavior: A selected button changes shape (morphs from round to square
 *   or square to round) alongside container elevation and active color fill.
 */
@Composable
fun <T> M3ConnectedButtonGroup(
    items: List<ConnectedButtonItem<T>>,
    selectedValue: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val total = items.size
            items.forEachIndexed { index, item ->
                val isSelected = item.value == selectedValue
                val prevSelected = index > 0 && items[index - 1].value == selectedValue

                // Subtle vertical divider between unselected adjacent items
                if (index > 0 && !isSelected && !prevSelected) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.55f)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    )
                }

                ConnectedButtonSegment(
                    item = item,
                    isSelected = isSelected,
                    isFirst = index == 0,
                    isLast = index == total - 1,
                    onClick = { onItemSelected(item.value) }
                )
            }
        }
    }
}

@Composable
private fun <T> RowScope.ConnectedButtonSegment(
    item: ConnectedButtonItem<T>,
    isSelected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // M3 Expressive Shape Morphing Behavior:
    // - Selected button changes shape: morphs to rounded-square (8dp)
    // - Unselected button: conforms to outer connected geometry (outer round 22dp, inner 4dp)
    // - Pressed behavior: only the pressed button's shape compresses/morphs (to 6dp)
    //   while its width remains fixed (weight 1f) so adjacent button widths are unaffected.
    val targetStartCorner = when {
        isPressed -> 6.dp
        isSelected -> 8.dp
        isFirst -> 22.dp
        else -> 4.dp
    }

    val targetEndCorner = when {
        isPressed -> 6.dp
        isSelected -> 8.dp
        isLast -> 22.dp
        else -> 4.dp
    }

    val animatedStartCorner by animateDpAsState(
        targetValue = targetStartCorner,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "m3_btn_start_corner"
    )

    val animatedEndCorner by animateDpAsState(
        targetValue = targetEndCorner,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "m3_btn_end_corner"
    )

    val segmentShape = RoundedCornerShape(
        topStart = animatedStartCorner,
        bottomStart = animatedStartCorner,
        topEnd = animatedEndCorner,
        bottomEnd = animatedEndCorner
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "m3_btn_container_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "m3_btn_content_color"
    )

    val segmentModifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .clip(segmentShape)
        .background(containerColor)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            role = Role.RadioButton,
            onClick = onClick
        )
        .semantics {
            selected = isSelected
        }
        .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier)

    Box(
        modifier = segmentModifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Box(modifier = Modifier.size(5.dp))
            }
            Text(
                text = item.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
