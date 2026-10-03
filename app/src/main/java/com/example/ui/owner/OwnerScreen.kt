package com.example.ui.owner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Driver
import com.example.data.model.Vehicle
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScreen(
    drivers: List<Driver>,
    vehicles: List<Vehicle>,
    allAttendance: List<AttendanceRecord>,
    pendingAttendance: List<AttendanceRecord>,
    onFinalizeRecord: (recordId: Long, remarks: String, overtimeHours: Double, allowance: Double, totalPayout: Double) -> Unit,
    onRejectRecord: (recordId: Long, reason: String) -> Unit,
    onBulkFinalize: () -> Unit,
    onAddDriver: (name: String, phone: String, license: String, wage: Double, otRate: Double, plate: String) -> Unit,
    onAddVehicle: (plate: String, model: String, type: String, currentOdo: Double, fuel: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Pending Review, 1: All Attendance, 2: Fleet & Drivers
    val tabs = listOf("Pending Review (${pendingAttendance.size})", "All Attendance", "Fleet & Drivers")

    var finalizingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var rejectingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }

    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }

    // Metrics calculations
    val activeOnDutyCount = remember(allAttendance) {
        allAttendance.count { it.status == AttendanceStatus.PUNCHED_IN }
    }
    val finalizedCount = remember(allAttendance) {
        allAttendance.count { it.status == AttendanceStatus.FINALIZED }
    }
    val totalPayoutSum = remember(allAttendance) {
        allAttendance.filter { it.status == AttendanceStatus.FINALIZED }.sumOf { it.totalPayoutAmount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // KPI Metric Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Pending Approvals",
                        value = "${pendingAttendance.size}",
                        subtitle = if (pendingAttendance.isEmpty()) "All clear" else "Action needed",
                        icon = Icons.Default.HourglassTop,
                        iconBgColor = StatusPendingBg,
                        iconColor = StatusPendingAmber,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_pending_approvals")
                    )

                    MetricStatCard(
                        title = "Active On Duty",
                        value = "$activeOnDutyCount",
                        subtitle = "Drivers in shift",
                        icon = Icons.Default.LocalShipping,
                        iconBgColor = StatusActiveBg,
                        iconColor = StatusActiveBlue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_active_drivers")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Finalized Shifts",
                        value = "$finalizedCount",
                        subtitle = "Verified & closed",
                        icon = Icons.Default.Verified,
                        iconBgColor = StatusSuccessBg,
                        iconColor = StatusSuccessGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_finalized_shifts")
                    )

                    MetricStatCard(
                        title = "Total Payout",
                        value = "₹${totalPayoutSum.toInt()}",
                        subtitle = "Approved wages",
                        icon = Icons.Default.Payments,
                        iconBgColor = Color(0xFFEFF6FF),
                        iconColor = OwnerIndigoPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_wages")
                    )
                }
            }
        }

        // Navigation Tabs (Owner Modes)
        item {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedTab == index) OwnerIndigoPrimary else TextSecondaryDark
                            )
                        },
                        modifier = Modifier.testTag("owner_tab_$index")
                    )
                }
            }
        }

        // TAB CONTENT
        when (selectedTab) {
            // TAB 0: PENDING REVIEW QUEUE
            0 -> {
                if (pendingAttendance.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Attendance Submissions Awaiting Approval",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            Button(
                                onClick = onBulkFinalize,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccessGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("bulk_finalize_button")
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finalize All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(pendingAttendance, key = { it.id }) { record ->
                        OwnerPendingAttendanceCard(
                            record = record,
                            driver = drivers.find { it.id == record.driverId },
                            onOpenFinalizeDialog = { finalizingRecord = record },
                            onOpenRejectDialog = { rejectingRecord = record }
                        )
                    }
                } else {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = StatusSuccessGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Pending Approvals",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "All driver attendance records are up to date! When a driver completes their shift in the Driver App, it will appear here for your review and finalization.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // TAB 1: ALL ATTENDANCE HISTORY
            1 -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fleet Attendance Records (${allAttendance.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }

                if (allAttendance.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No records logged yet.",
                                modifier = Modifier.padding(24.dp),
                                color = TextSecondaryDark
                            )
                        }
                    }
                } else {
                    items(allAttendance, key = { it.id }) { record ->
                        OwnerHistoryCard(
                            record = record,
                            onReopenFinalize = { finalizingRecord = record }
                        )
                    }
                }
            }

            // TAB 2: FLEET & DRIVERS MANAGEMENT
            2 -> {
                item {
                    // Drivers Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registered Drivers (${drivers.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        FilledTonalButton(
                            onClick = { showAddDriverDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("owner_add_driver_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Driver", fontSize = 12.sp)
                        }
                    }
                }

                items(drivers, key = { it.id }) { driver ->
                    DriverManagementCard(driver = driver)
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    // Vehicles Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fleet Vehicles (${vehicles.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        FilledTonalButton(
                            onClick = { showAddVehicleDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("owner_add_vehicle_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Vehicle", fontSize = 12.sp)
                        }
                    }
                }

                items(vehicles, key = { it.id }) { vehicle ->
                    VehicleManagementCard(vehicle = vehicle)
                }
            }
        }
    }

    // Finalize Attendance Dialog
    finalizingRecord?.let { rec ->
        FinalizeAttendanceDialog(
            record = rec,
            driver = drivers.find { it.id == rec.driverId },
            onDismiss = { finalizingRecord = null },
            onFinalize = { remarks, otHours, allowance, totalPay ->
                onFinalizeRecord(rec.id, remarks, otHours, allowance, totalPay)
                finalizingRecord = null
            }
        )
    }

    // Reject Attendance Dialog
    rejectingRecord?.let { rec ->
        RejectAttendanceDialog(
            record = rec,
            onDismiss = { rejectingRecord = null },
            onConfirmReject = { reason ->
                onRejectRecord(rec.id, reason)
                rejectingRecord = null
            }
        )
    }

    // Add Driver Dialog
    if (showAddDriverDialog) {
        AddDriverDialog(
            onDismiss = { showAddDriverDialog = false },
            onAddDriver = onAddDriver
        )
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onAddVehicle = onAddVehicle
        )
    }
}

