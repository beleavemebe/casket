package space.solevarnya.casket

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Dao(
    @Suppress("unused")
    val entityClass: KClass<*>
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Entity(val tableFileName: String)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class PrimaryKey()

@Target(AnnotationTarget.FUNCTION)
annotation class Create()

@Target(AnnotationTarget.FUNCTION)
annotation class ReadAll()

@Target(AnnotationTarget.FUNCTION)
annotation class Read()

@Target(AnnotationTarget.FUNCTION)
annotation class Update()

@Target(AnnotationTarget.FUNCTION)
annotation class Delete()