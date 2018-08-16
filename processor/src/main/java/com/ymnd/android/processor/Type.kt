package com.ymnd.android.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import kotlin.reflect.KClass

//試行錯誤の結果、klazzいらない気がする
enum class Type(val clazz: KClass<*>, val klazz: KClass<*>, val defaultValue: Any) {
    BYTE(Byte::class, Byte::class, 0),
    BYTE_WRAPPER(java.lang.Byte::class, Byte::class, 0),
    SHORT(Short::class, Short::class, 0),
    SHORT_WRAPPER(java.lang.Short::class, Short::class, 0),
    INT(Int::class, Int::class, 0),
    INT_WRAPPER(java.lang.Integer::class, Int::class, 0),
    LONG(Long::class, Long::class, 0),
    LONG_WRAPPER(java.lang.Long::class, Long::class, 0),
    FLOAT(Float::class, Float::class, 0.0),
    FLOAT_WRAPPER(java.lang.Float::class, Float::class, 0.0),
    DOUBLE(Double::class, Double::class, 0.0),
    DOUBLE_WRAPPER(java.lang.Double::class, Double::class, 0.0),
    CHAR(Char::class, Char::class, '\u0000'),
    CHAR_WRAPPER(java.lang.Character::class, Char::class, '\u0000'),
    BOOLEAN(Boolean::class, Boolean::class, false),
    BOOLEAN_WRAPPER(java.lang.Boolean::class, Boolean::class, false),
    STRING(String::class, String::class, "\"\""),
    STRING_WRAPPER(java.lang.String::class, String::class, "\"\"");

    private val className
        get() = this.clazz.java.asTypeName() as ClassName


    private val klazzName
        get() = this.klazz.asClassName()

    private val isPrimitive: Boolean
        get() = this.klazz.java.isPrimitive

    companion object {
        fun of(targetClassName: ClassName): ClassName {
            return values().firstOrNull {
                it.className.canonicalName == targetClassName.canonicalName
            }?.klazzName ?: targetClassName
        }

        fun isPrimitive(targetClassName: ClassName): Boolean {
            return values().firstOrNull {
                it.className.canonicalName == targetClassName.canonicalName
            }?.isPrimitive ?: false
        }

        fun getDefaultValue(targetClassName: ClassName): Any? {
            return values().firstOrNull {
                it.className.canonicalName == targetClassName.canonicalName
            }?.defaultValue
        }
    }
}