package io.github.kotlinq.jooq

import org.jooq.CloseableDSLContext
import org.jooq.ExecuteContext
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import org.jooq.impl.DefaultExecuteListener
import org.jooq.impl.DefaultExecuteListenerProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

abstract class H2BaseTest: DefaultExecuteListener() {

    init {
        Class.forName("org.h2.Driver")
    }

    lateinit var dslContext: CloseableDSLContext

    var lastExecutedSQL: String? = null

    override fun executeStart(ctx: ExecuteContext) {
        lastExecutedSQL = ctx.query()?.getSQL(ParamType.INLINED)
    }

    @BeforeEach
    fun setup() {
        dslContext = DSL.using("jdbc:h2:./temp")
        dslContext.configuration().apply {
            set(DefaultExecuteListenerProvider(this@H2BaseTest))
        }
        dslContext.execute(this::class.java.getResourceAsStream("/schema.sql")!!.bufferedReader().readText())
    }

    @AfterEach
    fun tearDown() {
        dslContext.close()
        Path("./temp.mv.db").deleteIfExists()
    }
}