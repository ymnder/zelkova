package com.ymnd.android.processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

class BuilderField(
        private val fieldElement: Element,
        private val annotatedFieldElement: Element?
) {

    internal val isVal: Boolean = Modifier.FINAL in this.fieldElement.modifiers
    private val isDefaultValue = this.annotatedFieldElement != null
    private val isPrimitive = Type.isPrimitive(fieldElement.asType().asTypeName() as ClassName)
    internal val simpleName = fieldElement.simpleName
    internal val simpleNameString = "$simpleName"
    private val className = Type.of(fieldElement.asType().asTypeName() as ClassName)

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
            if (isDefaultValue) {
                typeSpecBuilder.addProperty(
                        PropertySpec.varBuilder(simpleNameString, className)
                                .addModifiers(KModifier.PRIVATE)
                                .initializer("${Type.getDefaultValue(fieldElement.asType().asTypeName() as ClassName)}")
                                .build()
                )
            } else {
                typeSpecBuilder.addProperty(
                        PropertySpec.varBuilder(simpleNameString, className)
                                .apply {
                                    if (isPrimitive) {
                                        this.addModifiers(KModifier.PRIVATE)
                                        this.delegate("kotlin.properties.Delegates.notNull()")
                                    } else {
                                        this.addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                    }
                                }
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
                        .addStatement("return apply { this.$simpleNameString = $simpleNameString }")
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
}