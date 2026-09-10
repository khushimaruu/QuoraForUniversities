package com.MADproject.quoraforuniversities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.MADproject.quoraforuniversities.ui.theme.CampusQnATheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CampusQnATheme {
                ProfileScreen {
                    // Handle navigation or actions from ProfileScreen if needed
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    CampusQnATheme {
        ProfileScreen()
    }
}
