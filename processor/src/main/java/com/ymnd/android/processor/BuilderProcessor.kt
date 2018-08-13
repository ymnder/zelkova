package com.ymnd.android.processor

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.ymnd.android.annotation.Builder
import com.ymnd.android.annotation.DefaultValue
import java.io.File
import javax.annotation.processing.Messager
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.util.Elements

@AutoService(Processor::class)
class BuilderProcessor : BasicAnnotationProcessor() {
    companion object {
        private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedOptions() = setOf(KAPT_KOTLIN_GENERATED_OPTION_NAME)

    override fun initSteps(): Iterable<ProcessingStep> {
        val outputDirectory = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?.replace("kaptKotlin", "kapt")
                ?.let { File(it) }
                ?: throw IllegalArgumentException("No output directory...")

        return listOf(
                BuilderProcessingStep(
                        elements = processingEnv.elementUtils,
                        messager = processingEnv.messager,
                        outputDir = outputDirectory
                )
        )
    }
}

class BuilderProcessingStep(private val elements: Elements, private val messager: Messager, private val outputDir: File) : BasicAnnotationProcessor.ProcessingStep {

    override fun annotations() = setOf(
            Builder::class.java,
            DefaultValue::class.java
    )

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>?): MutableSet<Element> {
        elementsByAnnotation ?: return mutableSetOf()

        elementsByAnnotation[Builder::class.java]?.forEach {
            if (it.kind != ElementKind.CLASS) throw IllegalArgumentException("this annotation only class @${Builder::class.java.simpleName}")

            val typeSpecBuilder = TypeSpec
                    .classBuilder("${it.simpleName}Builder")


            val annotatedFieldElements = elementsByAnnotation[DefaultValue::class.java]

            val builderFields = it.enclosedElements.mapNotNull { fieldElement ->
                if (fieldElement.kind == ElementKind.FIELD) {
                    return@mapNotNull BuilderField(fieldElement,
                            annotatedFieldElements.firstOrNull { afe -> "${afe.simpleName}" == "${fieldElement.simpleName}\$annotations" }
                    )
                } else {
                    return@mapNotNull null
                }
            }

            val constructorField = builderFields.mapNotNull { it.createParamSpec() }
            typeSpecBuilder.primaryConstructor(
                    FunSpec.constructorBuilder()
                            .addParameters(constructorField)
                            .build()
            )

            builderFields.forEach { field ->
                field.addSpec(typeSpecBuilder)
            }

            val allFields = builderFields.joinToString { field -> field.simpleNameString }

            typeSpecBuilder.addFunction(
                    FunSpec.builder("build")
                            .addStatement("return %T($allFields)", it.asType().asTypeName())
                            .build()
            )
            factory(typeSpecBuilder)
        }

        return mutableSetOf()
    }

    private fun factory(typeSpecBuilder: TypeSpec.Builder) {
        val typeSpec = typeSpecBuilder.build()
        FileSpec.builder(PACKAGE_NAME, typeSpec.name!!)
                .addType(typeSpec)
                .build()
                .writeTo(outputDir)
    }

    companion object {
        private const val PACKAGE_NAME = "com.ymnd.android.builder"
    }
}