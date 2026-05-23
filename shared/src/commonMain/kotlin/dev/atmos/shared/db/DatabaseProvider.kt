package dev.atmos.shared.db

/**
 * Singleton holder for the SQLDelight [AtmosDatabase].
 *
 * Call [init] once at app startup (before any repository is used):
 *   - Android: inside TripDetectorHolder.init() / MainActivity.onCreate()
 *   - iOS: inside MainViewController()
 *
 * Access the database anywhere via [DatabaseProvider.database].
 */
object DatabaseProvider {

    private var _database: AtmosDatabase? = null

    fun init(factory: DatabaseDriverFactory) {
        if (_database == null) {
            _database = AtmosDatabase(factory.create())
        }
    }

    val database: AtmosDatabase
        get() = _database
            ?: error("DatabaseProvider not initialised — call DatabaseProvider.init() first")
}
