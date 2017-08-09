import AspektInvoker.Companion.withAspects
import Logger.Companion.logging


class AspektInvoker<V, R>(val function: ((V) -> R), val aspects: List<Aspekt<V, R>>) {
    companion object {
        @JvmStatic fun <V, R> ((V) -> R).withAspects(vararg aspects: Aspekt<V, R>): AspektInvoker<V, R> = AspektInvoker(this, aspects.toList())
        @JvmStatic fun <V, R> create(function: (V) -> R) : AspektInvoker<V, R> = AspektInvoker(function, listOf())
    }
    fun withAspect(aspect: Aspekt<V, R>) : AspektInvoker<V, R> = AspektInvoker(function, aspects.toMutableList().also { it.add(aspect) })

    fun apply() : (V) -> R =  {
        val initial = it
        val arounds = aspects.toList()
            .peek { it.before?.invoke(initial) }
            .filter { it.around != null }

        fun handleException(action: (V) -> R) : (V) -> R = {
            try {
                action.invoke(it)
            } catch (e: Exception) {
                aspects.forEach {it.afterThrowing?.invoke(e)}
                throw e
            }
        }

        val result = arounds.takeIf { it.isNotEmpty() }
            ?.first()
            .let { it?.around?.invoke(initial) }
            ?: handleException(function::invoke).invoke(it)

        result.run { aspects
                .toList()
                .parallelStream()
                .forEach {  it.after?.invoke(result) } }
        result
    }

}

abstract class Aspekt<V, R> {
    abstract val before: ((V) -> Unit)?
    abstract val after: ((R) -> Unit)?
    abstract val afterThrowing: ((Exception) -> Unit)?
    abstract val around: ((V) -> R)?
}

class Logger<V, R> : Aspekt<V, R>() {
    companion object {
        @JvmStatic fun <V, R> ((V) -> R).logging(): AspektInvoker<V,R> = AspektInvoker(this, listOf(Logger()))
    }
    override val before: (V) -> Unit = { println("sosi $it") }
    override val after: (R) -> Unit = { println("hooy $it") }
    override val afterThrowing: ((Exception) -> Unit)? = null
    override val around: ((V) -> R)? = null
}

val doShit: (String) -> String = { _: String -> "aaaa"}.withAspects(Logger()).apply()
val doShit2: (String) -> String = { _: String -> "bbbb"}.logging().apply()

fun main(args: Array<String>) {
    doShit("suka")
    doShit2("blyat")

}



inline fun <E> Iterable<E>.peek(action: (E) -> Unit): Iterable<E> {
    for (element in this) action(element)
    return this
}