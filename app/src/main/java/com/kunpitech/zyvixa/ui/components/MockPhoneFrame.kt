package com.kunpitech.zyvixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MockPhoneFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.58f)
            .aspectRatio(9f / 19.5f)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF0D0D18))
            .border(
                3.5.dp,
                Brush.verticalGradient(listOf(Color(0xFF2C2D3A), Color(0xFF151525))),
                RoundedCornerShape(32.dp)
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        // Wallpapers preview content
        content()

        // Camera Punch Hole / Dynamic Island notch
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .width(64.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black)
        )

        // Lockscreen Simulated Text Info overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "12:45",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Saturday, July 11",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Gestures Home Indicator Line
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .width(70.dp)
                .height(3.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.8f))
        )
    }
}
