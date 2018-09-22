package com.ymnd.android.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.ymnd.android.annotation.Builder
import me.eugeniomarletti.kotlin.metadata.ClassData
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.extractFullName
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import java.io.File
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class KotlinBuilderProcessor : AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return hashSetOf(
                Builder::class.java.canonicalName
        )
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(Builder::class.java)
                .forEach { element ->
                    generateClassFile(element)
                }
        return true
    }

    private fun generateClassFile(targetElement: Element) {
        val className = "${targetElement.simpleName}"
        val fileName = "${className}Builder"
        val packageName = "${processingEnv.elementUtils.getPackageOf(targetElement)}"
        val targetClassName = targetElement.asType().asTypeName()

        val metaData = (targetElement.kotlinMetadata as KotlinClassMetadata).data
        val properties: List<ProtoBuf.Property> = metaData.classProto.propertyList
        val fieldTypes: List<FieldType> = properties.map { property ->
            FieldType(property, metaData)
        }

        val typeSpecBuilder = TypeSpec
                .classBuilder(fileName)
                .addModifiers(KModifier.OPEN)

        fieldTypes.forEach { fieldType ->
            typeSpecBuilder.addProperty(
                    PropertySpec.varBuilder(fieldType.fieldName, fieldType.className)
                            .addModifiers(KModifier.PRIVATE)
                            .apply {
                                if (fieldType.nullable) {
                                    this.initializer("null")
                                } else {
                                    this.delegate("kotlin.properties.Delegates.notNull()")
                                }
                            }
                            .build()
            )

            typeSpecBuilder.addFunction(
                    FunSpec.builder(fieldType.fieldName)
                            .addParameter(fieldType.fieldName, fieldType.className)
                            .addStatement("return apply { this.%N = %N }",
                                    fieldType.fieldName, fieldType.fieldName
                            )
                            .build()
            )
        }

        val fieldNames = arrayListOf<String>().apply {
            fieldTypes.forEach { fieldType ->
                val fieldName = fieldType.fieldName
                add(fieldName)
                add(fieldName)
            }
        }

        typeSpecBuilder.addFunction(
                FunSpec.builder("build")
                        .addStatement("return %T(" + Collections.nCopies(fieldTypes.size, "%N = %N")
                                .joinToString() + ")",
                                targetClassName,
                                *fieldNames.toTypedArray()
                        )
                        .build()
        )

        val lambdaTypeName = LambdaTypeName.get(receiver = ClassName(packageName, "${fileName}Dsl"), returnType = Unit::class.java.asTypeName())
        val lambda = ParameterSpec.builder("f", lambdaTypeName).build()

        val dslTypeSpec = TypeSpec.classBuilder("${fileName}Dsl")
                .superclass(ClassName(packageName, fileName))
                .build()

        val dslFunSpec = FunSpec.builder(fileName.decapitalize())
                .addModifiers(KModifier.INLINE)
                .addParameter(lambda)
                .addStatement(
                        "return "
                                + "${fileName}Dsl("
                                + ").apply{ f() }"
                                + ".build()")
                .build()

        val typeSpec = typeSpecBuilder.build()

        val outputDirectory = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?.replace("kaptKotlin", "kapt")
                ?.let { File(it) }
                ?: throw IllegalArgumentException("No output directory...")

        FileSpec.builder(packageName, typeSpec.name!!)
                .addType(typeSpec)
                .addType(dslTypeSpec)
                .addFunction(dslFunSpec)
                .build()
                .writeTo(outputDirectory)

    }

    class FieldType(
            property: ProtoBuf.Property,
            metaData: ClassData
    ) {
        private val returnType = property.returnType.extractFullName(metaData, true)
        private val simpleType = returnType
                .replace("`", "")
                .replace("kotlin.collections.", "")
                .replace("kotlin.", "")

        val className = ClassName.bestGuess(simpleType)
        val fieldName = metaData.nameResolver.getString(property.name)
        val nullable = property.returnType.nullable
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}