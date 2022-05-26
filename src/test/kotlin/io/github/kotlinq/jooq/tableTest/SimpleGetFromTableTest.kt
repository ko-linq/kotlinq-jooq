package io.github.kotlinq.jooq.tableTest

import io.github.kotlinq.jooq.H2BaseTest
import io.github.kotlinq.jooq.selectQueryableFrom
import org.jooq.generated.Tables
import org.jooq.generated.tables.records.EmployeeRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SimpleGetFromTableTest: H2BaseTest() {

    private val ADMIN = EmployeeRecord(1, "admin", 66, true)
    private val USER = EmployeeRecord(2, "user", 33, false)

    @BeforeEach
    fun initData() {
        dslContext.batchStore(ADMIN, USER,).execute()
    }

    @Test
    fun get() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE).toList()
        assertEquals(
            """ select $allEmployeeFields from "EMPLOYEE" """.trim(),
            lastExecutedSQL
        )
        assertEquals(listOf(ADMIN, USER), records)
    }

    @Test
    fun getCount() {
        val count = dslContext.selectQueryableFrom(Tables.EMPLOYEE).count()
        assertEquals(
            """ select count(*) from "EMPLOYEE" """.trim(),
            lastExecutedSQL
        )
        assertEquals(2, count)
    }

    @Test
    fun filterByInt() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .filter { it.age > 44 }
            .toList()
        assertEquals(
            """ select $allEmployeeFields from "EMPLOYEE" where "EMPLOYEE"."AGE" > 44 """.trim(),
            lastExecutedSQL
        )
        assertEquals(listOf(ADMIN,), records)
    }

    @Test
    fun sortByInt() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .sortedBy { it.age }
            .toList()
        assertEquals(
            """ select $allEmployeeFields from "EMPLOYEE" order by "EMPLOYEE"."AGE" asc """.trim(),
            lastExecutedSQL
        )
        assertEquals(listOf(USER, ADMIN), records)
    }

    @Test
    fun max() {
        val max = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .map { it.age }
            .aggregate { it.maxOrNull() }
        assertEquals(
            """ select max("alias_12764853"."AGE") from (select "EMPLOYEE"."AGE" from "EMPLOYEE") "alias_12764853" """.trim(),
            lastExecutedSQL
        )
        assertEquals(66, max)
    }
}

