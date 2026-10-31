package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ByeProTheme

/** Shared BYE PRO FINANCE field style for every screen and form. */
@Composable
fun FinanceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = androidx.compose.material3.LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = if (singleLine) modifier.height(44.dp) else modifier.heightIn(min = 44.dp),
        enabled = enabled, readOnly = readOnly, textStyle = textStyle,
        label = label, placeholder = placeholder, leadingIcon = leadingIcon,
        trailingIcon = trailingIcon, prefix = prefix, suffix = suffix,
        supportingText = supportingText, isError = isError,
        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions, singleLine = singleLine,
        maxLines = maxLines, minLines = minLines, interactionSource = interactionSource,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ByeProTheme.colors.fieldText,
            unfocusedTextColor = ByeProTheme.colors.fieldText,
            focusedContainerColor = ByeProTheme.colors.fieldBg,
            unfocusedContainerColor = ByeProTheme.colors.fieldBg,
            disabledContainerColor = ByeProTheme.colors.fieldBg.copy(alpha = 0.55f),
            focusedBorderColor = ByeProTheme.colors.fieldFocus,
            unfocusedBorderColor = ByeProTheme.colors.fieldBorder,
            focusedLabelColor = ByeProTheme.colors.fieldFocus,
            cursorColor = ByeProTheme.colors.fieldFocus,
            errorBorderColor = MaterialTheme.colorScheme.error
        )
    )
}

/** Shared primary action button; custom semantic colors can be passed when required. */
@Composable
fun FinanceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = ByeProTheme.colors.goldPrimary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
        disabledElevation = 0.dp
    ),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick, modifier = modifier.heightIn(min = 46.dp), enabled = enabled, shape = shape,
        colors = colors, elevation = elevation, border = border,
        contentPadding = contentPadding, interactionSource = interactionSource, content = content
    )
}

/** Shared secondary action button. */
@Composable
fun FinanceOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(9.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = ByeProTheme.colors.cardSecondary,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    if (border == null) {
        OutlinedButton(
            onClick = onClick, modifier = modifier.heightIn(min = 40.dp), enabled = enabled, shape = shape,
            colors = colors, elevation = elevation, contentPadding = contentPadding,
            interactionSource = interactionSource, content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick, modifier = modifier.heightIn(min = 44.dp), enabled = enabled, shape = shape,
            colors = colors, elevation = elevation, border = border,
            contentPadding = contentPadding, interactionSource = interactionSource, content = content
        )
    }
}

/** Shared bordered quiet action following the Web .text-button contract. */
@Composable
fun FinanceTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(
        containerColor = Color.Transparent,
        contentColor = ByeProTheme.colors.textPrimary
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
}

/** Inline text action used for links such as auth mode switching. */
@Composable
fun FinanceLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 32.dp),
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = Color.Transparent,
            contentColor = ByeProTheme.colors.goldPrimary
        ),
        border = null,
        contentPadding = PaddingValues(0.dp),
        content = content
    )
}

/** Shared dialog surface and corner treatment. */
@Composable
fun FinanceAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = RoundedCornerShape(16.dp),
        containerColor = ByeProTheme.colors.surfaceMain,
        modifier = modifier.border(1.dp, ByeProTheme.colors.dialogBorder, RoundedCornerShape(16.dp)),
        iconContentColor = MaterialTheme.colorScheme.secondary,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        properties = properties
    )
}


/** Lucide-sized semantic icon wrapper for consistent 20dp glyphs across screens. */
@Composable
fun FinanceIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/** Accessible two-state choice styled with the same Web segmented-control tokens. */
@Composable
fun FinanceChoiceButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(9.dp)
    val background = if (selected) ByeProTheme.colors.goldPrimary else ByeProTheme.colors.cardSecondary
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary else ByeProTheme.colors.textPrimary
    val outline = if (selected) ByeProTheme.colors.goldPrimary else ByeProTheme.colors.borderBase
    CompositionLocalProvider(LocalContentColor provides foreground) {
        Box(
            modifier = modifier
                .heightIn(min = 44.dp)
                .background(background, shape)
                .border(1.dp, outline, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** Shared icon action control with consistent default accent color. */
@Composable
fun FinanceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides ByeProTheme.colors.textPrimary) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .then(Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp))
                .border(1.dp, ByeProTheme.colors.borderBase, RoundedCornerShape(12.dp))
                .background(ByeProTheme.colors.surfaceMain, RoundedCornerShape(12.dp)),
            enabled = enabled,
            content = content
        )
    }
}

/** Shared floating action button for create/add actions across the app. */
@Composable
fun FinanceFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ByeProTheme.colors.goldPrimary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        content = content
    )
}
