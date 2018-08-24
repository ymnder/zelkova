package com.ymnd.android.processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.Element

class TypeConverter {

    fun convert(fieldElement: Element): FieldType {
        val element = fieldElement.asType().asTypeName()
        return when (element) {
            is ClassName -> {
                FieldType(
                        isPrimitive = Type.isPrimitive(element),
                        defaultValue = Type.getDefaultValue(element),
                        typeName = Type.of(element)
                )
            }
            is ParameterizedTypeName -> {
                FieldType(typeName = element)
            }
            is WildcardTypeName -> {
                FieldType(typeName = element)
            }
            is TypeVariableName -> {
                FieldType(typeName = element)
            }
            else -> throw IllegalArgumentException("unexpected type: $element")
        }
    }
}