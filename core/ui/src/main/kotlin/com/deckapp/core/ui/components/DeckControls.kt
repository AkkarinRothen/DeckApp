package com.deckapp.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.AppTheme
import com.deckapp.core.ui.theme.LocalAppTheme

@Composable
fun deckCardBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
fun deckCardElevation(): CardElevation = CardDefaults.cardElevation(
    defaultElevation = if (LocalAppTheme.current == AppTheme.LIGHT) 1.dp else 0.dp
)

/** Native Material semantics and interactions with DeckApp defaults. Explicit styles still win. */
@Composable
fun DeckCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = deckCardElevation(),
    border: BorderStroke? = deckCardBorder(),
    content: @Composable ColumnScope.() -> Unit
) = Card(modifier, shape, colors, elevation, border, content)

@Composable
fun DeckCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = deckCardElevation(),
    border: BorderStroke? = deckCardBorder(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) = Card(onClick, modifier, enabled, shape, colors, elevation, border, interactionSource, content)

@Composable
fun DeckButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = Button(onClick, modifier.heightIn(min = 48.dp), enabled, shape, colors, elevation, border,
    contentPadding, interactionSource, content)

@Composable
fun DeckOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape, colors, elevation, border,
    contentPadding, interactionSource, content)

@Composable
fun DeckTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = TextButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape, colors, elevation, border,
    contentPadding, interactionSource, content)
