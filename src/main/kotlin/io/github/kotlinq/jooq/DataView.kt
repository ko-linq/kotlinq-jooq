package io.github.kotlinq.jooq

import org.jooq.*

interface DataView<R> {
    fun getField(name: String): Field<*>
    fun getField(idx: Int): Field<*>

    fun getField(): Field<*>

    fun createSelect(fields: List<Field<*>>, queryDraft: QueryDraft): Select<out Record>

    fun getAsTable(fields: List<Field<*>>, queryDraft: QueryDraft, alias: String): Table<out Record> = createSelect(fields, queryDraft).asTable(alias)

    fun createSelectCount(queryDraft: QueryDraft): Select<Record1<Int>>

    fun toList(select: Select<*>): List<R>

}