package space.solevarnya.casket

object Casket

@Suppress("unused")
inline fun <reified T> Casket.obtainDao(): T {
    val daoImplPath = "space.solevarnya.casket.generated.${T::class.simpleName}_Impl"
    return Class.forName(daoImplPath)
        .constructors[0]
        .newInstance() as T
}