package com.fuzzy.amusic.database

import android.os.Environment
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fuzzy.amusic.MainApplication
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URLEncoder

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

    fun toPath(): String {
        return MainApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath + "/" + id + ext
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

    fun isExistInCache(): Boolean {
        return File(toPath()).isFile
    }
}

/**
data class Song constructor(
val id: String,
val name: String,
val version: Int?,
val length: Int,
val artists: String,
val date: String
) {
constructor(
id: String,
name: String
) : this(id, name, null, 165000, "a", "2022.09.04")

fun toPath(): String {
return getInstance().getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath + "/" + id + ".mp3"
}

fun isExistInCache(): Boolean {
return File(toPath()).isFile
}
}
 */