package com.ymnd.android.zelkova

import com.ymnd.android.annotation.Builder

@Builder
data class KTypes(
        var byteVal: Byte,
        var byteNull: Byte?,
        var shortVal: Short,
        var shortNull: Short?,
        var intVal: Int,
        var intNull: Int?,
        var longVal: Long,
        var longNull: Long?,
        var floatVal: Float,
        var floatNull: Float?,
        var doubleVal: Double,
        var doubleNull: Double?,
        var charVal: Char,
        var charNull: Char?,
        var booleanVal: Boolean,
        var booleanNull: Boolean?,
        var stringVal: String,
        var stringNull: String?
)