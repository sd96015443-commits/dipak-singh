package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Driver
import com.example.data.model.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface FleetDao {

    // Drivers
    @Query("SELECT * FROM drivers ORDER BY name ASC")
    fun getAllDrivers(): Flow<List<Driver>>

    @Query("SELECT * FROM drivers WHERE id = :id LIMIT 1")
    suspend fun getDriverById(id: Long): Driver?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: Driver): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(drivers: List<Driver>)

    @Update
    suspend fun updateDriver(driver: Driver)

    @Query("DELETE FROM drivers WHERE id = :id")
    suspend fun deleteDriver(id: Long)

    // Vehicles
    @Query("SELECT * FROM vehicles ORDER BY plateNumber ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: Long): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<Vehicle>)

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET currentOdometerKm = :newOdo WHERE id = :vehicleId")
    suspend fun updateVehicleOdometer(vehicleId: Long, newOdo: Double)

    // Attendance
    @Query("SELECT * FROM attendance_records ORDER BY checkInTimestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE driverId = :driverId ORDER BY checkInTimestamp DESC")
    fun getAttendanceForDriver(driverId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE status = :status ORDER BY checkInTimestamp DESC")
    fun getAttendanceByStatus(status: String = AttendanceStatus.PENDING_APPROVAL): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE driverId = :driverId AND status = :status LIMIT 1")
    fun getActiveShiftForDriver(driverId: Long, status: String = AttendanceStatus.PUNCHED_IN): Flow<AttendanceRecord?>

    @Query("SELECT * FROM attendance_records WHERE id = :id LIMIT 1")
    suspend fun getAttendanceById(id: Long): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendanceRecord: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(attendanceRecord: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteAttendance(id: Long)

    @Query("UPDATE attendance_records SET status = :status, ownerRemarks = :remarks, finalizedTimestamp = :finalizedTime, overtimeHours = :overtime, dailyAllowance = :allowance, totalPayoutAmount = :totalPayout WHERE id = :recordId")
    suspend fun finalizeRecord(
        recordId: Long,
        status: String,
        remarks: String,
        finalizedTime: Long,
        overtime: Double,
        allowance: Double,
        totalPayout: Double
    )
}
