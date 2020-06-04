package com.lukevanoort.stuntman

import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

/** SMTaskDataWad provides a generic method for storing data
 * about a task that can be used to recreate the task from
 * scratch.
 *
 * Keys may only contain alphanumeric values plus ':' and '_' or an
 * IllegalArgumentException will be thrown
 */

interface SMTaskReadableDataWad {

    abstract fun getString(key: String): String?
    abstract fun getString(key: String, default: String): String
    abstract fun getStringArray(key: String): Array<String>?
    abstract fun getStringArray(key: String, default: Array<String>): Array<String>
    abstract fun getInt(key: String): Int?
    abstract fun getInt(key: String, default: Int): Int
    abstract fun getIntArray(key: String): Array<Int>?
    abstract fun getIntArray(key: String, default: Array<Int>): Array<Int>
    abstract fun getValues(): Sequence<Pair<String, SMWadDatapoint>>

    abstract fun provideChildReadableNamespacedWad(namespace: String): SMTaskReadableDataWad
    abstract fun getPopulatedReadableChildNamespaces(): List<Pair<String,SMTaskReadableDataWad>>
}

interface SMTaskWritableDataWad {

    abstract fun store(key: String, value: String)
    abstract fun store(key: String, value: Array<String>)

    abstract fun store(key: String, value: Int)
    abstract fun store(key: String, value: Array<Int>)

    abstract fun deleteKey(key: String)

    abstract fun provideChildWritableNamespacedWad(namespace: String): SMTaskWritableDataWad
    abstract fun getPopulatedWritableChildNamespaces(): List<Pair<String,SMTaskWritableDataWad>>
}

interface SMTaskDataWad : SMTaskWritableDataWad, SMTaskReadableDataWad {
    abstract fun provideChildNamespacedWad(namespace: String): SMTaskDataWad
    abstract fun getPopulatedChildNamespaces(): List<Pair<String,SMTaskDataWad>>
}

interface SMTaskWadPersister {
    fun writeWad(wad: SMTaskDataWad)

    /** readWad will read in an SMPersistentTaskWad from a source,
     * if the source is empty, it should return an empty Wad
     */
    fun readWad(): SMTaskDataWad
}

private fun String.verifyKey() {
    val nonNum = this.find {
        it != ':' && it != '_' && !it.isLetterOrDigit()
    }
    if (nonNum != null) {
        throw IllegalArgumentException(String.format("unsupported character %c in key, keys must only contain letters, digits, or ':'",nonNum))
    }
}

class SMMapTaskWad : SMTaskDataWad {
    // this is a first draft implementation that is inefficient but quicker to implement
    // it would be better to implement this using a trie and having namespaces simply be views
    // into the trie delimited by their namespace
    private val values = ConcurrentHashMap<String, SMWadDatapoint>()

    private val childNamespaces = HashMap<String,SMMapTaskWad>()
    private val nsLock = ReentrantReadWriteLock()

    fun valueSize():Int {
        return values.size
    }

    fun childNsSize(): Int {
        nsLock.readLock().lock()
        val size = childNamespaces.size
        nsLock.readLock().unlock()
        return size
    }

    override fun store(key: String, value: String) {
        key.verifyKey()
        values[key] = SMWadDatapoint.StringData(value = value)
    }

    override fun store(key: String, value: Array<String>) {
        key.verifyKey()
        values[key] = SMWadDatapoint.StringArrayData(value = value)
    }

    override fun store(key: String, value: Int) {
        key.verifyKey()
        values[key] = SMWadDatapoint.IntData(value = value)
    }

    override fun store(key: String, value: Array<Int>) {
        key.verifyKey()
        values[key] = SMWadDatapoint.IntArrayData(value = value)
    }

    override fun getString(key: String): String? {
        return values[key]?.let {
            when(it) {
                is SMWadDatapoint.StringData -> it.value
                else -> null
            }
        }
    }

    override fun getString(key: String, default: String): String {
        return values[key]?.let {
            when(it) {
                is SMWadDatapoint.StringData -> it.value
                else -> default
            }
        } ?: default
    }

    override fun getStringArray(key: String): Array<String>? {
        return values[key]?.let {
            when(it) {
                is SMWadDatapoint.StringArrayData -> it.value
                else -> null
            }
        }
    }

