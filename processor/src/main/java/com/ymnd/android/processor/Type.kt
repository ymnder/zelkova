package com.ymnd.android.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import kotlin.reflect.KClass

enum class Type(val clazz: Class<*>, val klazz: KClass<*>, val defaultValue: Any) {
    BYTE(Byte::class.java, Byte::class, 0),
    BYTE_WRAPPER(java.lang.Byte::class.java, Byte::class, 0),
    SHORT(Short::class.java, Short::class, 0),
    SHORT_WRAPPER(java.lang.Short::class.java, Short::class, 0),
    INT(Int::class.java, Int::class, 0),
    INT_WRAPPER(java.lang.Integer::class.java, Int::class, 0),
    LONG(Long::class.java, Long::class, 0),
    LONG_WRAPPER(java.lang.Long::class.java, Long::class, 0),
    FLOAT(Float::class.java, Float::class, 0.0),
    FLOAT_WRAPPER(java.lang.Float::class.java, Float::class, 0.0),
    DOUBLE(Double::class.java, Double::class, 0.0),
    DOUBLE_WRAPPER(java.lang.Double::class.java, Double::class, 0.0),
    CHAR(Char::class.java, Char::class, '\u0000'),
    CHAR_WRAPPER(java.lang.Character::class.java, Char::class, '\u0000'),
    BOOLEAN(Boolean::class.java, Boolean::class, false),
    BOOLEAN_WRAPPER(java.lang.Boolean::class.java, Boolean::class, false),
    STRING(String::class.java, String::class, "\"\""),
    STRING_WRAPPER(java.lang.String::class.java, String::class, "\"\"");

    private val className
        get() = (this.clazz.asTypeName() as ClassName)

    private val klazzName
        get() = this.klazz.asClassName()

    private val isPrimitive: Boolean
        get() = this.klazz.java.isPrimitive

    private val isWrapper: Boolean
        get() = this.clazz != this.klazz

    companion object {
        fun of(targetClassName: ClassName): ClassName {
            return values().firstOrNull {
                it.className.canonicalName == targetClassName.canonicalName
            }?.let {
                it.klazzName.apply { if (it.isWrapper) this.asNullable() }
            } ?: targetClassName
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