package com.example.ui.driver

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Driver
import com.example.data.model.Vehicle
import com.example.ui.components.AddDriverDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    drivers: List<Driver>,
    vehicles: List<Vehicle>,
    selectedDriverId: Long?,
    activeShift: AttendanceRecord?,
    records: List<AttendanceRecord>,
    onSelectDriver: (Long) -> Unit,
    onPunchIn: (Driver, Vehicle, Double, String, String) -> Unit,
    onPunchOut: (AttendanceRecord, Double, String) -> Unit,
    onAddDriver: (String, String, String, Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDriver = drivers.find { it.id == selectedDriverId } ?: drivers.firstOrNull()

    var showAddDriverDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Punch in form states
    var selectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var punchInOdometerText by remember { mutableStateOf("") }
    var shiftType by remember { mutableStateOf("Full Day") }
    var punchInNotes by remember { mutableStateOf("") }

    // Punch out form states
    var punchOutOdometerText by remember { mutableStateOf("") }
    var punchOutNotes by remember { mutableStateOf("") }

    // Sync vehicle selection with driver default
    LaunchedEffect(currentDriver, vehicles) {
        if (selectedVehicleId == null && vehicles.isNotEmpty()) {
            val matched = vehicles.find { it.plateNumber == currentDriver?.defaultVehiclePlate }
            val v = matched ?: vehicles.first()
            selectedVehicleId = v.id
            punchInOdometerText = v.currentOdometerKm.toInt().toString()
        }
    }

    val selectedVehicle = vehicles.find { it.id == selectedVehicleId } ?: vehicles.firstOrNull()

    // Live clock for punch in/out
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("hh:mm:ss a, dd MMM yyyy", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(1000)
        }
    }

    val filteredRecords = remember(records, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> records.filter { it.status == AttendanceStatus.PENDING_APPROVAL }
            "FINALIZED" -> records.filter { it.status == AttendanceStatus.FINALIZED }
            "REJECTED" -> records.filter { it.status == AttendanceStatus.REJECTED }
            else -> records
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver Selector & Profile Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Driver Profile",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark
                        )
                        TextButton(
                            onClick = { showAddDriverDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Driver", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Drivers Scroll
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        drivers.forEach { driver ->
                            val isSelected = driver.id == currentDriver?.id
                            Surface(
                                onClick = {
                                    onSelectDriver(driver.id)
                                    val matched = vehicles.find { it.plateNumber == driver.defaultVehiclePlate }
                                    if (matched != null) {
                                        selectedVehicleId = matched.id
                                        punchInOdometerText = matched.currentOdometerKm.toInt().toString()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) DriverAmberLight else Color(0xFFF1F5F9),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, DriverAmberPrimary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("driver_chip_${driver.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) DriverAmberPrimary else SlateMedium),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = driver.name.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = driver.name.split(" ").first(),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) DriverAmberDark else TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }

                    currentDriver?.let { d ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = d.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "Lic: ${d.licenseNumber} • Phone: ${d.phone}",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${d.wagePerDay.toInt()}/day",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = StatusSuccessGreen
                                    )
                                    Text(
                                        text = "OT: ₹${d.overtimeHourlyRate.toInt()}/hr",
                                        fontSize = 10.sp,
                                        color = TextTertiaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Shift Action Card (Punch In or Punch Out)
        item {
            if (activeShift == null) {
                // ================= PUNCH IN CARD =================
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DriverAmberLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = DriverAmberPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Mark Today's Attendance",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = SlateDark
                                    )
                                    Text(
                                        text = currentTimeString.ifBlank { "Live Time Sync" },
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Vehicle Selector
                        Text(
                            text = "Assigned Fleet Vehicle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vehicles.forEach { v ->
                                val isSelected = v.id == selectedVehicleId
                                Surface(
                                    onClick = {
                                        selectedVehicleId = v.id
                                        punchInOdometerText = v.currentOdometerKm.toInt().toString()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, OwnerIndigoPrimary) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("vehicle_chip_${v.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = if (isSelected) OwnerIndigoPrimary else SlateLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = v.plateNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) OwnerIndigoPrimary else TextPrimaryDark
                                        )
                                        Text(
                                            text = "${v.currentOdometerKm.toInt()} km",
                                            fontSize = 10.sp,
                                            color = TextTertiaryDark
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Start Odometer & Shift Type
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = punchInOdometerText,
                                onValueChange = { punchInOdometerText = it },
                                label = { Text("Start Odometer (km)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("driver_start_odometer_input")
                            )

                            // Shift Type Menu
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Shift Type", fontSize = 11.sp, color = TextSecondaryDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                var expanded by remember { mutableStateOf(false) }
                                OutlinedCard(
                                    onClick = { expanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(shiftType, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("Full Day", "Half Day", "Night Shift", "Long Haul").forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st) },
                                            onClick = {
                                                shiftType = st
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = punchInNotes,
                            onValueChange = { punchInNotes = it },
                            label = { Text("Starting Note / Route Plan (Optional)") },
                            placeholder = { Text("e.g. Depot pickup to North Market") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("driver_start_notes_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Punch-In Button
                        Button(
                            onClick = {
                                val driver = currentDriver ?: return@Button
                                val vehicle = selectedVehicle ?: return@Button
                                val odo = punchInOdometerText.toDoubleOrNull() ?: vehicle.currentOdometerKm
                                onPunchIn(driver, vehicle, odo, shiftType, punchInNotes)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DriverAmberPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("punch_in_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PUNCH IN / START DUTY",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            } else {
                // ================= PUNCH OUT / ACTIVE SHIFT CARD =================
                val shiftStartFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                val startOdo = activeShift.startOdometerKm
                val curOdoVal = punchOutOdometerText.toDoubleOrNull() ?: (startOdo + 45.0)
                val distKm = if (curOdoVal >= startOdo) (curOdoVal - startOdo) else 0.0

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(2.dp, DriverAmberPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(StatusActiveBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = StatusActiveBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Active Shift in Progress",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "Started at ${shiftStartFormat.format(Date(activeShift.checkInTimestamp))}",
                                        fontSize = 12.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                            StatusBadge(status = AttendanceStatus.PUNCHED_IN)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Active Vehicle & Odometer Info
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = activeShift.vehiclePlate,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = activeShift.vehicleModel,
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Start: ${startOdo.toInt()} km",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = activeShift.shiftType,
                                        fontSize = 11.sp,
                                        color = DriverAmberDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // End Odometer input
                        OutlinedTextField(
                            value = punchOutOdometerText,
                            onValueChange = { punchOutOdometerText = it },
                            label = { Text("Current / Finish Odometer (km) *") },
                            placeholder = { Text("e.g. ${(startOdo + 85).toInt()}") },
                            leadingIcon = {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            supportingText = {
                                Text("Calculated Trip Distance: ${distKm.toInt()} km")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("driver_end_odometer_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = punchOutNotes,
                            onValueChange = { punchOutNotes = it },
                            label = { Text("Closing Notes (Deliveries, Toll, Fuel)") },
                            placeholder = { Text("e.g. 8 stops done, fuel ₹500 added") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("driver_closing_notes_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val endOdo = punchOutOdometerText.toDoubleOrNull() ?: (startOdo + 50.0)
                                onPunchOut(activeShift, endOdo, punchOutNotes)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("punch_out_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PUNCH OUT & SEND FOR APPROVAL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Attendance History Header & Filter Chips
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Shift & Attendance History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SlateDark
                    )
                    Text(
                        text = "${filteredRecords.size} record(s)",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "PENDING", "FINALIZED", "REJECTED").forEach { f ->
                        FilterChip(
                            selected = selectedFilter == f,
                            onClick = { selectedFilter = f },
                            label = { Text(f, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = TextTertiaryDark,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No attendance records found for this filter",
                            fontSize = 13.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                DriverAttendanceCard(record = record)
            }
        }
    }

    if (showAddDriverDialog) {
        AddDriverDialog(
            onDismiss = { showAddDriverDialog = false },
            onAddDriver = onAddDriver
        )
    }
}

@Composable
fun DriverAttendanceCard(
    record: AttendanceRecord,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(14.dp),
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
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = record.dateString,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "• ${record.shiftType}",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
                StatusBadge(status = record.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vehicle: ${record.vehiclePlate}",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
                Text(
                    text = "${record.calculatedHours} hrs • ${record.calculatedDistanceKm.toInt()} km",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val inStr = timeFormat.format(Date(record.checkInTimestamp))
                val outStr = record.checkOutTimestamp?.let { timeFormat.format(Date(it)) } ?: "Active"
                Text(
                    text = "$inStr - $outStr",
                    fontSize = 11.sp,
                    color = TextTertiaryDark
                )
                if (record.status == AttendanceStatus.FINALIZED) {
                    Text(
                        text = "Payout: ₹${record.totalPayoutAmount.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccessGreen
                    )
                }
            }

            // Driver notes
            if (record.driverNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Driver: ${record.driverNotes}",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    maxLines = 2
                )
            }

            // Owner stamp
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
                            imageVector = if (record.status == AttendanceStatus.FINALIZED) Icons.Default.Verified else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (record.status == AttendanceStatus.FINALIZED) StatusSuccessGreen else StatusRejectedRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Owner: ${record.ownerRemarks}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (record.status == AttendanceStatus.FINALIZED) Color(0xFF065F46) else Color(0xFF991B1B)
                        )
                    }
                }
            }
        }
    }
}
