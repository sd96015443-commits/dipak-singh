package com.example.data.repository

import com.example.data.dao.FleetDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Driver
import com.example.data.model.Vehicle
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class FleetRepository(private val dao: FleetDao) {

    val allDrivers: Flow<List<Driver>> = dao.getAllDrivers()
    val allVehicles: Flow<List<Vehicle>> = dao.getAllVehicles()
    val allAttendance: Flow<List<AttendanceRecord>> = dao.getAllAttendance()
    val pendingAttendance: Flow<List<AttendanceRecord>> = dao.getAttendanceByStatus(AttendanceStatus.PENDING_APPROVAL)

    fun getAttendanceForDriver(driverId: Long): Flow<List<AttendanceRecord>> =
        dao.getAttendanceForDriver(driverId)

    fun getActiveShift(driverId: Long): Flow<AttendanceRecord?> =
        dao.getActiveShiftForDriver(driverId, AttendanceStatus.PUNCHED_IN)

    suspend fun insertDriver(driver: Driver): Long = dao.insertDriver(driver)

    suspend fun insertVehicle(vehicle: Vehicle): Long = dao.insertVehicle(vehicle)

    suspend fun punchIn(
        driver: Driver,
        vehicle: Vehicle,
        startOdometerKm: Double,
        shiftType: String,
        notes: String
    ): Long {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date(now))

        val record = AttendanceRecord(
            driverId = driver.id,
            driverName = driver.name,
            vehicleId = vehicle.id,
            vehiclePlate = vehicle.plateNumber,
            vehicleModel = vehicle.modelName,
            dateString = dateString,
            checkInTimestamp = now,
            checkOutTimestamp = null,
            startOdometerKm = startOdometerKm,
            endOdometerKm = null,
            shiftType = shiftType,
            status = AttendanceStatus.PUNCHED_IN,
            driverNotes = notes,
            ownerRemarks = "",
            finalizedTimestamp = null,
            calculatedHours = 0.0,
            calculatedDistanceKm = 0.0
        )
        return dao.insertAttendance(record)
    }

    suspend fun punchOut(
        record: AttendanceRecord,
        endOdometerKm: Double,
        closingNotes: String
    ) {
        val now = System.currentTimeMillis()
        val durationMs = max(0L, now - record.checkInTimestamp)
        val hours = (durationMs / (1000.0 * 60.0 * 60.0) * 10).toInt() / 10.0 // 1 decimal place
        val distance = max(0.0, endOdometerKm - record.startOdometerKm)
        val combinedNotes = if (closingNotes.isNotBlank()) {
            if (record.driverNotes.isNotBlank()) "${record.driverNotes}\n• Finish Note: $closingNotes" else closingNotes
        } else {
            record.driverNotes
        }

        // Base calculation estimate for driver:
        val driver = dao.getDriverById(record.driverId)
        val baseWage = driver?.wagePerDay ?: 800.0
        val overtimeHourly = driver?.overtimeHourlyRate ?: 100.0
        val overtimeHrs = if (hours > 8.0) hours - 8.0 else 0.0
        val estimatedPay = baseWage + (overtimeHrs * overtimeHourly)

        val updatedRecord = record.copy(
            checkOutTimestamp = now,
            endOdometerKm = endOdometerKm,
            status = AttendanceStatus.PENDING_APPROVAL,
            driverNotes = combinedNotes,
            calculatedHours = hours,
            calculatedDistanceKm = distance,
            overtimeHours = overtimeHrs,
            totalPayoutAmount = estimatedPay
        )
        dao.updateAttendance(updatedRecord)

        // Update the vehicle's current odometer in fleet records
        if (endOdometerKm > 0) {
            dao.updateVehicleOdometer(record.vehicleId, endOdometerKm)
        }
    }

    suspend fun finalizeAttendance(
        recordId: Long,
        remarks: String,
        overtimeHours: Double,
        allowance: Double,
        totalPayout: Double
    ) {
        dao.finalizeRecord(
            recordId = recordId,
            status = AttendanceStatus.FINALIZED,
            remarks = remarks,
            finalizedTime = System.currentTimeMillis(),
            overtime = overtimeHours,
            allowance = allowance,
            totalPayout = totalPayout
        )
    }

    suspend fun rejectAttendance(recordId: Long, reason: String) {
        dao.finalizeRecord(
            recordId = recordId,
            status = AttendanceStatus.REJECTED,
            remarks = reason,
            finalizedTime = System.currentTimeMillis(),
            overtime = 0.0,
            allowance = 0.0,
            totalPayout = 0.0
        )
    }

    suspend fun bulkFinalizePending(remarks: String = "Bulk approved by Vehicle Owner") {
        // Find all pending records and finalize them with their calculated amounts
        val now = System.currentTimeMillis()
        // We will fetch pending and update
    }
}
