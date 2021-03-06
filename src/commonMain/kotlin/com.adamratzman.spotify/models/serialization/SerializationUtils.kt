/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2020; Original author: Adam Ratzman */
package com.adamratzman.spotify.models.serialization

import com.adamratzman.spotify.SpotifyApi
import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.http.SpotifyEndpoint
import com.adamratzman.spotify.models.AbstractPagingObject
import com.adamratzman.spotify.models.CursorBasedPagingObject
import com.adamratzman.spotify.models.NeedsApi
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.instantiatePagingObjects
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.map
import kotlinx.serialization.serializer

@Suppress("EXPERIMENTAL_API_USAGE")
internal val stableJson =
        Json.nonstrict
// Json(JsonConfiguration.Stable.copy(strictMode = false, useArrayPolymorphism = true), spotifyUriSerializersModule)

internal inline fun <reified T : Any> String.toObjectNullable(serializer: KSerializer<T>, api: SpotifyApi<*, *>?, json: Json): T? = try {
    toObject(serializer, api, json)
} catch (e: Exception) {
    null
}

internal inline fun <reified T : Any> String.toObject(serializer: KSerializer<T>, api: SpotifyApi<*, *>?, json: Json): T {
    return this.parseJson {
        val obj = json.parse(serializer, this)
        api?.let {
            if (obj is NeedsApi) obj.api = api
            if (obj is AbstractPagingObject<*>) obj.endpoint = api.tracks
            obj.instantiatePagingObjects(api)
        }
        obj
    }
}

internal inline fun <reified T> String.toList(serializer: KSerializer<List<T>>, api: SpotifyApi<*, *>?, json: Json): List<T> {
    return this.parseJson {
        json.parse(serializer, this).apply {
            if (api != null) {
                forEach { obj ->
                    if (obj is NeedsApi) obj.api = api
                    if (obj is AbstractPagingObject<*>) obj.endpoint = api.tracks
                }
            }
        }
    }
}

internal fun <T : Any> String.toPagingObject(
    tClazz: KClass<T>,
    tSerializer: KSerializer<T>,
    innerObjectName: String? = null,
    endpoint: SpotifyEndpoint,
    json: Json,
    arbitraryInnerNameAllowed: Boolean = false,
    skipInnerNameFirstIfPossible: Boolean = true
): PagingObject<T> {
    if (innerObjectName != null || (arbitraryInnerNameAllowed && !skipInnerNameFirstIfPossible)) {
        val map = this.parseJson { json.parse((String.serializer() to PagingObject.serializer(tSerializer)).map, this) }
        return (map[innerObjectName] ?: if (arbitraryInnerNameAllowed) map.keys.firstOrNull()?.let { map[it] }
                ?: error("") else error(""))
                .apply {
                    this.endpoint = endpoint
                    this.itemClazz = tClazz
                    this.items.map { obj ->
                        if (obj is NeedsApi) obj.api = endpoint.api
                        if (obj is AbstractPagingObject<*>) obj.endpoint = endpoint
                    }
                }
    }

    return try {
        val pagingObject = this.parseJson { json.parse(PagingObject.serializer(tSerializer), this) }

        pagingObject.apply {
            this.endpoint = endpoint
            this.itemClazz = tClazz
            this.items.map { obj ->
                if (obj is NeedsApi) obj.api = endpoint.api
                if (obj is AbstractPagingObject<*>) obj.endpoint = endpoint
            }
        }
    } catch (jde: SpotifyException.ParseException) {
        if (!arbitraryInnerNameAllowed && jde.message?.contains("unable to parse", true) == true) {
            toPagingObject(
                    tClazz,
                    tSerializer,
                    innerObjectName,
                    endpoint,
                    json,
                    true,
                    false
            )
        } else throw jde
    }
}

