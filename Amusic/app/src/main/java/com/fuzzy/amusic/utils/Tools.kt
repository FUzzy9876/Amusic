package com.fuzzy.amusic.utils

object Tools {
    fun prettyTime(msec: Int): String {
        val min = (msec / 1000 / 60).toString()
        var sec = (msec / 1000 % 60).toString()
        if (sec.length == 1) { sec = "0$sec" }
        return "$min:$sec"
    }
}