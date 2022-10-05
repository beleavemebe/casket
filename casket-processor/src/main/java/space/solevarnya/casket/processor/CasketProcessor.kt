package space.solevarnya.casket.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import space.solevarnya.casket.DaoSkeleton
import space.solevarnya.casket.EntityMapper
import space.solevarnya.casket.KeyMatcher
import space.solevarnya.casket.PrimaryKey

class CasketProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    private val packageName = "space.solevarnya.casket.generated"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val unableToProcess = mutableListOf<KSClassDeclaration>()
        val entities = resolver
            .getSymbolsWithAnnotation("space.solevarnya.casket.Entity")
            .filterIsInstance<KSClassDeclaration>()
        extractTablePaths(entities, resolver)
        entities.forEach {
            generateMapper(it, resolver)
            generateKeyMatcher(it, resolver)
        }
        unableToProcess += entities.filterNot(KSNode::validate).toList()

        val daos = resolver
            .getSymbolsWithAnnotation("space.solevarnya.casket.Dao")
            .filterIsInstance<KSClassDeclaration>()
        daos.forEach {
            implementDao(it, resolver)
        }
        unableToProcess += daos.filterNot(KSNode::validate).toList()

        return unableToProcess
    }

    @OptIn(KspExperimental::class)
    private fun generateKeyMatcher(
        entityDeclaration: KSClassDeclaration,
        resolver: Resolver
    ) {
        val className = entityDeclaration.simpleName.asString() + "KeyMatcher"
        val pkAnnotatedProperty = entityDeclaration
            .primaryConstructor!!
            .parameters
            .firstOrNull { it.isAnnotationPresent(PrimaryKey::class) }
        pkAnnotatedProperty ?: return
        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.objectBuilder(className)
                    .addSuperinterface(
                        KeyMatcher::class.asClassName()
                            .parameterizedBy(
                                entityDeclaration.toClassName(),
                                pkAnnotatedProperty.type.toTypeName()
                            )
                    )
                    .addFunction(
                        FunSpec.builder("keyMatchesEntity")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter(
                                ParameterSpec("key", pkAnnotatedProperty.type.toTypeName())
                            )
                            .addParameter(
                                ParameterSpec("entity", entityDeclaration.toClassName())
                            )
                            .addCode("return entity.${pkAnnotatedProperty.name!!.asString()} == key")
                            .returns(Boolean::class)
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator, false, resolver.getAllFiles().toList())
    }

    private fun generateMapper(
        entityDeclaration: KSClassDeclaration,
        resolver: Resolver
    ) {
        val className = entityDeclaration.simpleName.asString() + "Mapper"
        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.objectBuilder(className)
                    .addSuperinterface(
                        EntityMapper::class
                            .asClassName()
                            .parameterizedBy(entityDeclaration.toClassName()),
                    )
                    .addFunction(
                        FunSpec.builder("mapToString")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter(ParameterSpec("entity", entityDeclaration.toClassName()))
                            .returns(String::class)
                            .addCode(createMapToStringImpl(entityDeclaration))
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("mapToEntity")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter(ParameterSpec("string", String::class.asTypeName()))
                            .returns(entityDeclaration.toClassName())
                            .addCode(createMapToEntityImpl(entityDeclaration))
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator, false, resolver.getAllFiles().toList())
    }

    private fun createMapToStringImpl(entityDeclaration: KSClassDeclaration): CodeBlock {
        val params = entityDeclaration.primaryConstructor!!.parameters
        val listProperties = params.joinToString("\n") {
            "    entity.${it.name!!.asString()},"
        }
        return CodeBlock.of(
            "return listOf(\n%L\n).joinToString(\"\t\")", listProperties
        )
    }

    private fun createMapToEntityImpl(entityDeclaration: KSClassDeclaration): CodeBlock {
        val params = entityDeclaration.primaryConstructor!!.parameters
        var i = 0
        val assignProperties = params.joinToString("") {
            val propertyName = it.name!!.asString()
            val conversion = when (val typeName = it.type.resolve().toClassName().simpleName) {
                "String" -> "parts[${i++}]"
                "Int" -> "parts[${i++}].toInt()"
                else -> error("unknown type $typeName")
            }
            "$propertyName = $conversion, "
        }
        val className = entityDeclaration.simpleName.asString()
        return CodeBlock.builder()
            .add(CodeBlock.of("val parts = string.split(\"\t\").filter { it != \"\" }\n"))
            .add("return %L(\n    %L\n)", className, assignProperties)
            .build()
    }

    private fun extractTablePaths(
        declarations: Sequence<KSClassDeclaration>,
        resolver: Resolver,
    ) = runCatching {
        val className = "CasketTablePaths"
        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.objectBuilder(className)
                    .apply {
                        declarations.iterator().forEach { entityDeclaration ->
                            val filePath = entityDeclaration.annotations.iterator()
                                .next().arguments[0].value
                            addProperty(
                                PropertySpec.builder(
                                    "tablePathOf${entityDeclaration.simpleName.asString()}",
                                    String::class,
                                    KModifier.CONST
                                )
                                    .initializer("%S", filePath)
                                    .build()
                            )
                        }
                    }.build()
            )
            .build()
            .writeTo(codeGenerator, false, resolver.getAllFiles().toList())
    }

    private fun implementDao(
        declaration: KSClassDeclaration,
        resolver: Resolver,
    ) {
        val className = declaration.simpleName.asString() + "_Impl"
        val daoBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(declaration.toClassName())
            .also {
                declaration.accept(DaoCodegenVisitor(it), Unit)
            }

        FileSpec.builder(packageName, className)
            .addType(daoBuilder.build())
            .build()
            .writeTo(codeGenerator, false, resolver.getAllFiles().toList())
    }

    inner class DaoCodegenVisitor(
        private val daoBuilder: TypeSpec.Builder
    ) : KSVisitorVoid() {
        lateinit var entityClass: KSType

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if (classDeclaration.classKind != ClassKind.INTERFACE) {
                logger.error("Only interfaces can be annotated '@Dao'")
                return
            }

            val daoAnnotation = classDeclaration.annotations.iterator().next()
            entityClass = daoAnnotation.arguments[0].value as KSType

            addMapperProperty()
            addDaoSkeletonProperty()
            classDeclaration.getAllFunctions().forEach {
                visitFunctionDeclaration(it, Unit)
            }
        }

        private fun addMapperProperty() {
            daoBuilder.addProperty(
                PropertySpec.builder(
                    "mapper",
                    EntityMapper::class.asClassName().parameterizedBy(entityClass.toTypeName()),
                    KModifier.PRIVATE
                )
                    .initializer("%L", "${entityClass}Mapper")
                    .build()
            )
        }

        @OptIn(KspExperimental::class)
        private fun addDaoSkeletonProperty() {
            val pkPropertyParameter = entityClass.declaration.closestClassDeclaration()!!
                .primaryConstructor!!
                .parameters
                .firstOrNull { it.isAnnotationPresent(PrimaryKey::class) }
            val isPkPresent = pkPropertyParameter != null

            daoBuilder.addProperty(
                PropertySpec.builder(
                    "skeleton",
                    if (isPkPresent) {
                        DaoSkeleton::class.asClassName().parameterizedBy(entityClass.toTypeName(), pkPropertyParameter!!.type.resolve().toTypeName())
                    } else {
                        DaoSkeleton::class.asClassName().parameterizedBy(entityClass.toTypeName(), Nothing::class.asTypeName())
                    },
                    KModifier.PRIVATE
                )
                    .apply {
                        if (isPkPresent) {
                            initializer(
                                "%T(CasketTablePaths.%L, mapper, %L)",
                                DaoSkeleton::class,
                                "tablePathOf$entityClass",
                                "${entityClass}KeyMatcher"
                            )
                        } else {
                            initializer(
                                "%T(CasketTablePaths.%L, mapper)",
                                DaoSkeleton::class,
                                "tablePathOf$entityClass"
                            )
                        }
                    }.build()
            )
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            val crudAnnotation = function.annotations
                .firstOrNull {
                    it.shortName.asString() in setOf(
                        "Create", "ReadAll", "Read", "Update", "Delete"
                    )
                }

            crudAnnotation ?: return
            when (crudAnnotation.shortName.asString()) {
                "Create" -> generateCreate(function)
                "ReadAll" -> generateReadAll(function)
                "Read" -> generateRead(function)
                "Update" -> generateUpdate(function)
                "Delete" -> generateDelete(function)
            }
        }

        @OptIn(KspExperimental::class)
        private fun generateCreate(function: KSFunctionDeclaration) {
            val pkAnnotatedProperty = entityClass.declaration.closestClassDeclaration()!!
                .primaryConstructor!!
                .parameters
                .firstOrNull { it.isAnnotationPresent(PrimaryKey::class) }

            daoBuilder.addFunction(
                FunSpec.builder(function.simpleName.asString())
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(ParameterSpec("entity", function.parameters[0].type.toTypeName()))
                    .returns(function.returnType!!.toTypeName())
                    .apply {
                        if (pkAnnotatedProperty != null) {
                            addCode("skeleton.appendEntity(entity.%L, entity)", pkAnnotatedProperty.name!!.asString())
                        } else {
                            addCode("skeleton.appendEntity(entity)")
                        }
                    }
                    .build()
            )
        }

        private fun generateReadAll(function: KSFunctionDeclaration) {
            daoBuilder.addFunction(
                FunSpec.builder(function.simpleName.asString())
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(function.returnType!!.toTypeName())
                    .addCode("return skeleton.readEntities()")
                    .build()
            )
        }

        @OptIn(KspExperimental::class)
        private fun generateRead(function: KSFunctionDeclaration) {
            val idParam = function.parameters[0]
            val pkAnnotatedProperty = entityClass.declaration.closestClassDeclaration()!!
                .primaryConstructor!!
                .parameters
                .firstOrNull { it.isAnnotationPresent(PrimaryKey::class) }

            require(pkAnnotatedProperty != null) {
                "'read(id)' operation does not work on entities without primary key"
            }

            require(idParam.type.resolve() == pkAnnotatedProperty.type.resolve()) {
                "Type ${idParam.type} of 'read(id)' function's only parameter does not match the type of $entityClass's primary key ${pkAnnotatedProperty.type}"
            }

            daoBuilder.addFunction(
                FunSpec.builder(function.simpleName.asString())
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("id", function.parameters[0].type.toTypeName())
                    .returns(function.returnType!!.toTypeName())
                    .addCode("return skeleton.readEntity(id)")
                    .build()
            )
        }

        private fun generateUpdate(function: KSFunctionDeclaration) {
            FunSpec.builder(function.simpleName.asString())
                .addModifiers(KModifier.OVERRIDE)
                .addCode("TODO(\"ya eblan\")")
                .build()
        }

        private fun generateDelete(function: KSFunctionDeclaration) {
            FunSpec.builder(function.simpleName.asString())
                .addModifiers(KModifier.OVERRIDE)
                .addCode("TODO(\"ya eblan\")")
                .build()
        }
    }
}