internal fun <T : Any> initPagingObject(tClazz: KClass<T>, pagingObject: AbstractPagingObject<T>, endpoint: SpotifyEndpoint) {
    pagingObject.apply {
        this.endpoint = endpoint
        this.itemClazz = tClazz
        this.items.map { obj ->
            if (obj is NeedsApi) obj.api = endpoint.api
            if (obj is AbstractPagingObject<*>) obj.endpoint = endpoint
        }
    }
}

internal inline fun <reified T : Any> String.toPagingObject(
    tSerializer: KSerializer<T>,
    innerObjectName: String? = null,
    endpoint: SpotifyEndpoint,
    json: Json,
    arbitraryInnerNameAllowed: Boolean = false,
    skipInnerNameFirstIfPossible: Boolean = true
): PagingObject<T> = toPagingObject(T::class, tSerializer, innerObjectName, endpoint, json, arbitraryInnerNameAllowed, skipInnerNameFirstIfPossible)

internal fun <T : Any> String.toCursorBasedPagingObject(
    tClazz: KClass<T>,
    tSerializer: KSerializer<T>,
    innerObjectName: String? = null,
    endpoint: SpotifyEndpoint,
    json: Json,
    arbitraryInnerNameAllowed: Boolean = false,
    skipInnerNameFirstIfPossible: Boolean = true
): CursorBasedPagingObject<T> {
    if (innerObjectName != null || (arbitraryInnerNameAllowed && !skipInnerNameFirstIfPossible)) {
        val map = this.parseJson { json.parse((String.serializer() to CursorBasedPagingObject.serializer(tSerializer)).map, this) }
        return (map[innerObjectName] ?: if (arbitraryInnerNameAllowed) map.keys.firstOrNull()?.let { map[it] }
                ?: error("") else error(""))
                .apply { initPagingObject(tClazz, this, endpoint) }
    }
    return try {
        val pagingObject = this.parseJson { json.parse(CursorBasedPagingObject.serializer(tSerializer), this) }

        initPagingObject(tClazz, pagingObject, endpoint)

        pagingObject
    } catch (jde: SpotifyException.ParseException) {
        if (!arbitraryInnerNameAllowed && jde.message?.contains("unable to parse", true) == true) {
            toCursorBasedPagingObject(
                    tClazz,
                    tSerializer,
                    innerObjectName,
                    endpoint,
                    json,
                    arbitraryInnerNameAllowed = true,
                    skipInnerNameFirstIfPossible = false
            )
        } else throw jde
    }
}

internal inline fun <reified T : Any> String.toCursorBasedPagingObject(
    tSerializer: KSerializer<T>,
    innerObjectName: String? = null,
    endpoint: SpotifyEndpoint,
    json: Json,
    arbitraryInnerNameAllowed: Boolean = false,
    skipInnerNameFirstIfPossible: Boolean = true
): CursorBasedPagingObject<T> =
        toCursorBasedPagingObject(T::class, tSerializer, innerObjectName, endpoint, json, arbitraryInnerNameAllowed, skipInnerNameFirstIfPossible)

internal inline fun <reified T> String.toInnerObject(serializer: KSerializer<T>, innerName: String, json: Json): T {
    val map = this.parseJson { json.parse((String.serializer() to serializer).map, this) }
    return (map[innerName] ?: error("Inner object with name $innerName doesn't exist in $map"))
}

internal inline fun <reified T> String.toInnerArray(serializer: KSerializer<List<T>>, innerName: String, json: Json): List<T> {
    val map = this.parseJson { json.parse((String.serializer() to serializer).map, this) }
    return (map[innerName] ?: error("Inner object with name $innerName doesn't exist in $map")).toList()
}

internal fun Map<String, JsonElement>.toJson() = JsonObject(this).toString()

internal fun <A, B> createMapSerializer(aSerializer: KSerializer<A>, bSerializer: KSerializer<B>) =
        (aSerializer to bSerializer).map

internal fun <T> String.parseJson(producer: String.() -> T): T =
        try {
            producer(this)
        } catch (e: Exception) {
            throw SpotifyException.ParseException(
                    "Unable to parse $this",
                    e
            )
        }
