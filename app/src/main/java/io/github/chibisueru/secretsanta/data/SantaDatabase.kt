package io.github.chibisueru.secretsanta.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GroupEntity::class, ParticipantEntity::class, ExclusionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SantaDatabase : RoomDatabase() {
    abstract fun dao(): SantaDao

    companion object {
        fun create(context: Context): SantaDatabase =
            Room.databaseBuilder(context, SantaDatabase::class.java, "secret_santa.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v2: organizer details on groups and an organizer flag on participants. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE santa_groups ADD COLUMN organizerName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE santa_groups ADD COLUMN organizerPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE santa_groups ADD COLUMN organizerEmail TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE participants ADD COLUMN isOrganizer INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
