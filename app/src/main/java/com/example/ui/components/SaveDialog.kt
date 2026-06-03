package com.example.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.lang.AppLanguage
import com.example.lang.Lang

@Composable
fun SaveDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Lang.t("save_dialog_title", lang)) },
        text = { 
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text(Lang.t("enter_name", lang)) }, 
                singleLine = true 
            ) 
        },
        confirmButton = { 
            Button(onClick = { if (name.isNotBlank()) onSave(name) }) { 
                Text(Lang.t("save", lang)) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text(Lang.t("cancel", lang)) 
            } 
        }
    )
}
