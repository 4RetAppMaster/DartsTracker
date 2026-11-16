package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PlayerAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    backgroundColor: Color = BrandPrimary,
    contentColor: Color = BrandOnPrimary
) {
    val initial = name.firstOrNull()?.uppercase() ?: "P"
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.45).sp
        )
    }
}

@Composable
fun CheckoutBadge(
    checkoutText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BrandSuccess.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        color = BrandSuccessContainer.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = "Checkout Target",
                tint = BrandSuccess,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = "CHECKOUT ROUTE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSuccess,
                    letterSpacing = 1.sp
                )
                Text(
                    text = checkoutText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = BrandPrimary
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp)),
        color = AppSurfaceElevated
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
