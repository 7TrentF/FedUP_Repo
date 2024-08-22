package com.example.fedup_foodwasteapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlin.concurrent.Volatile

// This annotation defines the database, indicating that it uses the `Ingredients` entity and
// specifies the database version as 1. The `exportSchema` parameter is set to false, meaning
// the database schema won't be exported as a JSON file.
@Database(entities = [Ingredients::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // This abstract function provides an instance of the DAO (Data Access Object) for `Ingredients`.
    // DAOs are responsible for defining the methods that access the database.
    abstract fun ingredientDao(): IngredientDao

    companion object {
        // The `@Volatile` annotation ensures that the value of `INSTANCE` is always up-to-date
        // and visible to all threads. This prevents caching issues that might cause one thread
        // to see a stale version of the `INSTANCE` variable.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // This function returns the singleton instance of `AppDatabase`. If the instance
        // doesn't exist yet, it creates one using the Room database builder.
        // The `synchronized` block ensures that only one thread can initialize the database
        // at a time, preventing multiple instances from being created.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // If INSTANCE is `null`, a new database instance is created using Room's
                // `databaseBuilder`. The `context.applicationContext` ensures that the database
                // is tied to the application's lifecycle, not an activity or other component.
                // The `AppDatabase::class.java` tells Room to create an instance of this class,
                // and `"app_database"` specifies the name of the database file.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()

                // The newly created instance is then assigned to `INSTANCE`, ensuring that the
                // next time this function is called, the same database instance is returned.
                INSTANCE = instance

                // Finally, the newly created instance is returned.
                instance
            }
        }
    }
}
