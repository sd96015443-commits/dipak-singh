package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class Driver(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val licenseNumber: String,
    val defaultVehiclePlate: String = "",
    val wagePerDay: Double = 800.0,
    val overtimeHourlyRate: Double = 100.0,
    val isActive: Boolean = true,
    val joinedDate: String = "2024-01-15"
)

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plateNumber: String,
    val modelName: String,
    val vehicleType: String = "Truck", // Truck, Van, Pickup, Mini-Bus
    val currentOdometerKm: Double = 10000.0,
    val fuelType: String = "Diesel",
    val isActive: Boolean = true
)

object AttendanceStatus {
    const val PUNCHED_IN = "PUNCHED_IN"         // Currently on duty
    const val PENDING_APPROVAL = "PENDING_APPROVAL" // Driver punched out, waiting for owner
    const val FINALIZED = "FINALIZED"           // Owner verified and approved
    const val REJECTED = "REJECTED"             // Owner rejected or asked for rework
}

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val driverId: Long,
    val driverName: String,
    val vehicleId: Long,
    val vehiclePlate: String,
    val vehicleModel: String,
    val dateString: String, // YYYY-MM-DD
    val checkInTimestamp: Long,
    val checkOutTimestamp: Long? = null,
    val startOdometerKm: Double,
    val endOdometerKm: Double? = null,
    val shiftType: String = "Full Day", // Full Day, Half Day, Night Shift, Long Haul
    val status: String = AttendanceStatus.PUNCHED_IN,
    val driverNotes: String = "",
    val ownerRemarks: String = "",
    val finalizedTimestamp: Long? = null,
    val calculatedHours: Double = 0.0,
    val calculatedDistanceKm: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val dailyAllowance: Double = 0.0,
    val totalPayoutAmount: Double = 0.0
)
