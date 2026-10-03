package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceRecord
import com.example.data.model.Driver
import com.example.data.model.Vehicle
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinalizeAttendanceDialog(
    record: AttendanceRecord,
    driver: Driver?,
    onDismiss: () -> Unit,
    onFinalize: (remarks: String, overtimeHours: Double, allowance: Double, totalPayout: Double) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val baseWage = driver?.wagePerDay ?: 800.0
    val otHourlyRate = driver?.overtimeHourlyRate ?: 100.0

    var overtimeInput by remember { mutableStateOf(if (record.overtimeHours > 0) record.overtimeHours.toString() else "0.0") }
    var allowanceInput by remember { mutableStateOf(if (record.dailyAllowance > 0) record.dailyAllowance.toString() else "100.0") }
    var remarksInput by remember { mutableStateOf(record.ownerRemarks.ifBlank { "Trip and odometer logs verified. Approved." }) }

    val parsedOt = overtimeInput.toDoubleOrNull() ?: 0.0
    val parsedAllowance = allowanceInput.toDoubleOrNull() ?: 0.0
    val totalCalculatedPayout = baseWage + (parsedOt * otHourlyRate) + parsedAllowance

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Finalize Attendance",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateDark
                        )
                        Text(
                            text = "Vehicle Owner Verification",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Shift Summary Box
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = record.driverName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimaryDark
                            )
                            StatusBadge(status = record.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

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
                                text = record.shiftType,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = OwnerIndigoPrimary
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Check-In", fontSize = 11.sp, color = TextTertiaryDark)
                                Text(
                                    timeFormat.format(Date(record.checkInTimestamp)),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text("Check-Out", fontSize = 11.sp, color = TextTertiaryDark)
                                Text(
                                    record.checkOutTimestamp?.let { timeFormat.format(Date(it)) } ?: "--:--",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text("Hours", fontSize = 11.sp, color = TextTertiaryDark)
                                Text(
                                    "${record.calculatedHours} hrs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = StatusSuccessGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Odometer: ${record.startOdometerKm.toInt()} → ${record.endOdometerKm?.toInt() ?: "--"} km",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "Dist: ${record.calculatedDistanceKm.toInt()} km",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        if (record.driverNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Driver Note: ${record.driverNotes}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E3A8A),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Owner adjustments
                Text(
                    text = "Owner Verification & Wage Finalization",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = overtimeInput,
                        onValueChange = { overtimeInput = it },
                        label = { Text("OT (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finalize_ot_input")
                    )

                    OutlinedTextField(
                        value = allowanceInput,
                        onValueChange = { allowanceInput = it },
                        label = { Text("Allowance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finalize_allowance_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = remarksInput,
                    onValueChange = { remarksInput = it },
                    label = { Text("Owner Verification Remarks / Stamp") },
                    placeholder = { Text("e.g., Verified trip, approved payment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finalize_remarks_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Calculated Pay Card
                Surface(
                    color = StatusSuccessBg,
                    shape = RoundedCornerShape(10.dp),
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
                                text = "Final Payout to Driver",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusSuccessGreen
                            )
                            Text(
                                text = "Base (₹${baseWage.toInt()}) + OT (₹${(parsedOt * otHourlyRate).toInt()}) + Allowance (₹${parsedAllowance.toInt()})",
                                fontSize = 10.sp,
                                color = Color(0xFF065F46)
                            )
                        }
                        Text(
                            text = "₹${totalCalculatedPayout.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = StatusSuccessGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onFinalize(
                                remarksInput,
                                parsedOt,
                                parsedAllowance,
                                totalCalculatedPayout
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccessGreen),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_finalize_button")
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finalize & Approve")
                    }
                }
            }
        }
    }
}

@Composable
fun RejectAttendanceDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onConfirmReject: (reason: String) -> Unit
) {
    var reasonInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Request Revision / Reject", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Request ${record.driverName} to correct attendance for ${record.dateString}.",
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Reason for Rejection / Revision") },
                    placeholder = { Text("e.g., Odometer reading mismatch, extra hours not verified") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reject_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReject(reasonInput) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                modifier = Modifier.testTag("confirm_reject_button")
            ) {
                Text("Reject Attendance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddDriverDialog(
    onDismiss: () -> Unit,
    onAddDriver: (name: String, phone: String, license: String, wage: Double, otRate: Double, plate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }
    var wage by remember { mutableStateOf("800") }
    var otRate by remember { mutableStateOf("100") }
    var plate by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Register New Driver",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Driver Full Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("add_driver_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("add_driver_phone_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = license,
                    onValueChange = { license = it },
                    label = { Text("Commercial Driving License # *") },
                    modifier = Modifier.fillMaxWidth().testTag("add_driver_license_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = wage,
                        onValueChange = { wage = it },
                        label = { Text("Daily Wage (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("add_driver_wage_input")
                    )
                    OutlinedTextField(
                        value = otRate,
                        onValueChange = { otRate = it },
                        label = { Text("OT (₹/hr)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("add_driver_ot_input")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text("Assigned Vehicle Plate (Optional)") },
                    placeholder = { Text("e.g. MH-12-RN-4821") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please fill in name, phone, and license number",
                        color = StatusRejectedRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isBlank() || phone.isBlank() || license.isBlank()) {
                                showError = true
                            } else {
                                onAddDriver(
                                    name,
                                    phone,
                                    license,
                                    wage.toDoubleOrNull() ?: 800.0,
                                    otRate.toDoubleOrNull() ?: 100.0,
                                    plate
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                        modifier = Modifier.weight(1.5f).testTag("confirm_add_driver_btn")
                    ) {
                        Text("Add Driver")
                    }
                }
            }
        }
    }
}

@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onAddVehicle: (plate: String, model: String, type: String, currentOdo: Double, fuel: String) -> Unit
) {
    var plate by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Truck") }
    var odometer by remember { mutableStateOf("15000") }
    var fuel by remember { mutableStateOf("Diesel") }

    var showError by remember { mutableStateOf(false) }

    val vehicleTypes = listOf("Truck", "Pickup", "Van", "Mini-Bus", "Trailer")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Fleet Vehicle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text("Registration Plate # *") },
                    placeholder = { Text("e.g. MH-12-AB-9876") },
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_plate_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Vehicle Model *") },
                    placeholder = { Text("e.g. Tata 407, Bolero Maxi") },
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_model_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Vehicle Type", fontSize = 12.sp, color = TextSecondaryDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vehicleTypes.take(3).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text("Current Odometer (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_vehicle_odo_input")
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter valid plate number and model name",
                        color = StatusRejectedRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (plate.isBlank() || model.isBlank()) {
                                showError = true
                            } else {
                                onAddVehicle(
                                    plate,
                                    model,
                                    type,
                                    odometer.toDoubleOrNull() ?: 0.0,
                                    fuel
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                        modifier = Modifier.weight(1.5f).testTag("confirm_add_vehicle_btn")
                    ) {
                        Text("Add Vehicle")
                    }
                }
            }
        }
    }
}