    override fun getStringArray(key: String, default: Array<String>): Array<String> {
        return values[key]?.let {
            when(it) {
                is SMWadDatapoint.StringArrayData -> it.value
                else -> default
            }
        } ?: default
    }

    override fun getInt(key: String): Int? {
        return values[key]?.let {
            when(it) {
                is SMWadDatapoint.IntData -> it.value
                else -> null
            }
        }
    }

    override fun getInt(key: String, default: Int): Int {
        return values[key]?.let {
            when(it) {
                is SMWadDatapoint.IntData -> it.value
                else -> default
            }
        } ?: default
    }

    override fun getIntArray(key: String): Array<Int>? {
        return values[key]?.let {
            when(it) {
                is SMWadDatapoint.IntArrayData -> it.value
                else -> null
            }
        }
    }

    override fun getIntArray(key: String, default: Array<Int>): Array<Int> {
        return values[key]?.let {
            when(it) {
                is SMWadDatapoint.IntArrayData -> it.value
                else -> default
            }
        } ?: default
    }

    override fun deleteKey(key: String) {
        key.verifyKey()
        values.remove(key)
    }

    override fun provideChildWritableNamespacedWad(namespace: String): SMTaskWritableDataWad {
        return provideChildNamespacedWad(namespace)
    }

    override fun getPopulatedWritableChildNamespaces(): List<Pair<String, SMTaskWritableDataWad>> {
        nsLock.readLock().lock()
        val subs = childNamespaces.mapNotNull { t -> if (t.value.valueSize() > 0 || t.value.childNsSize() > 0) {
            Pair(t.key,t.value as SMTaskWritableDataWad)
        } else {
            null
        }}
        nsLock.readLock().unlock()
        return subs
    }

    override fun getValues(): Sequence<Pair<String, SMWadDatapoint>> {
        return values.asSequence().map { Pair(it.key,it.value) }
    }

    override fun provideChildReadableNamespacedWad(namespace: String): SMTaskReadableDataWad {
        return provideChildNamespacedWad(namespace)
    }

    override fun getPopulatedReadableChildNamespaces(): List<Pair<String, SMTaskReadableDataWad>> {
        nsLock.readLock().lock()
        val subs = childNamespaces.mapNotNull { t -> if (t.value.valueSize() > 0 || t.value.childNsSize() > 0) {
            Pair(t.key,t.value as SMTaskReadableDataWad)
        } else {
            null
        }}
        nsLock.readLock().unlock()
        return subs
    }

    override fun provideChildNamespacedWad(namespace: String): SMTaskDataWad {
        nsLock.writeLock().lock()
        var ns = childNamespaces[namespace]
        if (ns == null) {
            ns = SMMapTaskWad()
            childNamespaces[namespace] = ns
        }
        nsLock.writeLock().unlock()
        return ns
    }

    override fun getPopulatedChildNamespaces(): List<Pair<String,SMTaskDataWad>> {
        nsLock.readLock().lock()
        val subs = childNamespaces.mapNotNull { t -> if (t.value.valueSize() > 0 || t.value.childNsSize() > 0) {
            Pair(t.key,t.value)
        } else {
            null
        }}
        nsLock.readLock().unlock()
        return subs
    }

//    override fun deleteChildNamespace(namespace: String) {
//        nsLock.writeLock().lock()
//        childNamespaces.remove(namespace)
//        nsLock.writeLock().unlock()
//    }

}


sealed class SMWadDatapoint{
    data class StringData(val value: String) : SMWadDatapoint()
    data class StringArrayData(val value: Array<String>) : SMWadDatapoint() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StringArrayData

            if (!value.contentEquals(other.value)) return false

            return true
        }

        override fun hashCode(): kotlin.Int {
            return value.contentHashCode()
        }
    }

    data class IntData(val value: Int) : SMWadDatapoint()
    data class IntArrayData(val value: Array<Int>) : SMWadDatapoint() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as IntArrayData

            if (!value.contentEquals(other.value)) return false

            return true
        }

        override fun hashCode(): kotlin.Int {
            return value.contentHashCode()
        }
    }
}
