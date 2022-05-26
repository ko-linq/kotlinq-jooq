package io.github.kotlinq.jooq.impl

import io.github.kotlinq.expression.node.*
import io.github.kotlinq.jooq.DataView
import io.github.kotlinq.jooq.KnownFunctions
import org.jooq.Condition
import org.jooq.Field
import org.jooq.impl.DSL

class NodeToJooq(
    private val lambdaArguments: List<DataView<*>>
) {
    fun <T> field(node: Node): Field<T> {
        return DataViewNodeToJooq(this).field(node)
    }

    fun <T> field(node: Node, argument: LambdaArgument): Field<T> {
        return DataViewNodeToJooq(this, lambdaArguments[argument.number]).field(node)
    }

    fun condition(node: Node): Condition {
        return DataViewNodeToJooq(this).condition(node)
    }

    fun <T> aggregation(node: Node): Field<T> {
        return DataViewNodeToJooq(this).aggregation(node)
    }
}

class DataViewNodeToJooq(
    private val nodeToJooq: NodeToJooq,
    private val dataView: DataView<*>? = null
) {

    fun <T> field(node: Node): Field<T> {

        return when(node) {
            is Unknown -> error("Impossible to use unknown node: ${node}")
            is UnaryMinus -> DSL.minus(field<Number>(node))
            is UnaryPlus -> field(node)
            is UnaryBang -> DSL.not(field<Boolean>(node))
            is Plus -> field<T>(node.left).plus(field<T>(node.right))
            is Minus -> field<T>(node.left).minus(field<T>(node.right))
            is Multiply -> field<Number>(node.left).mul(field<Number>(node.right))
            is Divide -> field<Number>(node.left).div(field<Number>(node.right))
            is And, is Or -> error("Cannot use logical operations as fields")
            is Equal, is NotEqual, is Less,
            is Greater, is GreaterOrEqual, is LessOrEqual,
                -> error("Cannot use comparison operations as fields")
            is GetProperty -> when {
                node.left is LambdaArgument -> nodeToJooq.field(node.right, node.left as LambdaArgument)
                else -> error("Cannot parse $node")
            }
            is Concat -> DSL.concat(*node.children.map { field<String>(it) }.toTypedArray())
            is Identifier -> dataView?.getField(node.name) ?: error("Unknown identifier ${node.name}")
            is Value -> DSL.value(node.value)
            is LambdaArgument -> dataView?.getField() ?: nodeToJooq.field(node, node)
            is Call -> call(node)
            is Val -> DSL.value(node.value)
            is Ref -> error("Cannot use references")
            is Error -> error(node.errorMessage)
        } as Field<T>
    }

    fun <T> call(node: Call): Field<T> {
        val method = node.method
        var arguments = node.children.drop(1)
        val methodName = when {
            method is IdentifiedNode -> method.name
            method is GetProperty && method.right is IdentifiedNode -> {
                //todo: support more levels
                arguments = listOf(method.left) + arguments
                (method.right as IdentifiedNode).name
            }
            else -> error("Cannot process call $node")
        }
        return (KnownFunctions.invoke(methodName, *arguments.map { field<Any>(it) }.toTypedArray())
            ?: error("Unknown function $methodName")) as Field<T>
    }

    fun condition(node: Node): Condition {
        val secondChildIsNull = (node is TwoChildren && node.right == Value(null))
        return when {
            node is And -> condition(node.left).and(condition(node.right))
            node is Or -> condition(node.left).or(condition(node.right))
            node is Equal && secondChildIsNull -> field<Any>(node.left).isNull
            node is Equal -> field<Any>(node.left).eq(field<Any>(node.right) as Field<*>)
            node is NotEqual && secondChildIsNull -> field<Any>(node.left).notEqual(field<Any>(node.right) as Field<*>)
            node is Less -> field<Any>(node.left).lessThan(field<Any>(node.right) as Field<*>)
            node is LessOrEqual -> field<Any>(node.left).lessOrEqual(field<Any>(node.right) as Field<*>)
            node is Greater -> field<Any>(node.left).greaterThan(field<Any>(node.right) as Field<*>)
            node is GreaterOrEqual -> field<Any>(node.left).greaterOrEqual(field<Any>(node.right) as Field<*>)
            node is UnaryBang -> condition(node.child).not()
            node is GetProperty
                    || node is Value
                    || node is Val
                    || node is Call
                    || node is LambdaArgument
            -> field<Any>(node).isTrue
            else -> error("Cannot get conditions from $node")

        }
    }

    fun <T> aggregation(node: Node): Field<T> {
        return when (node) {
            is Call -> call<T>(node)
            else -> error("Not supported aggregation by $node")
        }
    }


}