package io.github.kotlinq.jooq

import org.jooq.Condition
import org.jooq.OrderField
import org.jooq.SelectWhereStep

data class QueryDraft(
    val conditions: List<Condition>,
    val sort: List<OrderField<*>>,
    val offset: Int?,
    val limit: Int?
) {
    fun isEmpty() = conditions.isEmpty() && sort.isEmpty() && offset == null && limit == null

    fun applyTo(query: SelectWhereStep<*>) {
        query.where(conditions)
        query.orderBy(sort)
        offset?.let { query.offset(it) }
        limit?.let { query.limit(it) }
    }
}