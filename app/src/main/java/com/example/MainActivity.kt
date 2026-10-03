package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.database.FleetDatabase
import com.example.data.repository.FleetRepository
import com.example.ui.FleetApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppRole
import com.example.viewmodel.FleetViewModel
import com.example.viewmodel.FleetViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = FleetDatabase.getDatabase(applicationContext, lifecycleScope)
        val dao = database.fleetDao()
        val repository = FleetRepository(dao)

        val viewModel: FleetViewModel by viewModels {
            FleetViewModelFactory(repository, dao)
        }

        // Check if launched via Owner launcher alias or intent extra
        val launchTarget = intent?.getStringExtra("target_role")
        val isOwnerLauncher = intent?.component?.className?.contains("Owner", ignoreCase = true) == true
        if (launchTarget == "OWNER" || isOwnerLauncher) {
            viewModel.setRole(AppRole.OWNER)
        }

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FleetApp(viewModel = viewModel)
                }
            }
        }
    }
}
