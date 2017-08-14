import java.util.concurrent.CompletableFuture

class NonBlockingAspektInvoker<V, R>(function: ((V) -> R), aspects: List<Aspekt<V, R>>)
    : _AspektInvoker<V, R>(function, aspects) {
    companion object {
        @JvmStatic fun <V, R> ((V) -> R).nonBlocking(vararg aspects: Aspekt<V, R>)
            : NonBlockingAspektInvoker<V, R> = NonBlockingAspektInvoker(this, aspects.toList())

        @JvmStatic fun <V, R> createNonBlocking(function: (V) -> R)
            : NonBlockingAspektInvoker<V, R> = NonBlockingAspektInvoker(function, listOf())
    }
    override fun withAspect(aspect: Aspekt<V, R>) : NonBlockingAspektInvoker<V, R>
        = NonBlockingAspektInvoker(function, aspects.toMutableList().also { it.add(aspect) })

    fun apply() : (V) -> CompletableFuture<R>
        = { CompletableFuture.supplyAsync { _apply().invoke(it) } }

}

class AspektInvoker<V, R>(function: ((V) -> R), aspects: List<Aspekt<V, R>>)
    : _AspektInvoker<V, R>(function, aspects) {
    companion object {
        @JvmStatic fun <V, R> ((V) -> R).withAspects(vararg aspects: Aspekt<V, R>)
            : AspektInvoker<V, R> = AspektInvoker(this, aspects.toList())

        @JvmStatic fun <V, R> createAspects(function: (V) -> R)
            : AspektInvoker<V, R> = AspektInvoker(function, listOf())
    }

    override fun withAspect(aspect: Aspekt<V, R>) : AspektInvoker<V, R>
        = AspektInvoker(function, aspects.toMutableList().also { it.add(aspect) })

    fun nonBlocking() : NonBlockingAspektInvoker<V, R>
        = NonBlockingAspektInvoker(function, aspects)

    fun apply() : (V) -> R = _apply()
}


abstract class _AspektInvoker<V, R>
    internal constructor(val function: ((V) -> R), val aspects: List<Aspekt<V, R>>) {

    abstract fun withAspect(aspect: Aspekt<V, R>) : _AspektInvoker<V, R>

    protected fun _apply() : (V) -> R =  {
        val initial = it
        aspects.forEach{ it.before?.invoke(initial) }

        fun exceptionally(action: (V) -> R) : (V) -> R = {
            try { action.invoke(it) }
            catch (e: Exception) {
                aspects.forEach {it.afterThrowing?.invoke(e)}; throw e
            } }

        val arounds = aspects.filter { it.around != null }

        val result = arounds.takeIf(List<Aspekt<V,R>>::isNotEmpty)
            ?.map { it.around }
            ?.map { it?.invoke(initial) }
            ?.filter { it != null }
            ?.first()
            ?: exceptionally(function::invoke).invoke(it)

        result.also { aspects.forEach { it.after?.invoke(result) }}
    }

}

