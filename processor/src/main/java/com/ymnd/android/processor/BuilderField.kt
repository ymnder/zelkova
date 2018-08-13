package com.ymnd.android.processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

class BuilderField(
        private val fieldElement: Element,
        private val annotatedFieldElement: Element?
) {

    private val isVal: Boolean = Modifier.FINAL in this.fieldElement.modifiers
    private val isDefaultValue = this.annotatedFieldElement != null
    private val isPrimitive = checkPrimitive(fieldElement.asType().asTypeName() as ClassName)
    private val isNullable = this.fieldElement.asType().asTypeName().nullable
    val simpleName = fieldElement.simpleName
    internal val simpleNameString = "$simpleName"
    // TypeName as ClassNameはダウンキャストだから危ない
    private val className = convertClassNameAsKotlin(fieldElement.asType().asTypeName() as ClassName)

    fun createParamSpec(): ParameterSpec? {
        if (!isVal) return null
        return ParameterSpec
                .builder(simpleNameString, className)
                .build()
    }

    private fun createPropertySpec(typeSpecBuilder: TypeSpec.Builder) {
        if (isVal) {
            typeSpecBuilder
                    .addProperty(
                            PropertySpec.builder(simpleNameString, className)
                                    .addModifiers(KModifier.PRIVATE)
                                    .initializer(simpleNameString)
                                    .build()
                    )
        } else {
            if (isPrimitive || isDefaultValue) {
                typeSpecBuilder.addProperty(
                        PropertySpec.varBuilder(simpleNameString, className)
                                .addModifiers(KModifier.PRIVATE)
                                .initializer("${convertDefaultValue(fieldElement.asType().asTypeName() as ClassName)}")
                                .build()
                )
            } else {
                typeSpecBuilder.addProperty(
                        PropertySpec.varBuilder(simpleNameString, className)
                                .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                .build()
                )
            }
        }
    }

    private fun createFunctionSpec(typeSpecBuilder: TypeSpec.Builder) {
        if (isVal) return
        typeSpecBuilder.addFunction(
                FunSpec.builder(simpleNameString)
                        .addParameter(simpleNameString, className)
                        .addStatement("return apply{ this.$simpleNameString = $simpleNameString }")
                        .build()
        )
    }

    fun addSpec(typeSpecBuilder: TypeSpec.Builder): TypeSpec.Builder {
        return typeSpecBuilder
                .apply {
                    createPropertySpec(this)
                    createFunctionSpec(this)
                }
    }

    private fun convertClassNameAsKotlin(className: ClassName): ClassName {
        return when (className.canonicalName) {
            Type.STRING.canonicalName -> Type.STRING.className
            Type.INT.canonicalName -> Type.INT.className
            Type.INTEGER.canonicalName -> Type.INTEGER.className
            Type.LONG.canonicalName -> Type.LONG.className
            Type.FLOAT.canonicalName -> Type.FLOAT.className
            Type.DOUBLE.canonicalName -> Type.DOUBLE.className
            Type.BOOLEAN.canonicalName -> Type.BOOLEAN.className
            else -> className
        }
    }

    private fun checkPrimitive(className: ClassName): Boolean {
        return when (className.canonicalName) {
            Type.STRING.canonicalName -> Type.STRING.isPrimitive
            Type.INT.canonicalName -> Type.INT.isPrimitive
            Type.INTEGER.canonicalName -> Type.INTEGER.isPrimitive
            Type.LONG.canonicalName -> Type.LONG.isPrimitive
            Type.FLOAT.canonicalName -> Type.FLOAT.isPrimitive
            Type.DOUBLE.canonicalName -> Type.DOUBLE.isPrimitive
            Type.BOOLEAN.canonicalName -> Type.BOOLEAN.isPrimitive
            else -> false
        }
    }

    private fun convertDefaultValue(className: ClassName): Any? {
        return when (className.canonicalName) {
            Type.STRING.canonicalName -> Type.STRING.defaultValue
            Type.INT.canonicalName -> Type.INT.defaultValue
            Type.INTEGER.canonicalName -> Type.INTEGER.defaultValue
            Type.LONG.canonicalName -> Type.LONG.defaultValue
            Type.FLOAT.canonicalName -> Type.FLOAT.defaultValue
            Type.DOUBLE.canonicalName -> Type.DOUBLE.defaultValue
            Type.BOOLEAN.canonicalName -> Type.BOOLEAN.defaultValue
            else -> null
        }
    }

    enum class Type(val canonicalName: String, val className: ClassName, val isPrimitive: Boolean, val defaultValue: Any) {
        STRING(String::class.java.name, String::class.asClassName(), String::class.java.isPrimitive, "\"\""),
        INT(Int::class.java.name, Int::class.asClassName(), Int::class.java.isPrimitive, 0),
        INTEGER(Integer::class.java.name, Int::class.asClassName().asNullable(), Int::class.java.isPrimitive, 0),
        LONG(Long::class.java.name, Long::class.asClassName(), Long::class.java.isPrimitive, 0),
        FLOAT(Float::class.java.name, Float::class.asClassName(), Float::class.java.isPrimitive, 0),
        DOUBLE(Double::class.java.name, Double::class.asClassName(), Double::class.java.isPrimitive, 0),
        BOOLEAN(Boolean::class.java.name, Boolean::class.asClassName(), Boolean::class.java.isPrimitive, false),
    }
}