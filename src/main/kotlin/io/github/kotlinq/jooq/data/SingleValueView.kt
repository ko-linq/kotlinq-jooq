package io.github.kotlinq.jooq.data

import io.github.kotlinq.jooq.DataView
import io.github.kotlinq.jooq.QueryDraft
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Select
import org.jooq.impl.DSL

class SingleValueView<T: Any>(
    private val select: Select<*>
): DataView<T> {

    init {
        require(select.fields().size == 1)
    }

    private val singleField = select.field(0)!!

    override fun getField(name: String): Field<*> {
        if (name == singleField.name) return singleField
        error("cannot get field by name from scalar value")
    }

    override fun getField(idx: Int): Field<*> {
        return select.field(idx)!!
    }

    override fun getField(): Field<*> {
        return singleField
    }

    override fun createSelect(
        fields: List<Field<*>>,
        queryDraft: QueryDraft
    ): Select<out Record> {
        require(fields.size <= 1)
        if (fields.isEmpty() && queryDraft.isEmpty()) return select
        val query = if (fields.isEmpty()) {
            DSL.selectFrom(select.asTable())
        } else {
            DSL.select(fields).from(select.asTable())
        }
        queryDraft.applyTo(query)

        return query
    }

    override fun createSelectCount(
        queryDraft: QueryDraft
    ): Select<Record1<Int>> {
        if (queryDraft.isEmpty()) return DSL.selectCount().from(select)

        val query = DSL.selectCount().from(select.asTable())
        queryDraft.applyTo(query)
        return query
    }

    override fun toList(select: Select<*>): List<T> {
        return select.map { it[0] as T }
    }
}