package dev.atmos.shared.location

// ── actual factory ────────────────────────────────────────────────────────────

actual fun createTripDetector(): TripDetector = IosTripDetector()

// ── iOS stub — full state machine wired in task 2.4 ──────────────────────────

class IosTripDetector : TripDetector {
    override fun startMonitoring() = Unit
    override fun stopMonitoring() = Unit
    override fun requestPermissions() = Unit
    override fun manualEndAndSave() = Unit
    override fun discardSession() = Unit
    override fun handleTransition(event: VehicleActivity) = Unit
}
