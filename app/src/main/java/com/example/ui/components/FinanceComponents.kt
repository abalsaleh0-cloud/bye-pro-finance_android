package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier,
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
    shape: Shape = RoundedCornerShape(12.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = ByeProTheme.colors.goldPrimary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
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
    shape: Shape = RoundedCornerShape(12.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = ByeProTheme.colors.goldPrimary),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    if (border == null) {
        OutlinedButton(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
            colors = colors, elevation = elevation, contentPadding = contentPadding,
            interactionSource = interactionSource, content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
            colors = colors, elevation = elevation, border = border,
            contentPadding = contentPadding, interactionSource = interactionSource, content = content
        )
    }
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
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = RoundedCornerShape(20.dp),
        colors = AlertDialogDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        properties = properties
    )
}
