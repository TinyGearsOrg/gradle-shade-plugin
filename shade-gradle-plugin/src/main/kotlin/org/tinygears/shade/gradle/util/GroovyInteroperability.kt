/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.tinygears.shade.gradle.util

import groovy.lang.Closure

/**
 * Adapts a Kotlin function to a single argument Groovy [Closure].
 *
 * @param T the expected type of the single argument to the closure.
 * @param action the function to be adapted.
 *
 * @see [KotlinClosure1]
 */
fun <T, V: Any> Any.closureOf(action: T.() -> V): Closure<Any?> =
    KotlinClosure1(action, this, this)

/**
 * Adapts an unary Kotlin function to an unary Groovy [Closure].
 *
 * @param T the type of the single argument to the closure.
 * @param V the return type.
 * @param function the function to be adapted.
 * @param owner optional owner of the Closure.
 * @param thisObject optional _this Object_ of the Closure.
 *
 * @see [Closure]
 */
class KotlinClosure1<in T : Any?, V : Any>(
    val function: T.() -> V?,
    owner: Any? = null,
    thisObject: Any? = null) : Closure<V?>(owner, thisObject) {

    @Suppress("unused") // to be called dynamically by Groovy
    fun doCall(it: T): V? = it.function()
}

/**
 * Enables function invocation syntax on [Closure] references.
 */
operator fun <T> Closure<T>.invoke(): T = call()


/**
 * Enables function invocation syntax on [Closure] references.
 */
operator fun <T> Closure<T>.invoke(x: Any?): T = call(x)


/**
 * Enables function invocation syntax on [Closure] references.
 */
operator fun <T> Closure<T>.invoke(vararg xs: Any?): T = call(*xs)
