package com.lukevanoort.cellarman.stuntman

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.lukevanoort.stuntman.SMMapTaskWad
import com.lukevanoort.stuntman.SMTaskDataWad
import com.lukevanoort.stuntman.SMTaskWadPersister
import com.lukevanoort.stuntman.SMWadDatapoint

private const val STR_VALUES_KEY = "|STV"
private const val STR_ARR_VALUES_KEY = "|STAV"
private const val INT_VALUES_KEY = "|INTV"
private const val INT_ARR_VALUES_KEY = "|INTAV"
private const val SUB_NAMESPACES_KEY = "|SNS"

private inline fun makeKey(namespace: String, key: String) : String {
    return "$namespace/$key"
}

private inline fun makeFullNs(currentNamespace: String, nextNamespace: String) : String {
    return "$currentNamespace/$nextNamespace"
}

private val gson: Gson = Gson()
private val stringArrayType = object : TypeToken<Array<String>>() {}.type
private val intArrayType = object : TypeToken<Array<Int>>() {}.type

private fun persistWad(
    prefs: SharedPreferences.Editor,
    namespace: String,
    wad: SMTaskDataWad
) {
    val stringData = HashSet<String>()
    val stringArrayData = HashSet<String>()
    val intData = HashSet<String>()
    val intArrayData = HashSet<String>()
    wad.getValues().forEach {
        val key = makeKey(namespace,it.first)
        val datapoint = it.second
        when(datapoint) {
            is SMWadDatapoint.StringData -> {
                prefs.putString(key,datapoint.value)
                stringData.add(it.first)
            }
            is SMWadDatapoint.StringArrayData -> {
                prefs.putString(key, gson.toJson(datapoint.value, stringArrayType))
                stringArrayData.add(it.first)
            }
            is SMWadDatapoint.IntData -> {
                prefs.putInt(key,datapoint.value)
                intData.add(it.first)
            }
            is SMWadDatapoint.IntArrayData -> {
                prefs.putString(key, gson.toJson(datapoint.value, intArrayType))
                intArrayData.add(it.first)
            }
        }.let {}
    }

    prefs.putStringSet(makeKey(namespace, STR_VALUES_KEY),stringData)
    prefs.putStringSet(makeKey(namespace, STR_ARR_VALUES_KEY),stringArrayData)
    prefs.putStringSet(makeKey(namespace, INT_VALUES_KEY),intData)
    prefs.putStringSet(makeKey(namespace, INT_ARR_VALUES_KEY),intArrayData)

    val subWads = HashSet<String>()
    wad.getPopulatedChildNamespaces().forEach {
        persistWad(prefs,makeFullNs(namespace,it.first),it.second)
        subWads.add(it.first)
    }
    prefs.putStringSet(makeKey(namespace, SUB_NAMESPACES_KEY),subWads)
}

private fun recoverWad(
    prefs: SharedPreferences,
    namespace: String,
    wad: SMTaskDataWad
) {
    prefs.getStringSet(makeKey(namespace, STR_VALUES_KEY),null)?.forEach { key ->
        prefs.getString(makeKey(namespace,key),null)?.let {value ->
            wad.store(key,value)
        }
    }

    prefs.getStringSet(makeKey(namespace, STR_ARR_VALUES_KEY),null)?.forEach { key ->
        prefs.getString(makeKey(namespace,key),null)?.let { value ->
            try {
                val strings : Array<String> = gson.fromJson(value, stringArrayType)
                wad.store(key,strings)
            } catch (e : JsonSyntaxException) {
            } catch (e : JsonParseException) {
            }
        }
    }

    prefs.getStringSet(makeKey(namespace, INT_VALUES_KEY),null)?.forEach { key ->
        prefs.getInt(makeKey(namespace,key),-1).let {value ->
            if (value == -1) {
                if (prefs.contains(makeKey(namespace,key))) {
                    wad.store(key,value)
                }
            }
        }
    }
    prefs.getStringSet(makeKey(namespace, INT_ARR_VALUES_KEY), null)?.forEach { key ->
        prefs.getString(makeKey(namespace,key),null)?.let { value ->
            try {
                val strings : Array<Int> = gson.fromJson(value, intArrayType)
                wad.store(key,strings)
            } catch (e : JsonSyntaxException) {
            } catch (e : JsonParseException) {
            }
        }
    }

    prefs.getStringSet(makeKey(namespace, SUB_NAMESPACES_KEY), null)?.forEach { sns ->
        val fullns = makeFullNs(namespace,sns)
        val nextWad = wad.provideChildNamespacedWad(sns)
        recoverWad(
            prefs,
            fullns,
            nextWad
        )
    }
}

class SharedPreferenceGsonWadPersister(
    val prefs: SharedPreferences,
    val clearAfterOperations: Boolean
) : SMTaskWadPersister {
    override fun writeWad(wad: SMTaskDataWad) {
        val editor = prefs.edit()
        if (clearAfterOperations) {
            editor.clear()
        }
        persistWad(editor,"",wad)
        editor.apply()
    }

    override fun readWad(): SMTaskDataWad {
        val wad = SMMapTaskWad()
        recoverWad(prefs,"", wad)
        if (clearAfterOperations) {
            prefs.edit().clear().apply()
        }
        return wad
    }

}