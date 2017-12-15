import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

inline fun <E> Iterable<E>.peek(action: (E) -> Unit): Iterable<E> {
    for (element in this) action(element)
    return this
}

inline fun <T> supplyAsync(executor: Executor, noinline supplier: () -> T) : CompletableFuture<T> =
        CompletableFuture.supplyAsync(Supplier { supplier.invoke() }, executor)


inline fun <T> supplyAsync(noinline supplier: () -> T) : CompletableFuture<T> =
        CompletableFuture.supplyAsync { supplier.invoke() }
