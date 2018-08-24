package com.ymnd.android.processor

import com.squareup.kotlinpoet.TypeName

data class FieldType(
        val isPrimitive: Boolean = false,
        val defaultValue: Any? = null,
        val typeName: TypeName
)