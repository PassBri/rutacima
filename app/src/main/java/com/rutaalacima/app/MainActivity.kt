package com.rutaalacima.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.rutaalacima.app.ui.RutaRoot
import com.rutaalacima.app.ui.theme.RutaTheme

/** AppCompatActivity para que el cambio de idioma por app (12 idiomas) se aplique al instante. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RutaTheme {
                RutaRoot()
            }
        }
    }
}
