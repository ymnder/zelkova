package com.ymnd.android.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.ymnd.android.annotation.Builder
import me.eugeniomarletti.kotlin.metadata.*
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver
import java.io.File
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
        val typeSpecBuilder = TypeSpec
                .classBuilder("${targetElement.simpleName}Builder")
                .addModifiers(KModifier.OPEN)

        val metaData = (targetElement.kotlinMetadata as KotlinClassMetadata).data
        val typeElement = targetElement as TypeElement
        val properties: List<ProtoBuf.Property> = metaData.classProto.propertyList
        val tparams: List<ProtoBuf.TypeParameter> = metaData.classProto.typeParameterList
        val name: String = typeElement.simpleName.toString()
        val pairs: List<Triple<String, String, String>> = properties.map { protbuf ->
            val retType = protbuf.returnType.extractFullName(metaData, true)
            val imports = protbuf.returnType.extractFullImport(metaData, true)
//                        it.isVal
//                        it.isVar
//                        it.returnType.nullable
            val pname = metaData.nameResolver.getString(protbuf.name)
            //これは最適化できそう
            val importsList = imports
                    .removeBackticks()
                    .split(",")
                    .map { it.trim() }
                    .distinct()
                    .joinToString(prefix = "import ", postfix = " \n", separator = " \n import ")
            Triple(name, retType.removeBackticks(), "var")
        }
        pairs.forEach { pair ->
            typeSpecBuilder.addProperty(
                    PropertySpec.builder(pair.first, generateClassName(pair.second))
                            .addModifiers(KModifier.PRIVATE)
                            .initializer(pair.first)
                            .build()
            )
        }
        val className = targetElement.simpleName.toString()
        val pack = processingEnv.elementUtils.getPackageOf(targetElement).toString()
        //generateClass(className, pack)

        val typeSpec = typeSpecBuilder.build()
        FileSpec.builder(PACKAGE_NAME, typeSpec.name!!)
                .addType(typeSpec)
                .build()
                .writeTo(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]!!.let { File(it) })

    }

    private fun generateClassName(className: String): ClassName {
        try {
            return ClassName.bestGuess(className)
        } catch (e: IllegalArgumentException) {
            return ClassName.bestGuess("kotlin.collections.List<kotlin.String>")
        }
    }

    fun String.removeBackticks() = this.replace("`", "")

    private fun generateClass(className: String, pack: String) {
        val fileName = "Generated_$className"
        val file = FileSpec.builder(pack, fileName)
                .addType(TypeSpec.classBuilder(fileName)
                        .addFunction(FunSpec.builder("getName")
                                .addStatement("return \"World\"")
                                .build())
                        .build())
                .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        private const val PACKAGE_NAME = "com.ymnd.android.builder"
    }

    fun ProtoBuf.Type.extractFullImport(
            nameResolver: NameResolver,
            getTypeParameter: (index: Int) -> ProtoBuf.TypeParameter,
            outputTypeAlias: Boolean = true,
            throwOnGeneric: Throwable? = null
    ): String {

        if (!hasClassName() && throwOnGeneric != null) throw throwOnGeneric

        val name = when {
            hasTypeParameter() -> getTypeParameter(typeParameter).name
            hasTypeParameterName() -> typeParameterName
            outputTypeAlias && hasAbbreviatedType() -> abbreviatedType.typeAliasName
            else -> className
        }.let { nameResolver.getString(it).escapedClassName }

        val argumentList = when {
            outputTypeAlias && hasAbbreviatedType() -> abbreviatedType.argumentList
            else -> argumentList
        }
        val arguments = argumentList
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "", postfix = "") {
                    when {
                        it.hasType() -> it.type.extractFullImport(nameResolver, getTypeParameter, outputTypeAlias, throwOnGeneric)
                        throwOnGeneric != null -> throw throwOnGeneric
                        else -> ""
                    }
                }
                ?: ""

        return if (!arguments.isEmpty()) "$name,$arguments" else name
    }

    fun ProtoBuf.Type.extractFullImport(
            data: ClassData,
            outputTypeAlias: Boolean = true,
            throwOnGeneric: Throwable? = null
    ) =
            extractFullImport(data.nameResolver, data.proto::getTypeParameter, outputTypeAlias, throwOnGeneric)

}