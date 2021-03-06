/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2020; Original author: Adam Ratzman */
package com.adamratzman.spotify.utils

import org.w3c.files.File

actual typealias BufferedImage = File

actual typealias ConcurrentHashMap<K, V> = HashMap<K, V>

actual fun <K, V> ConcurrentHashMap<K, V>.asList(): List<Pair<K, V>> = toList()
