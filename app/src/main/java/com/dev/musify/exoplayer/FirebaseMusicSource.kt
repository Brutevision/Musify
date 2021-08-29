package com.dev.musify.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.dev.musify.data.entities.remote.MusicDatabase
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
In this class we will have the list of songs fetched from firebase
Now that process may take some time, coz they are songs and in our service we don't have that possibility to wait until this
process gets finished.

So we create that 'state' variable and execute certain actions according to the state condition

BASICALLY THIS CLASS ACTS LIKE A PAUSE TO SERVICE AND LOAD ALL SONGS FIRST:)
 */

/**
 * Any Media has a lot of data/properties but we cannot always create parameters for the class in which the media is stored(Song class
 * in this case).
 * So MediaMetadataCompat is a very useful tool to set the various properties of that media stuff. So we can map properties of song class
 * to MediaMetadataCompat properties and that is what we do in fun fetchMediaData()
 */
class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
) {

    //This is similar to song class. Difference is it contains other meta data of the song.
    var songs = emptyList<MediaMetadataCompat>()

    //Function to fetch songs
    suspend fun fetchMediaData() = withContext(Dispatchers.IO) {
        state =
            State.STATE_INITIALIZING //Before downloading songs State->INITIALIZING && after downloading State->INITIALIZED
        val allSongs = musicDatabase.getAllSongs()
        songs = allSongs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, song.subtitle)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI, song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.subtitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.subtitle)
                .build()
        }
        state = State.STATE_INITIALIZED
    }

    /**
     * Songs need to play on after another. That is done by making each song a MediaSource and adding each to a
     * thread of MediaSources (ConcatenatingMediaSource())
     */
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asMediaItems() = songs.map { song ->
        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }.toMutableList()

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    //Check in which state this MusicSource is present
    private var state: State = State.STATE_CREATED // Initially created
        /*
        * Before downloading songs State->INITIALIZING && after downloading State->INITIALIZED
        */
        set(value) {
            if (value == State.STATE_INITIALIZED || value == State.STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == State.STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    fun whenReady(action: (Boolean) -> Unit): Boolean {
        if (state == State.STATE_CREATED || state == State.STATE_INITIALIZING) {
            onReadyListeners += action
            return false
        } else {
            action(state == State.STATE_INITIALIZED)
            return true
        }
    }
}

//Defining several states in which this musicSource can be in
enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}