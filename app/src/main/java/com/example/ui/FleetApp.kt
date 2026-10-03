package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.RoleSwitcherHeader
import com.example.ui.driver.DriverScreen
import com.example.ui.owner.OwnerScreen
import com.example.viewmodel.AppRole
import com.example.viewmodel.FleetViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FleetApp(
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val allDrivers by viewModel.allDrivers.collectAsStateWithLifecycle()
    val allVehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    val pendingAttendance by viewModel.pendingAttendance.collectAsStateWithLifecycle()

    val selectedDriverId by viewModel.selectedDriverId.collectAsStateWithLifecycle()
    val activeShift by viewModel.currentDriverActiveShift.collectAsStateWithLifecycle()
    val driverRecords by viewModel.currentDriverRecords.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // BackHandler: if in Owner mode, back returns to Driver mode
    BackHandler(enabled = currentRole == AppRole.OWNER) {
        viewModel.setRole(AppRole.DRIVER)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RoleSwitcherHeader(
                currentRole = currentRole,
                pendingApprovalsCount = pendingAttendance.size,
                onRoleChange = { newRole ->
                    viewModel.setRole(newRole)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentRole,
            transitionSpec = {
                if (targetState == AppRole.OWNER) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "role_switch_animation",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { role ->
            when (role) {
                AppRole.DRIVER -> {
                    DriverScreen(
                        drivers = allDrivers,
                        vehicles = allVehicles,
                        selectedDriverId = selectedDriverId,
                        activeShift = activeShift,
                        records = driverRecords,
                        onSelectDriver = { viewModel.selectDriver(it) },
                        onPunchIn = { driver, vehicle, startOdo, shiftType, notes ->
                            viewModel.punchIn(driver, vehicle, startOdo, shiftType, notes)
                        },
                        onPunchOut = { record, endOdo, notes ->
                            viewModel.punchOut(record, endOdo, notes)
                        },
                        onAddDriver = { name, phone, lic, wage, ot, plate ->
                            viewModel.addDriver(name, phone, lic, wage, ot, plate)
                        }
                    )
                }

                AppRole.OWNER -> {
                    OwnerScreen(
                        drivers = allDrivers,
                        vehicles = allVehicles,
                        allAttendance = allAttendance,
                        pendingAttendance = pendingAttendance,
                        onFinalizeRecord = { recId, remarks, otHours, allowance, totalPay ->
                            viewModel.finalizeAttendance(recId, remarks, otHours, allowance, totalPay)
                        },
                        onRejectRecord = { recId, reason ->
                            viewModel.rejectAttendance(recId, reason)
                        },
                        onBulkFinalize = {
                            viewModel.bulkFinalizeAllPending()
                        },
                        onAddDriver = { name, phone, lic, wage, ot, plate ->
                            viewModel.addDriver(name, phone, lic, wage, ot, plate)
                        },
                        onAddVehicle = { plate, model, type, odo, fuel ->
                            viewModel.addVehicle(plate, model, type, odo, fuel)
                        }
                    )
                }
            }
        }
    }
}
