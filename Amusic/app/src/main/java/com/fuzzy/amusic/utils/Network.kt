package com.fuzzy.amusic.utils

import android.os.Environment
import android.util.Log
import com.fuzzy.amusic.MainApplication
import com.fuzzy.amusic.MainApplication.getInstance
import com.fuzzy.amusic.database.Song
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class Network {
    val context: MainApplication = getInstance()

    private fun download(url: String, path: String, onComplete: (Boolean) -> Unit = {}): Boolean {
        var success = true
        var connection: HttpURLConnection? = null

        val cacheDir: String = context.getExternalFilesDir(null)!!.absolutePath + "/cache"
        if (!File(cacheDir).isDirectory) {
            Log.i("Network / download", "cache dir does not exist, will create it")
            File(cacheDir).mkdirs()
        }

        val cachePath: String = cacheDir + "/" + File(path).name
        if (File(cachePath).isFile) {
            Log.i("Network / download", "cache file exists, will remove it")
            File(cachePath).delete()
        }

        Log.i("Network / download", "download start, from url: $url, to path: $path, cache file in: $cachePath")
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.readTimeout = 2000
            connection.connectTimeout = 2000
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                File(cachePath).createNewFile()
                connection.inputStream.use { input ->
                    BufferedOutputStream(FileOutputStream(cachePath)).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            else {
                throw RuntimeException("network error, ${connection.responseCode}")
            }
            if (File(path).isFile) {
                Log.i("Network / download", "target file exists, will remove it")
                File(path).delete()
            }
            File(cachePath).copyTo(File(path))
            File(cachePath).delete()
            Log.i("Network / download", "download $path complete")
        }
        catch (e: Exception) {
            Log.e("Network / download", "download error", e)
            success = false
        }
        finally {
            connection?.disconnect()
            onComplete(success)
        }
        return success
    }

    suspend fun downloadSongsData() {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/song_database.csv"
        Network().download("https://csv-db.studio.asf.ink/song_database.csv", path)
    }

    suspend fun downloadSong(song: Song): Boolean {
        return download(song.toUrl(), song.toPath())
    }
}
