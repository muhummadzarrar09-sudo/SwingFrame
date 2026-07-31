package app.swingframe.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.swingframe.R
import app.swingframe.ui.theme.BackgroundDark
import app.swingframe.ui.theme.MoonstoneBlue
import app.swingframe.ui.theme.MustardGreen

@Composable
fun AppSplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "SwingFrame Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Brand Name
        Text(
            text = "SWINGFRAME",
            color = MoonstoneBlue,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtitle
        Text(
            text = "ON-DEVICE AI COACH",
            color = MustardGreen,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
