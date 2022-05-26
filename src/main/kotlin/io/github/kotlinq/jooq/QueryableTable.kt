package io.github.kotlinq.jooq

import io.github.kotlinq.Kotlinq
import io.github.kotlinq.collections.DataQueryable
import io.github.kotlinq.collections.Queryable
import io.github.kotlinq.expression
import io.github.kotlinq.expression.node.Call
import io.github.kotlinq.expression.node.Node
import io.github.kotlinq.expression.node.Ref
import io.github.kotlinq.jooq.data.SingleValueView
import io.github.kotlinq.jooq.data.SubselectView
import io.github.kotlinq.jooq.data.TableView
import io.github.kotlinq.jooq.impl.NodeToJooq
import io.github.kotlinq.jooq.impl.RecordMapping
import org.jooq.*
import org.jooq.impl.DSL

@Kotlinq(off = true)
class QueryableTable<E>(
    private val view: DataView<E>,
    private var configuration: Configuration? = null
) : DataQueryable<E, Jooq>, Attachable {

    private val conditions = mutableListOf<Condition>()
    private val sort = mutableListOf<OrderField<*>>()
    private var offset: Int? = null
    private var limit: Int? = null

    private val mapper = NodeToJooq(listOf(view))

    override fun filter(predicate: (E) -> Boolean): QueryableTable<E> {
        conditions += mapper.condition(predicate.expression)
        return this
    }

    private fun createSelect(): Select<*> {
        return view.createSelect(emptyList(), queryDraft())
    }

    private fun queryDraft(): QueryDraft {
        return QueryDraft(conditions, sort, offset, limit)
    }

    private fun createSelectCount(): Select<Record1<Int>> {
        return view.createSelectCount(queryDraft())
    }

    override fun <C : Comparable<C>> sortedDescendingBy(selector: (E) -> C): QueryableTable<E> {
        sort += mapper.field<Any>(selector.expression).desc()
        return this
    }

    override fun <C : Comparable<C>> sortedBy(selector: (E) -> C): QueryableTable<E> {
        sort += mapper.field<Any>(selector.expression).asc()
        return this
    }

    override fun toList(): List<E> {
        return view.toList(createSelect().attached())
    }

    override fun <R, Result> join(
        another: DataQueryable<R, Jooq>,
        condition: (E, R) -> Boolean,
        construct: (E, R) -> Result
    ): DataQueryable<Result, Jooq> {
        val anotherCasted = another as QueryableTable<R>

        val tableLeft = view.getAsTable(emptyList(), queryDraft(), "left")
        val tableRight = anotherCasted.view.getAsTable(emptyList(), anotherCasted.queryDraft(), "right")

        val leftAsDataView = TableView(tableLeft)
        val rightAsDataView = TableView(tableRight)
        val joinMapper = NodeToJooq(listOf(leftAsDataView, rightAsDataView))
        val conditionExpr = joinMapper.condition(condition.expression)

        //todo: need to support more expressions
        val call = construct.expression as Call
        val method = call.method as Ref
        val tupleClass = method.returnClass
        val fields = RecordMapping.toFieldsList(call) { joinMapper.field<Any>(it) }
        val select = DSL.select(fields).from(tableLeft).innerJoin(tableRight).on(conditionExpr).attached()
        return QueryableTable(
            SubselectView(tupleClass, select),
            configuration
        ) as QueryableTable<Result>
    }

    override fun count(): Long {
        val select = createSelectCount().attached()
        return select.fetchSingle().value1().toLong()
    }

    override fun drop(offset: Int): QueryableTable<E> {
        check(this.offset == null)
        this.offset = offset
        return this
    }

    override fun <R> map(mapper: (E) -> R): QueryableTable<R> {
        val expr = mapper.expression
        return map(expr)
    }

    private fun <R> map(expr: Node): QueryableTable<R> {
        return when (expr) {
            is Call -> {
                val argFields = RecordMapping.toFieldsList(expr) { mapper.field<Any>(it) }
                QueryableTable(
                    SubselectView(
                        RecordMapping.getReturnClass(expr),
                        view.createSelect(argFields, queryDraft())
                    ),
                    configuration()
                ) as QueryableTable<R>
            }
            else -> {
                //try to calculate
                val field = mapper.field<Any>(expr)
                QueryableTable(
                    SingleValueView(view.createSelect(listOf(field), queryDraft())),
                    configuration()
                ) as QueryableTable<R>
            }
        }
    }

    override fun <R> aggregate(aggFn: (Iterable<E>) -> R): R {
        val aggregationField = mapper.aggregation<Any>(aggFn.expression)
        return view.createSelect(listOf(aggregationField), queryDraft()).attached().fetchSingle()[0] as R
    }

    override fun take(limit: Int): Queryable<E> {
        check(this.limit == null)
        this.limit = limit
        return this
    }

    override fun attach(configuration: Configuration?) {
        this.configuration = configuration
    }

    override fun detach() {
        configuration = null
    }

    override fun configuration(): Configuration? {
        return configuration
    }

    private fun <A : Attachable> A.attached(): A = apply {
        attach(configuration)
    }

}
