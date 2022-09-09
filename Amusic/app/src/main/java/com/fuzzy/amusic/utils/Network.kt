package com.fuzzy.amusic.utils

import android.os.Environment
import android.util.Log
import com.fuzzy.amusic.MainApplication
import com.fuzzy.amusic.MainApplication.getInstance
import com.fuzzy.amusic.database.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class Network {
    val context: MainApplication = getInstance()

    private fun download(url: String, path: String): Boolean {
        var success = true
        var connection: HttpURLConnection? = null

        val cachePath = context.getExternalFilesDir(null)!!.absolutePath + "/cache/" + File(path).nameWithoutExtension + "." + File(path).extension

        Log.i("Network.download", "download start, from url: $url, to path, $path")
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.readTimeout = 2000
            connection.connectTimeout = 2000
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    BufferedOutputStream(FileOutputStream(cachePath)).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            else {
                throw RuntimeException("network error, ${connection.responseCode}")
            }
            File(cachePath).copyTo(File(path))
            Log.i("Network.download", "download complete")
        }
        catch (e: Exception) {
            Log.e("Network.download", "download error", e)
            success = false
        }
        finally {
            connection?.disconnect()
        }
        return success
    }

    fun downloadSongsData() {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/song_database.csv"
        Network().download("https://csv-db.studio.asf.ink/song_database.csv", path)
    }

    fun downloadSong(song: Song) {
        download(song.toUrl(), song.toPath())
    }

    private fun fakeDownload(resourceInput: InputStream, path: String) {
        BufferedInputStream(resourceInput).use { input ->
            BufferedOutputStream(FileOutputStream(path)).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun fakeGetSong(songId: String) {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath + "/" + songId + ".mp3"
        val asset = context.assets.open("$songId.mp3")
        fakeDownload(asset, path)
    }

    fun fakeGetData() {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/song_database.csv"
        val asset = context.assets.open("song_database.csv")
        fakeDownload(asset, path)
    }
}
