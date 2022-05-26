package io.github.kotlinq.jooq

import io.github.kotlinq.jooq.data.TableView
import org.jooq.DSLContext
import org.jooq.Table
import org.jooq.TableRecord

object Jooq

/**
 * Returns [QueryableTable] data source pointed to provided [table]
 */
fun <R : TableRecord<R>> DSLContext.selectQueryableFrom(table: Table<R>): QueryableTable<R> {
    return QueryableTable(TableView(table), configuration())
}