@Composable
fun OwnerPendingAttendanceCard(
    record: AttendanceRecord,
    driver: Driver?,
    onOpenFinalizeDialog: () -> Unit,
    onOpenRejectDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val baseWage = driver?.wagePerDay ?: 800.0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Driver Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DriverAmberLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = record.driverName.take(1),
                            color = DriverAmberDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Column {
                        Text(
                            text = record.driverName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "${record.dateString} • ${record.shiftType}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
                StatusBadge(status = record.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle and Times
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Vehicle: ${record.vehiclePlate} (${record.vehicleModel})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Check-In", fontSize = 10.sp, color = TextTertiaryDark)
                            Text(
                                timeFormat.format(Date(record.checkInTimestamp)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column {
                            Text("Check-Out", fontSize = 10.sp, color = TextTertiaryDark)
                            Text(
                                record.checkOutTimestamp?.let { timeFormat.format(Date(it)) } ?: "--:--",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column {
                            Text("Duration", fontSize = 10.sp, color = TextTertiaryDark)
                            Text(
                                "${record.calculatedHours} hrs",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccessGreen
                            )
                        }

                        Column {
                            Text("Distance", fontSize = 10.sp, color = TextTertiaryDark)
                            Text(
                                "${record.calculatedDistanceKm.toInt()} km",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OwnerIndigoPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Odometer: ${record.startOdometerKm.toInt()} km → ${record.endOdometerKm?.toInt() ?: "--"} km",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            if (record.driverNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Driver Note: ${record.driverNotes}",
                        fontSize = 11.sp,
                        color = Color(0xFF1E40AF),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenRejectDialog,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reject_attendance_${record.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject / Rework", fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenFinalizeDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccessGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("finalize_attendance_${record.id}")
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check & Finalize", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OwnerHistoryCard(
    record: AttendanceRecord,
    onReopenFinalize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = record.driverName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "• ${record.dateString}",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
                StatusBadge(status = record.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${record.vehiclePlate} • ${record.calculatedHours} hrs • ${record.calculatedDistanceKm.toInt()} km",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                if (record.status == AttendanceStatus.FINALIZED) {
                    Text(
                        text = "Approved: ₹${record.totalPayoutAmount.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = StatusSuccessGreen
                    )
                }
            }

            if (record.ownerRemarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = if (record.status == AttendanceStatus.FINALIZED) StatusSuccessBg else StatusRejectedBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (record.status == AttendanceStatus.FINALIZED) Icons.Default.Verified else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (record.status == AttendanceStatus.FINALIZED) StatusSuccessGreen else StatusRejectedRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Owner Stamp: ${record.ownerRemarks}",
                            fontSize = 11.sp,
                            color = if (record.status == AttendanceStatus.FINALIZED) Color(0xFF065F46) else Color(0xFF991B1B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverManagementCard(driver: Driver) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = driver.name.take(1),
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )
                }

                Column {
                    Text(
                        text = driver.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Lic: ${driver.licenseNumber} • ${driver.phone}",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${driver.wagePerDay.toInt()}/day",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = StatusSuccessGreen
                )
                Text(
                    text = "OT: ₹${driver.overtimeHourlyRate.toInt()}/hr",
                    fontSize = 10.sp,
                    color = TextTertiaryDark
                )
            }
        }
    }
}

@Composable
fun VehicleManagementCard(vehicle: Vehicle) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = OwnerIndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = vehicle.plateNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "${vehicle.modelName} • ${vehicle.vehicleType}",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${vehicle.currentOdometerKm.toInt()} km",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = vehicle.fuelType,
                    fontSize = 10.sp,
                    color = TextTertiaryDark
                )
            }
        }
    }
}
