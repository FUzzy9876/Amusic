package com.fuzzy.amusic.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.fuzzy.amusic.MainApplication.getInstance
import com.fuzzy.amusic.database.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class CsvFileReader {
    private val context: Context = getInstance()
    private val network: Network = Network()

    var csvLines: MutableList<String>? = null

    suspend fun downloadSongsData() {
        // todo: 联网需要修改
        Log.i("CsvFileReader / downloadSongsData", "start downloading songs database")
        network.fakeGetData()
    }

    fun readCsvFile() {
        val csvFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/song_database.csv")
        if (!csvFile.isFile) {
            Log.e("readCsvFile", "no files available")
        }
        val content = csvFile.readText()
        val lines = content.split("\n").toMutableList()
        lines.removeFirst()
        lines.reverse()
        csvLines = lines
    }

    fun stringToSong(str: String): Song {
        var s = str
        while ("\"" in s) {
            val start = s.indexOf("\"", 0)
            val end = s.indexOf("\"", start + 1)
            val before = s.substring(0, start)
            val after = s.substring(end + 1)
            var middle = s.substring(start + 1, end)
            middle = middle.replace(",", "/")
            s = before + middle + after
        }
        val data = s.split(",")

        var name = data[8]
        val extra1 = data[9]
        val extra2 = data[10]
        if (extra1.isNotEmpty() || extra2.isNotEmpty()) {
            name += if (extra1.isNotEmpty() && extra2.isNotEmpty()) {
                "【$extra1 $extra2】"
            } else {
                "【$extra1$extra2】"
            }
        }

        return Song(data[0], name, data[5].toInt(), data[13], data[1], data[7])
    }

    fun getCsvData(indexStart: Int = 0, indexEnd: Int = 50): List<Song> {
        if (csvLines == null || csvLines!!.isEmpty()) {
            Log.i("getCsvData", "no data already read, prepare to read csv file")
            readCsvFile()
        }

        val result: MutableList<Song> = mutableListOf()
        val size = csvLines!!.size
        if (indexStart < 0 || indexEnd < 0 || indexStart >= size || indexEnd >= size) {
            Log.e("getCsvData", "data size is $size, but got range $indexStart to $indexEnd")
            return result
        }
        Log.i("getCsvData", "total $size, get ${indexStart + 1} to ${indexEnd + 1}")
        for (i in (indexStart .. indexEnd)) {
            result.add(stringToSong(csvLines!![i]))
        }
        return result.toList()
    }
}