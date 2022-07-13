package io.github.alexandrepiveteau.echo.indexeddb

import com.juul.indexeddb.Database
import com.juul.indexeddb.openDatabase

/** The name of the database where events are stored. */
private const val Name = "echo"

/** The version of the schema. */
private const val Version = 2

/** Opens and returns the [Database] to be used to store events. */
internal suspend fun openDatabase(): Database =
    openDatabase(Name, Version) { database, oldVersion, _ ->
      if (oldVersion != Version) {
        database.deleteObjectStore(EventsStore)
        database.createObjectStore(EventsStore, EventIdPath)
      }
    }
