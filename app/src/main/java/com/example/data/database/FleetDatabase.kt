package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FleetDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Driver
import com.example.data.model.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [Driver::class, Vehicle::class, AttendanceRecord::class],
    version = 1,
    exportSchema = false
)
abstract class FleetDatabase : RoomDatabase() {

    abstract fun fleetDao(): FleetDao

    companion object {
        @Volatile
        private var INSTANCE: FleetDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FleetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FleetDatabase::class.java,
                    "fleet_pulse_database"
                )
                    .addCallback(FleetDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class FleetDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.fleetDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: FleetDao) {
            val d1 = Driver(
                id = 1,
                name = "Rajesh Kumar",
                phone = "+91 98765 43210",
                licenseNumber = "DL-0420190012431",
                defaultVehiclePlate = "MH-12-RN-4821",
                wagePerDay = 850.0,
                overtimeHourlyRate = 120.0,
                isActive = true,
                joinedDate = "2023-04-12"
            )
            val d2 = Driver(
                id = 2,
                name = "Amit Singh",
                phone = "+91 98220 11984",
                licenseNumber = "MH-1420210088912",
                defaultVehiclePlate = "DL-01-AX-9022",
                wagePerDay = 800.0,
                overtimeHourlyRate = 100.0,
                isActive = true,
                joinedDate = "2023-08-20"
            )
            val d3 = Driver(
                id = 3,
                name = "Vikram Patel",
                phone = "+91 97401 22345",
                licenseNumber = "KA-0120180054719",
                defaultVehiclePlate = "KA-04-MB-1100",
                wagePerDay = 900.0,
                overtimeHourlyRate = 130.0,
                isActive = true,
                joinedDate = "2022-11-05"
            )
            dao.insertDrivers(listOf(d1, d2, d3))

            val v1 = Vehicle(
                id = 1,
                plateNumber = "MH-12-RN-4821",
                modelName = "Tata 407 Heavy Truck",
                vehicleType = "Truck",
                currentOdometerKm = 48390.0,
                fuelType = "Diesel",
                isActive = true
            )
            val v2 = Vehicle(
                id = 2,
                plateNumber = "DL-01-AX-9022",
                modelName = "Mahindra Bolero Maxi Truck",
                vehicleType = "Pickup",
                currentOdometerKm = 32205.0,
                fuelType = "Diesel",
                isActive = true
            )
            val v3 = Vehicle(
                id = 3,
                plateNumber = "KA-04-MB-1100",
                modelName = "Ashok Leyland Dost XL",
                vehicleType = "Van",
                currentOdometerKm = 19840.0,
                fuelType = "Diesel",
                isActive = true
            )
            dao.insertVehicles(listOf(v1, v2, v3))

            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val hourMs = 3600000L
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Record 1: Yesterday finalized
            val yesterdayDate = dateFormat.format(Date(now - dayMs))
            val rec1 = AttendanceRecord(
                id = 1,
                driverId = 1,
                driverName = "Rajesh Kumar",
                vehicleId = 1,
                vehiclePlate = "MH-12-RN-4821",
                vehicleModel = "Tata 407 Heavy Truck",
                dateString = yesterdayDate,
                checkInTimestamp = now - dayMs - 9 * hourMs,
                checkOutTimestamp = now - dayMs - hourMs,
                startOdometerKm = 48250.0,
                endOdometerKm = 48390.0,
                shiftType = "Full Day",
                status = AttendanceStatus.FINALIZED,
                driverNotes = "Delivered goods across 5 warehouse stops. Vehicle refueled at Central Bunk.",
                ownerRemarks = "Verified odometer (140 km) and diesel receipt. Approved with ₹150 lunch allowance.",
                finalizedTimestamp = now - dayMs + 3600000L,
                calculatedHours = 8.0,
                calculatedDistanceKm = 140.0,
                overtimeHours = 0.5,
                dailyAllowance = 150.0,
                totalPayoutAmount = 850.0 + (0.5 * 120.0) + 150.0
            )

            // Record 2: Today morning pending approval
            val todayDate = dateFormat.format(Date(now))
            val rec2 = AttendanceRecord(
                id = 2,
                driverId = 2,
                driverName = "Amit Singh",
                vehicleId = 2,
                vehiclePlate = "DL-01-AX-9022",
                vehicleModel = "Mahindra Bolero Maxi Truck",
                dateString = todayDate,
                checkInTimestamp = now - 7 * hourMs,
                checkOutTimestamp = now - 30 * 60 * 1000L,
                startOdometerKm = 32110.0,
                endOdometerKm = 32205.0,
                shiftType = "Full Day",
                status = AttendanceStatus.PENDING_APPROVAL,
                driverNotes = "City delivery batch #4. Toll tax paid ₹80. Odometer end photo submitted.",
                ownerRemarks = "",
                finalizedTimestamp = null,
                calculatedHours = 6.5,
                calculatedDistanceKm = 95.0,
                overtimeHours = 0.0,
                dailyAllowance = 80.0,
                totalPayoutAmount = 800.0 + 80.0
            )

            dao.insertAttendanceRecords(listOf(rec1, rec2))
        }
    }
}
