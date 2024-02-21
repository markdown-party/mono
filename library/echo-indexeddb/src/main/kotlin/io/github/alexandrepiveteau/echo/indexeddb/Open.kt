package io.github.alexandrepiveteau.echo.indexeddb

import com.juul.indexeddb.Database
import com.juul.indexeddb.VersionChangeTransaction
import com.juul.indexeddb.openDatabase

/** The name of the database where events are stored. */
private const val Name = "echo"

/** The version of the schema. */
private const val Version = 3

/** Opens and returns the [Database] to be used to store events. */
internal suspend fun openDatabase(): Database =
    openDatabase(Name, Version) { database, oldVersion, _ ->
      if (oldVersion < 2) from0To2(database)
      if (oldVersion < 3) from2To3()
    }

// UPGRADE SCRIPTS

private fun VersionChangeTransaction.from0To2(
    database: Database,
) = with(database) { createObjectStore(EventsStore, EventIdPath) }

private fun VersionChangeTransaction.from2To3() =
    objectStore(EventsStore).createIndex(SessionIndex, SessionPath, unique = false)
