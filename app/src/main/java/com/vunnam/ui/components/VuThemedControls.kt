package com.vunnam.ui.components

import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun vuSwitchColors(): SwitchColors {
    val c = VuShareTheme.colors
    return SwitchDefaults.colors(
        checkedThumbColor = c.switchCheckedThumb,
        checkedTrackColor = c.switchCheckedTrack,
        checkedBorderColor = c.switchCheckedTrack,
        uncheckedThumbColor = c.switchUncheckedThumb,
        uncheckedTrackColor = c.switchUncheckedTrack,
        uncheckedBorderColor = c.switchUncheckedBorder,
        disabledCheckedThumbColor = c.disabledText,
        disabledCheckedTrackColor = c.disabledSurface,
        disabledUncheckedThumbColor = c.disabledText,
        disabledUncheckedTrackColor = c.disabledSurface
    )
}

@Composable
fun vuCheckboxColors(): CheckboxColors {
    val c = VuShareTheme.colors
    return CheckboxDefaults.colors(
        checkedColor = c.primaryAccent,
        checkmarkColor = c.onPrimaryAccent,
        uncheckedColor = c.secondaryText,
        disabledCheckedColor = c.disabledSurface,
        disabledUncheckedColor = c.disabledSurface
    )
}

@Composable
fun vuFilterChipColors(): SelectableChipColors {
    val c = VuShareTheme.colors
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = c.primaryAccent,
        selectedLabelColor = c.onPrimaryAccent,
        selectedLeadingIconColor = c.onPrimaryAccent,
        containerColor = c.primaryCard,
        labelColor = c.primaryText,
        iconColor = c.secondaryText
    )
}

@Composable
fun vuTextFieldColors(): TextFieldColors {
    val c = VuShareTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.inputBackground,
        unfocusedContainerColor = c.inputBackground,
        disabledContainerColor = c.disabledSurface,
        focusedTextColor = c.primaryText,
        unfocusedTextColor = c.primaryText,
        focusedPlaceholderColor = c.secondaryText,
        unfocusedPlaceholderColor = c.secondaryText,
        focusedLeadingIconColor = c.primaryAccent,
        unfocusedLeadingIconColor = c.secondaryText,
        focusedTrailingIconColor = c.primaryText,
        unfocusedTrailingIconColor = c.secondaryText,
        focusedBorderColor = c.primaryAccent,
        unfocusedBorderColor = c.border,
        cursorColor = c.primaryAccent
    )
}
