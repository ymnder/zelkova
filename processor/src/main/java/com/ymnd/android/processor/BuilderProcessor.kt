package com.ymnd.android.processor

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.*
import com.ymnd.android.annotation.Builder
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

    override fun annotations() = setOf(Builder::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>?): MutableSet<Element> {
        elementsByAnnotation ?: return mutableSetOf()

        val filteredElements = elementsByAnnotation.asMap().filter {
            it.key == Builder::class.java
        }

        filteredElements[Builder::class.java]?.forEach {
            val klassBuilder = TypeSpec
                    .classBuilder("${it.simpleName}Builder")

            val elementArray = arrayListOf<String>()
            it.enclosedElements.forEach { element ->
                if (element.kind == ElementKind.FIELD) {
                    elementArray.add("${element.simpleName}")
                    // TypeName as ClassNameはダウンキャストだから危ない
                    val className = convertClassNameAsKotlin(element.asType().asTypeName() as ClassName)
                    klassBuilder
                            .addProperty(
                                    PropertySpec.varBuilder("${element.simpleName}", className)
                                            .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                            .build())
                            .addFunction(
                                    FunSpec.builder("${element.simpleName}")
                                            .addParameter("${element.simpleName}", className)
                                            .addStatement("return apply{ this.${element.simpleName} = ${element.simpleName} }")
                                            .build())
                }
            }
            klassBuilder.addFunction(
                    FunSpec.builder("build")
                            .addStatement("return %T(${elementArray.joinToString()})", it.asType().asTypeName())
                            .build()
            )
            factory(klassBuilder)
        }

        return mutableSetOf()
    }

    private fun factory(klassBuilder: TypeSpec.Builder) {
        val typeSpec = klassBuilder.build()
        FileSpec.builder(PACKAGE_NAME, typeSpec.name!!)
                .addType(typeSpec)
                .build()
                .writeTo(outputDir)
    }

    private fun convertClassNameAsKotlin(className: ClassName): ClassName {
        return when (className.canonicalName) {
            String::class.java.name -> String::class.asClassName()
            else -> className
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.ymnd.android.builder"
    }
}