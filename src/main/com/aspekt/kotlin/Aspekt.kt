import Cached.Companion.cached

abstract class Aspekt<V, R> {
    abstract val before: ((V) -> Unit)?
    abstract val after: ((R) -> Unit)?
    abstract val afterThrowing: ((Exception) -> Unit)?
    abstract val around: ((V) -> R?)?
}

class Cached<V, R>(transform: (V) -> R) : Aspekt<V, R>() {
    companion object {
        @JvmStatic fun <V, R> ((V) -> R).cached(): AspektInvoker<V,R> = AspektInvoker(this, listOf(Cached(this)))
    }

    //updates?
    val cache = HashMap<V,R>()

    override val before: ((V) -> Unit)? = null
    override val after: ((R) -> Unit)? = null
    override val afterThrowing: ((Exception) -> Unit)? = null
    override val around: ((V) -> R?)? = { cache.computeIfAbsent(it, transform) }
}
//TODO: other aspects? sl4j?
