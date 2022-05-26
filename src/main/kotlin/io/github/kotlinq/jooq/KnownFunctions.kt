package io.github.kotlinq.jooq

import org.jooq.Field
import org.jooq.impl.DSL
import kotlin.reflect.KCallable

object KnownFunctions {

    private val aggregators: MutableMap<String, (Field<*>) -> Field<*>> = mutableMapOf()
    private val f0: MutableMap<String, () -> Field<*>> = mutableMapOf()
    private val f1: MutableMap<String, (Field<*>) -> Field<*>> = mutableMapOf()

    fun aggregate(name: String, arg: Field<*>): Field<*>? = aggregators[name.lowercase()]?.invoke(arg)

    fun invoke(name: String, vararg args: Any) = when(args.size) {
        0 -> f0[name.lowercase()]?.invoke()
        1 -> f1[name.lowercase()]?.invoke(args[0] as Field<*>)
        else -> error("not supported")
    }

    fun registerAggregator(name: String, fn: (Field<*>) -> Field<*>) {
        aggregators[name.lowercase()] = fn
        register(name, fn)
    }

    fun registerAggregator(method: KCallable<*>, fn: (Field<*>) -> Field<*>) {
        registerAggregator(method.name, fn)
    }

    fun register(name: String, fn0: () -> Field<*>) {
        f0[name.lowercase()] = fn0
    }

    fun register(name: String, fn1: (Field<*>) -> Field<*>) {
        f1[name.lowercase()] = fn1
    }

    init {
        registerAggregator(Iterable<Int>::maxOrNull) { field -> DSL.max(field) }
        registerAggregator(Iterable<Int>::minOrNull) { field -> DSL.min(field) }
        registerAggregator(Iterable<Int>::sum) { field -> DSL.sum(field as Field<out Number>?) }
        registerAggregator(Iterable<Int>::average) { field -> DSL.avg(field as Field<out Number>?) }

        register("now") { -> DSL.currentLocalDate() }

        register("toInt") { field -> field}
        register("uppercase") { field -> DSL.upper(field as Field<String>) }
    }
}