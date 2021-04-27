package com.dev.musify.exoplayer

/**
In this class we will have the list of songes fetched from firebase
 Now that process may take some time, coz they are songs and in our service we don't have that possibility to wait until this
 process gets finished.

 So we create that 'state' variable and execute certain actions according to the state condition

 BASICALLY THIS CLASS ACTS LIKE A PAUSE TO SERVICE AND LOAD ALL SONGS FIRST:)
 */
class FirebaseMusicSource {

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    //Check in which state this MusicSource is present
    private var state: State = State.STATE_CREATED // Initially created
        /*
        * Before downloading songs State->INITIALIZING && after downloading State->INITIALIZED
        */
        set(value) {
            if(value == State.STATE_INITIALIZED || value == State.STATE_ERROR) {
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
        if(state == State.STATE_CREATED || state == State.STATE_INITIALIZING) {
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