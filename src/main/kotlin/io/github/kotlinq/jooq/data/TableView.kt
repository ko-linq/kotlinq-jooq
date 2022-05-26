package io.github.kotlinq.jooq.data

import io.github.kotlinq.jooq.DataView
import io.github.kotlinq.jooq.QueryDraft
import org.jooq.*
import org.jooq.impl.DSL

class TableView<R : Record>(
    private val table: Table<R>
): DataView<R> {

    private val normalizedFields = table.fields().associateBy { it.name.replace("_", "").lowercase() }

    override fun getField(name: String): Field<*> {
        return normalizedFields[name.lowercase()] ?: error("Cannot find column '$name' in '${table.name}")
    }

    override fun getAsTable(fields: List<Field<*>>, queryDraft: QueryDraft, alias: String): Table<out Record> {
        if (queryDraft.isEmpty()) return table.`as`(alias)
        return super.getAsTable(fields, queryDraft, alias)
    }

    override fun getField(idx: Int): Field<*> {
        return table.field(idx)!!
    }

    override fun getField(): Field<*> {
        error("Cannot use table row as field")
    }

    override fun createSelect(
        fields: List<Field<*>>,
        queryDraft: QueryDraft
    ): Select<out Record> {

        val query = if (fields.isEmpty()) {
            DSL.selectFrom(table)
        } else {
            DSL.select(fields.mapIndexed { idx, field ->
                if (field in table.fields())
                    field
                else
                    field.`as`("col$idx")
            }).from(table)
        }
        queryDraft.applyTo(query)
        return query
    }

    override fun createSelectCount(
        queryDraft: QueryDraft
    ): Select<Record1<Int>> {
        val query = DSL.selectCount().from(table)
        queryDraft.applyTo(query)
        return query
    }

    override fun toList(select: Select<*>): List<R> {
        return select.toList() as List<R>
    }

}