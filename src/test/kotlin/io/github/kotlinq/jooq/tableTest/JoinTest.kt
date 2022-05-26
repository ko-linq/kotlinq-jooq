package io.github.kotlinq.jooq.tableTest

import io.github.kotlinq.jooq.H2BaseTest
import io.github.kotlinq.jooq.selectQueryableFrom
import org.jooq.generated.Tables
import org.jooq.generated.tables.records.EmployeeRecord
import org.jooq.generated.tables.records.TrackedTimeRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class JoinTest: H2BaseTest() {

    private val ADMIN = EmployeeRecord(1, "admin", 66, true)
    private val USER = EmployeeRecord(2, "user", 33, false)

    private val TT1 = TrackedTimeRecord(1, ADMIN.id, LocalDate.of(2020, 1, 1), 55)
    private val TT2 = TrackedTimeRecord(2, USER.id, LocalDate.of(2020, 1, 1), 33)
    private val TT3 = TrackedTimeRecord(3, ADMIN.id, LocalDate.of(2020, 1, 2), 44)

    @BeforeEach
    fun initData() {
        dslContext.batchStore(ADMIN, USER, TT1, TT2, TT3).execute()
    }

    data class UserTT(val username: String, val date: LocalDate, val minutes: Int)

    @Test
    fun join() {
        val result = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .join<TrackedTimeRecord, UserTT>(
                dslContext.selectQueryableFrom(Tables.TRACKED_TIME),
                { user, tt -> user.id == tt.userId }
            ) { user, tt -> UserTT(user.username, tt.date, tt.minutes) }
            .sortedDescendingBy { it.minutes }
            .map { "${it.username} tracked ${it.minutes} minutes at ${it.date}" }
            .toList()

        // select (((("alias_94962172"."USERNAME" || ' tracked ') || cast("alias_94962172"."MINUTES" as varchar)) || ' minutes at ') || cast("alias_94962172"."DATE" as varchar)) "col0"
        //   from (select "left"."USERNAME", "right"."DATE", "right"."MINUTES"
        //            from "EMPLOYEE" "left"
        //            join "TRACKED_TIME" "right" on "left"."ID" = "right"."USER_ID"
        //         ) "alias_94962172" order by "alias_94962172"."MINUTES" desc

        assertEquals(listOf(
            "admin tracked 55 minutes at 2020-01-01",
            "admin tracked 44 minutes at 2020-01-02",
            "user tracked 33 minutes at 2020-01-01",
        ), result)
    }

    @Test
    fun joinAndAggregate() {
        val sumOfAdminMinutes = dslContext.selectQueryableFrom(Tables.EMPLOYEE)
            .join<TrackedTimeRecord, UserTT>(
                dslContext.selectQueryableFrom(Tables.TRACKED_TIME),
                { user, tt -> user.id == tt.userId }) { user, tt -> UserTT(user.username, tt.date, tt.minutes) }
            .filter { it.username.uppercase() == "ADMIN" }
            .map { it.minutes }
            .aggregate { it.sum() }

        println(lastExecutedSQL)
        assertEquals(99, sumOfAdminMinutes)

    }

}