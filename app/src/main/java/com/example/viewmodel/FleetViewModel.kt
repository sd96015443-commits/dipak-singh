package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.dao.FleetDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Driver
import com.example.data.model.Vehicle
import com.example.data.repository.FleetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

enum class AppRole {
    DRIVER,
    OWNER
}

class FleetViewModel(
    private val repository: FleetRepository,
    private val dao: FleetDao
) : ViewModel() {

    // App Role State
    private val _currentRole = MutableStateFlow(AppRole.DRIVER)
    val currentRole: StateFlow<AppRole> = _currentRole.asStateFlow()

    // Fleet Data
    val allDrivers: StateFlow<List<Driver>> = repository.allDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<AttendanceRecord>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAttendance: StateFlow<List<AttendanceRecord>> = repository.pendingAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Driver for Driver Mode
    private val _selectedDriverId = MutableStateFlow<Long?>(null)
    val selectedDriverId: StateFlow<Long?> = _selectedDriverId.asStateFlow()

    // Active shift for current driver
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDriverActiveShift: StateFlow<AttendanceRecord?> = _selectedDriverId
        .flatMapLatest { id ->
            if (id != null) repository.getActiveShift(id)
            else kotlinx.coroutines.flow.flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Attendance records for current driver
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDriverRecords: StateFlow<List<AttendanceRecord>> = _selectedDriverId
        .flatMapLatest { id ->
            if (id != null) repository.getAttendanceForDriver(id)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notification / Toast event channel
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        // Automatically select the first driver once drivers are loaded
        viewModelScope.launch {
            repository.allDrivers.collect { drivers ->
                if (_selectedDriverId.value == null && drivers.isNotEmpty()) {
                    _selectedDriverId.value = drivers.first().id
                }
            }
        }
    }

    fun setRole(role: AppRole) {
        _currentRole.value = role
        viewModelScope.launch {
            _snackbarMessage.emit("Switched to ${if (role == AppRole.DRIVER) "Driver Portal" else "Vehicle Owner Portal"}")
        }
    }

    fun selectDriver(driverId: Long) {
        _selectedDriverId.value = driverId
    }

    fun punchIn(
        driver: Driver,
        vehicle: Vehicle,
        startOdo: Double,
        shiftType: String,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                repository.punchIn(
                    driver = driver,
                    vehicle = vehicle,
                    startOdometerKm = startOdo,
                    shiftType = shiftType,
                    notes = notes
                )
                _snackbarMessage.emit("Attendance marked! Shift started for ${driver.name} on ${vehicle.plateNumber}")
            } catch (e: Exception) {
                _snackbarMessage.emit("Error marking attendance: ${e.message}")
            }
        }
    }

    fun punchOut(
        record: AttendanceRecord,
        endOdo: Double,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                repository.punchOut(record, endOdo, notes)
                _snackbarMessage.emit("Shift completed! Attendance sent to Vehicle Owner for review.")
            } catch (e: Exception) {
                _snackbarMessage.emit("Error punching out: ${e.message}")
            }
        }
    }

    fun finalizeAttendance(
        recordId: Long,
        remarks: String,
        overtimeHours: Double,
        allowance: Double,
        totalPayout: Double
    ) {
        viewModelScope.launch {
            try {
                repository.finalizeAttendance(
                    recordId = recordId,
                    remarks = remarks.ifBlank { "Approved and verified by Fleet Owner." },
                    overtimeHours = overtimeHours,
                    allowance = allowance,
                    totalPayout = totalPayout
                )
                _snackbarMessage.emit("Attendance finalized and approved!")
            } catch (e: Exception) {
                _snackbarMessage.emit("Error finalizing attendance: ${e.message}")
            }
        }
    }

    fun rejectAttendance(recordId: Long, reason: String) {
        viewModelScope.launch {
            try {
                repository.rejectAttendance(
                    recordId = recordId,
                    reason = reason.ifBlank { "Rejected / Needs correction in hours or odometer." }
                )
                _snackbarMessage.emit("Attendance marked as rejected / correction required.")
            } catch (e: Exception) {
                _snackbarMessage.emit("Error rejecting record: ${e.message}")
            }
        }
    }

    fun bulkFinalizeAllPending(defaultRemarks: String = "Bulk verified and finalized by Fleet Owner") {
        viewModelScope.launch {
            try {
                val pendingList = pendingAttendance.value
                var count = 0
                for (rec in pendingList) {
                    val driver = dao.getDriverById(rec.driverId)
                    val baseWage = driver?.wagePerDay ?: 800.0
                    val otRate = driver?.overtimeHourlyRate ?: 100.0
                    val totalPay = baseWage + (rec.overtimeHours * otRate) + rec.dailyAllowance
                    repository.finalizeAttendance(
                        recordId = rec.id,
                        remarks = defaultRemarks,
                        overtimeHours = rec.overtimeHours,
                        allowance = rec.dailyAllowance,
                        totalPayout = totalPay
                    )
                    count++
                }
                _snackbarMessage.emit("Finalized $count pending attendance record(s)!")
            } catch (e: Exception) {
                _snackbarMessage.emit("Error during bulk finalize: ${e.message}")
            }
        }
    }

    fun addDriver(
        name: String,
        phone: String,
        license: String,
        wagePerDay: Double,
        overtimeRate: Double,
        defaultPlate: String
    ) {
        viewModelScope.launch {
            try {
                val newDriver = Driver(
                    name = name.trim(),
                    phone = phone.trim(),
                    licenseNumber = license.trim().uppercase(),
                    wagePerDay = wagePerDay,
                    overtimeHourlyRate = overtimeRate,
                    defaultVehiclePlate = defaultPlate.trim(),
                    joinedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                )
                val newId = repository.insertDriver(newDriver)
                _selectedDriverId.value = newId
                _snackbarMessage.emit("Driver ${newDriver.name} added successfully!")
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to add driver: ${e.message}")
            }
        }
    }

    fun addVehicle(
        plate: String,
        model: String,
        type: String,
        currentOdometer: Double,
        fuelType: String
    ) {
        viewModelScope.launch {
            try {
                val newVehicle = Vehicle(
                    plateNumber = plate.trim().uppercase(),
                    modelName = model.trim(),
                    vehicleType = type,
                    currentOdometerKm = currentOdometer,
                    fuelType = fuelType
                )
                repository.insertVehicle(newVehicle)
                _snackbarMessage.emit("Vehicle ${newVehicle.plateNumber} added to fleet!")
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to add vehicle: ${e.message}")
            }
        }
    }
}

class FleetViewModelFactory(
    private val repository: FleetRepository,
    private val dao: FleetDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FleetViewModel::class.java)) {
            return FleetViewModel(repository, dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
