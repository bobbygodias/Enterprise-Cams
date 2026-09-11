package org.enterprisecams.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import org.enterprisecams.app.ui.EnterpriseCamsApp
import org.enterprisecams.app.ui.EnterpriseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EnterpriseTheme { EnterpriseCamsApp(viewModel()) } }
    }
}
