import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.function.Supplier
import kotlin.coroutines.experimental.buildIterator


class NBAspektInvoker<V, R>(function: ((V) -> R), aspects: List<Aspekt<V, R>>)
    : AbstractAspektInvoker<V, R>(function, aspects) {

    companion object {

        @JvmStatic
        fun <V, R> ((V) -> R).nonBlocking(vararg aspects: Aspekt<V, R>)
                : NBAspektInvoker<V, R> = NBAspektInvoker(this, aspects.toList())

        @JvmStatic
        fun <V, R> createNonBlocking(function: (V) -> R)
                : NBAspektInvoker<V, R> = NBAspektInvoker(function, listOf())

        @JvmStatic
        fun <V, R> ((V) -> CompletableFuture<R>)
                .completeOn(other: () -> R,
                          predicate: (CompletableFuture<R>) -> Boolean): (V) -> R =

                {v: V ->
                    this.invoke(v)
                            .takeUnless(predicate)?.join()
                            ?: other.invoke()
                }

    }


    override fun withAspect(aspect: Aspekt<V, R>): NBAspektInvoker<V, R>
            = NBAspektInvoker(function, aspects.plus(aspect))

    fun apply(executor: Executor? = null): (V) -> CompletableFuture<R> =
            executor?.let { async(it) } ?: async()

    private fun async(): (V) -> CompletableFuture<R> =
            { v: V -> supplyAsync { execWithAspects(v) } }

    private fun async(executor: Executor): (V) -> CompletableFuture<R> =
            { v: V -> supplyAsync(executor) { execWithAspects(v) } }

    private fun execWithAspects(v: V): R = applyAspects().invoke(v)

}


class AspektInvoker<V, R>(function: ((V) -> R), aspects: List<Aspekt<V, R>>)
    : AbstractAspektInvoker<V, R>(function, aspects) {
    companion object {
        @JvmStatic
        fun <V, R> ((V) -> R).withAspects(vararg aspects: Aspekt<V, R>)
                : AspektInvoker<V, R> = AspektInvoker(this, aspects.toList())

        @JvmStatic
        fun <V, R> createAspects(function: (V) -> R)
                : AspektInvoker<V, R> = AspektInvoker(function, listOf())
    }

    override fun withAspect(aspect: Aspekt<V, R>): AspektInvoker<V, R>
            = AspektInvoker(function, aspects.toMutableList().also { it.add(aspect) })

    fun nonBlocking(): NBAspektInvoker<V, R>
            = NBAspektInvoker(function, aspects)

    fun apply(): (V) -> R = applyAspects()
}


abstract class AbstractAspektInvoker<V, R>
internal constructor(val function: ((V) -> R), val aspects: List<Aspekt<V, R>>) {

    companion object {
        @JvmStatic private fun <V, R> applyAspects(function: (V) -> R,
                                                   aspects: List<Aspekt<V, R>>): (V) -> R = {
            val initial = it
            aspects.forEach { it.before?.invoke(initial) }

            val arounds = aspects.filter(Objects::nonNull)

            val result = arounds.takeIf(List<Aspekt<V, R>>::isNotEmpty)
                    ?.map { it.around }
                    ?.map { it?.invoke(initial) }
                    ?.first { it != null }
                    ?: exceptionally(function::invoke, aspects)
                    .invoke(it)

            result.also { aspects.forEach { it.after?.invoke(result) } }

        }

        @JvmStatic private fun <V, R> exceptionally(action: (V) -> R, aspects: List<Aspekt<V, R>>): (V) -> R = {
            try {
                action.invoke(it)
            } catch (e: Exception) {
                aspects.forEach { it.afterThrowing?.invoke(e) }; throw e
            }
        }

    }


    abstract fun withAspect(aspect: Aspekt<V, R>): AbstractAspektInvoker<V, R>

    protected fun applyAspects(): (V) -> R = applyAspects()
}

















