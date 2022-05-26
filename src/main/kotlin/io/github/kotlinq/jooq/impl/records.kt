package io.github.kotlinq.jooq.impl


import io.github.kotlinq.expression.node.Call
import io.github.kotlinq.expression.node.Node
import io.github.kotlinq.expression.node.Ref
import org.jooq.Field
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

class RecordMapper<R: Any>(
    private val klass: KClass<R>,
    mapping: RecordMapping
) {
    init {
        require(klass.isData) { "Mapping works for data classes only!" }
        requireNotNull(klass.primaryConstructor) { "Mapping works for data classes with primary constructor only!" }
        require(klass.constructors.size == 1) { "Mapping works for data classes with primary constructor only!" }
    }
    private val primaryConstructor = klass.primaryConstructor!!


    val fields = primaryConstructor
        .parameters
        .map {
            it.name to mapping.getRecordMapperOrNullWithoutCaching(it.type)
        }

    fun fromRecord(record: org.jooq.Record): R {
        return fromRecord(RecordCursor(record))
    }

    internal fun fromRecord(cursor: RecordCursor): R {
        val argsForThisConstructor = mutableListOf<Any?>()
        for ((_, mapper) in fields) {
            argsForThisConstructor += if (mapper == null) {
                cursor.getNextValue()
            } else {
                mapper.fromRecord(cursor)
            }
        }
        return primaryConstructor.call(*argsForThisConstructor.toTypedArray())
    }

    fun toFieldsList(node: Call, convert: (Node) -> Field<*>): List<Field<*>> {
        val list = mutableListOf<Field<*>>()
        check(node.children.size - 1 == fields.size) { "Call does not match primary constructor!" }
        for (ci in 1 until node.children.size) {
            val mapper = fields[ci-1].second
            val childNode = node.children[ci]
            if (mapper == null) {
                list += convert(childNode).`as`(fields[ci-1].first)
            } else {
                check(childNode is Call) { "Expected call for constructor of ${mapper.klass}, but here is ${childNode} " }
                check((childNode.method as? Ref)?.returnClass == mapper.klass)
                list += mapper.toFieldsList(childNode, convert)
            }
        }
        return list
    }
}

object RecordMapping {

    private val cache = ConcurrentHashMap<KClass<*>, RecordMapper<*>>()

    fun <R: Any> createFromRecord(record: org.jooq.Record, kclass: KClass<R>): R {
        return getRecordMapper(kclass).fromRecord(RecordCursor(record)) as R
    }

    fun toFieldsList(node: Call, convert: (Node) -> Field<*>): List<Field<*>> {
        val method = node.children[0] as Ref
        return getRecordMapper(method.returnClass).toFieldsList(node, convert)
    }

    fun getReturnClass(node: Call) = (node.children[0] as Ref).returnClass

    private fun <R: Any> createMapper(kclass: KClass<R>): RecordMapper<R> {
        return RecordMapper(kclass, this)
    }

    internal fun getRecordMapperOrNullWithoutCaching(type: KType): RecordMapper<*>? {
        val classifier = type.classifier
        if (classifier !is KClass<*> || !classifier.isData) return null
        return cache.getOrElse(classifier) { createMapper(classifier) }
    }

    fun <R: Any> getRecordMapper(recordClass: KClass<R>): RecordMapper<R> {
        return cache.getOrPut(recordClass) { createMapper(recordClass) } as RecordMapper<R>
    }
}


internal class RecordCursor(val record: org.jooq.Record) {
    var index = 0
    fun getNextValue(): Any? = record.get(index++)
}

