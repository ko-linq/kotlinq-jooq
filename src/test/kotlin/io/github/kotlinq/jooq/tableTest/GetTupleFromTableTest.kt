package io.github.kotlinq.jooq.tableTest

import io.github.kotlinq.jooq.H2BaseTest
import io.github.kotlinq.jooq.selectQueryableFrom
import org.jooq.generated.Tables
import org.jooq.generated.tables.records.EmployeeRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetTupleFromTableTest: H2BaseTest() {

    private val ADMIN = EmployeeRecord(1, "admin", 66, true)
    private val USER = EmployeeRecord(2, "user", 33, false)

    @BeforeEach
    fun initData() {
        dslContext.batchStore(ADMIN, USER,).execute()
    }

    data class UserBirth(val username: String, val year: Int)

    @Test
    fun getTuple() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .map { UserBirth(it.username, 2020 - it.age) }
            .toList()
        assertEquals(
            listOf(UserBirth("admin", 2020 - 66), UserBirth("user", 2020 - 33)),
            records
        )
    }

    @Test
    fun getPairs() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .sortedBy { it.age }
            .map { Pair(it.username, 2020 - it.age) }
            .toList()
        assertEquals(
            listOf(Pair("user", 2020 - 33), Pair("admin", 2020 - 66)),
            records
        )
    }

    @Test
    fun getPairs_sortedAfterMap() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .map { Pair(it.username, 2020 - it.age) }
            .sortedBy { it.first }
            .toList()
        assertEquals(
            listOf(Pair("admin", 2020 - 66), Pair("user", 2020 - 33)),
            records
        )
    }

    @Test
    fun getPairs_filteredAfterMap() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .map { Pair(it.username, 2020 - it.age) }
            .filter { it.first == "admin" }
            .toList()
        assertEquals(
            listOf(Pair("admin", 2020 - 66)),
            records
        )
    }


    @Test
    fun getPairs_offsetLimitAfterMap() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .map { Pair(it.username, 2020 - it.age) }
            .drop(1)
            .toList()
        assertEquals(
            listOf(Pair("user", 2020 - 33)),
            records
        )
    }

    @Test
    fun getPairs_mapAfterMap() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .map { Pair(it.username, 2020 - it.age) }
            .map { Pair(it.second, it.first) }
            .toList()
        assertEquals(
            listOf(Pair(2020 - 66, "admin"), Pair(2020 - 33, "user")),
            records
        )
    }

    data class Username(
        val username: String,
        val username2: String
    )

    data class Age(
        val age: Int,
        val year: Int
    )

    data class ComplexUser(val username: Username, val age: Age)

    @Test
    fun getNestedTuples() {
        val records = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .map { ComplexUser(Username(it.username, it.username.uppercase()), Age(it.age.toInt(), 2022-it.age)) }
            .take(1)
            .toList()
        assertEquals(
            listOf(ComplexUser(Username("admin", "ADMIN"), Age(66, 2022-66))),
            records
        )
    }
}