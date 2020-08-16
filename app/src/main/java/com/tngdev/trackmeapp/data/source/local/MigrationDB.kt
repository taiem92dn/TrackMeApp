package com.tngdev.trackmeapp.data.source.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationDB {
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE sessions " +
                            "ADD COLUMN ignoreLocationsAfterPauseCount INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}