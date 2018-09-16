package com.ymnd.android.processor

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.*
import com.ymnd.android.annotation.Builder
import com.ymnd.android.annotation.DefaultValue
import me.eugeniomarletti.kotlin.metadata.shadow.name.FqName
import me.eugeniomarletti.kotlin.metadata.shadow.platform.JavaToKotlinClassMap
import java.io.File
import javax.annotation.processing.Messager
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.util.Elements

//@AutoService(Processor::class)
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
                    .addModifiers(KModifier.OPEN)


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
//addRows: ListBuilderDsl.() -> Unit
//            + "," //これは自動で追加されるべきだ
//            + "f: ${it.simpleName}BuilderDsl.() -> Unit"
            val lambdaTypeName = LambdaTypeName.get(receiver = ClassName(PACKAGE_NAME,"${it.simpleName}BuilderDsl"), returnType = Unit::class.java.asTypeName())
            val lambda = ParameterSpec.builder("f", lambdaTypeName).build()

            //あとで直す
            val typeSpec = typeSpecBuilder.build()
            FileSpec.builder(PACKAGE_NAME, typeSpec.name!!)
                    .addType(typeSpec)
                    .addType(
                            TypeSpec.classBuilder("${it.simpleName}BuilderDsl")
                                    .superclass(ClassName(PACKAGE_NAME, "${it.simpleName}Builder"))
                                    .addSuperclassConstructorParameter(superClassConstructor(builderFields.filter { it.isVal }))
                                    .primaryConstructor(FunSpec.constructorBuilder()
                                            .addParameters(constructorField)
                                            .build())
                                    .build()
                    )
                    .addFunction(
                            FunSpec.builder("${it.simpleName}Builder".decapitalize())
                                    .addModifiers(KModifier.INLINE)
                                    .addParameters(constructorField)
                                    .addParameter(lambda)
                                    .addStatement(
                                            "return "
                                                    + "${it.simpleName}BuilderDsl("
                                                    + superClassConstructor(builderFields.filter { it.isVal })
                                                    + ").apply{ f() }"
                                                    + ".build()")
                                    .build())
                    .build()
                    .writeTo(outputDir)
        }

        return mutableSetOf()
    }

    private fun superClassConstructor(builderFields: List<BuilderField>): String {
        return builderFields
                .mapNotNull { it.simpleName }
                .joinToString()
    }

    companion object {
        private const val PACKAGE_NAME = "com.ymnd.android.builder"
    }
}