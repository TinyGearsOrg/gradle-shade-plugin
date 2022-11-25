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
package org.tinygears.shade.gradle.testkit.file

import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ResultHandler

class Results: ResultHandler<Void> {
    private val lock    = Object()
    private var success = false

    val successful: Boolean
        get() = success && exception == null

    val failed: Boolean
        get() = exception != null

    var exception: GradleConnectionException? = null
        private set

    fun waitForCompletion() {
        synchronized(lock) {
            while(!successful && !failed) {
                lock.wait()
            }
        }
    }

    fun markComplete() {
        synchronized(lock) {
            success = true
            exception = null
            lock.notifyAll()
        }
    }

    fun markFailed(e: GradleConnectionException) {
        synchronized(lock) {
            success = false
            exception = e
            lock.notifyAll()
        }
    }

    override fun onComplete(aVoid: Void) {
        markComplete()
    }

    override fun onFailure(e: GradleConnectionException) {
        markFailed(e)
    }
}
