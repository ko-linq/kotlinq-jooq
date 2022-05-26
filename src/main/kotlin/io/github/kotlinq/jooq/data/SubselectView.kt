package io.github.kotlinq.jooq.data

import io.github.kotlinq.jooq.DataView
import io.github.kotlinq.jooq.QueryDraft
import io.github.kotlinq.jooq.impl.RecordMapping
import org.jooq.Field
import org.jooq.Record1
import org.jooq.Select
import org.jooq.impl.DSL
import kotlin.reflect.KClass

class SubselectView<R: Any>(
    recordClass: KClass<R>,
    private val select: Select<*>       //already select with fields
): DataView<R> {

    private val recordMapper = RecordMapping.getRecordMapper(recordClass)

    private val fieldsByName = recordMapper.fields
        .mapIndexed { idx, (name, _) -> name!!.lowercase() to select.field(idx) }
        .toMap()

    override fun getField(name: String): Field<*> {
        return fieldsByName[name.lowercase()]!!
    }

    override fun getField(idx: Int): Field<*> {
        return select.field(idx)!!
    }

    override fun getField(): Field<*> {
        if (select.fields().size > 1) error("Cannot represent multi-field record as field")
        return getField(0)
    }

    override fun createSelect(
        fields: List<Field<*>>,
        queryDraft: QueryDraft
    ): Select<*> {
        if (fields.isEmpty() && queryDraft.isEmpty()) return select
        val table = select.asTable()
        return TableView(table).createSelect(fields, queryDraft)
    }

    override fun createSelectCount(
        queryDraft: QueryDraft
    ): Select<Record1<Int>> {
        if (queryDraft.isEmpty()) return DSL.selectCount().from(select)
        val table = select.asTable()
        return TableView(table).createSelectCount(queryDraft)
    }

    override fun toList(select: Select<*>): List<R> {
        return select.map { recordMapper.fromRecord(it) }
    }
}