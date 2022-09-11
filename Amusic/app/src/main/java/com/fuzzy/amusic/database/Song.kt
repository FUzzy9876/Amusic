package com.fuzzy.amusic.database

import android.os.Environment
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fuzzy.amusic.MainApplication
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URLEncoder
import kotlin.reflect.typeOf

@Serializable
@Entity
data class Song(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "length") val length: Int,
    @ColumnInfo(name = "artists") val artists: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "ext") val ext: String
) {
    constructor(
        id: String,
        name: String
    ): this(id, name, 165000, "a", "2022.09.04", "mp3")

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override operator fun equals(other: Any?): Boolean {
        return if (other is Song) {
            id == other.id
        } else {
            Log.e("Song / equal", "$Song cannot compare with $other")
            false
        }
    }

    fun toPath(): String {
        return MainApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath + "/" + id + "." + ext
    }

    fun toUrl(): String {
        val member: Map<String, String> = mapOf(("向晚" to "A"), ("贝拉" to "B"), ("珈乐" to "C"), ("嘉然" to "D"), ("乃琳" to "E"))
        var artistCode = ""
        for (artist in artists.split("/")) {
            if (artist in member.keys) {
                artistCode += member[artist]
            }
            else {
                artistCode = "L"
                break
            }
        }
        if (artistCode.length > 3) {
            artistCode = "F"
        }
        return "https://as-archive-azcn-0001.asf.ink/AZCN-Sharepoint/$date $artistCode $name.$ext"
    }

    fun isCached(): Boolean {
        return File(toPath()).isFile
    }
}