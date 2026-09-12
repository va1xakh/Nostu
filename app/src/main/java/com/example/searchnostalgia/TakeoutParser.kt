package com.example.searchnostalgia

import android.content.Context
import android.net.Uri
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.time.Instant
import java.time.ZoneId

object TakeoutParser {
    suspend fun parseAndStore(
        context: Context,
        uri: Uri,
        dao: SearchDao,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext
        val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))

        val batch = mutableListOf<SearchRecord>()
        var count = 0

        reader.beginArray()
        while (reader.hasNext()) {
            var rawTitle = ""
            var timeString = ""

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "title" -> rawTitle = reader.nextString()
                    "time" -> timeString = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            if (rawTitle.startsWith("Searched for ")) {
                val cleanQuery = rawTitle.removePrefix("Searched for ").trim()
                try {
                    val instant = Instant.parse(timeString)
                    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()

                    batch.add(
                        SearchRecord(
                            query = cleanQuery,
                            timestamp = instant.toEpochMilli(),
                            year = localDate.year,
                            month = localDate.monthValue,
                            day = localDate.dayOfMonth
                        )
                    )
                } catch (_: Exception) {}
            }

            if (batch.size >= 1000) {
                dao.insertBatch(batch)
                count += batch.size
                withContext(Dispatchers.Main) { onProgress(count) }
                batch.clear()
            }
        }
        reader.endArray()

        if (batch.isNotEmpty()) {
            dao.insertBatch(batch)
            count += batch.size
            withContext(Dispatchers.Main) { onProgress(count) }
            batch.clear()
        }
        reader.close()
    }
}
