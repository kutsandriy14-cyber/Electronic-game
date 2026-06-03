package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolButton(icon: ImageVector, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFF00FFCC) else Color(0x7F22222E)
    val tint = if (isSelected) Color(0xFF121215) else Color.White
    val borderColor = if (isSelected) Color.Transparent else Color(0x44FFFFFF)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp) // Fixed height to allow clean vertical layouts without clipping text
            .clickable(onClick = onClick)
            .background(bgColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text, 
                style = MaterialTheme.typography.labelSmall, 
                fontSize = 9.sp,
                color = tint, 
                maxLines = 2, // Allow wrapping for complete translation views!
                lineHeight = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
