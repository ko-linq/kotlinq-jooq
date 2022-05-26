### What is it

This library links together JOOQ and Kotlinq and allows to 
write code like this, using pure lambdas, without `DSL.*` methods:

```kotlin

import io.github.kotlinq.Kotlinq
import io.github.kotlinq.jooq.selectQueryableFrom
import org.jooq.generated.Tables
import org.jooq.impl.DSL


@Kotlinq
fun main() {
    DSL.using("jdbc:h2:./temp").use { dsl ->
        val uppercaseNames = dsl.selectQueryableFrom(Tables.EMPLOYEE)
            .filter { it.age > 18 }
            .sortedBy { 2022 - it.age }
            .map { "== ${it.username.uppercase()} ==" }
            .toList()
        println(uppercaseNames)

        val totalMinutes = dsl.selectQueryableFrom(Tables.TRACKED_TIME)
            .map { it.minutes }
            .aggregate { it.sum() }
        println(totalMinutes)
    }
}


```

### How to use

Add following to your `gradle.build`

```groovy
buildscript {
    repositories {
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.github.kotlinqs.kotlinq:com.github.kotlinqs.gradle.plugin:0.1-SNAPSHOT")
    }
}

apply(plugin="com.github.kotlinqs")

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.kotlinqs:kotlinq-jooq:0.1-SNAPSHOT")
}

```

See full example at https://github.com/kotlinqs/kotlinq-jooq
