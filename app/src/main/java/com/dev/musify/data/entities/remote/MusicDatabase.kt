package com.dev.musify.data.entities.remote

import com.dev.musify10.other.Constants.SONG_COLLECTION
import com.dev.musify.data.entities.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Here we get all songs from firebase. And we want its access in firebase music source via injecting it through DI.
 * So need to annotate in DI->ServiceModule
 */
class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)

    suspend fun getAllSongs(): List<Song>{
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch(e: Exception) {
            emptyList()
        }
    }
}