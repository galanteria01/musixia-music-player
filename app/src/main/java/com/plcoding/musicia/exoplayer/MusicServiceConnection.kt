package com.plcoding.musicia.exoplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.plcoding.musicia.other.Constants
import com.plcoding.musicia.other.Event
import com.plcoding.musicia.other.Resource

class MusicServiceConnection (
    context: Context
){
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected

    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError

    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    private val _currentPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val currentPlayingSong: LiveData<MediaMetadataCompat?> = _currentPlayingSong

    lateinit var mediaController: MediaControllerCompat

    private var mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java,
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply {
        connect()
    }

    fun subscribe(parentId:String, callback:MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId:String, callback:MediaBrowserCompat.SubscriptionCallback){
    mediaBrowser.unsubscribe(parentId, callback)
}

    val transportControls: MediaControllerCompat.TransportControls
            get() = mediaController.transportControls

    private inner class MediaBrowserConnectionCallback(
            private val context: Context
    ): MediaBrowserCompat.ConnectionCallback(){
            override fun onConnected() {
                    mediaController = MediaControllerCompat(context,mediaBrowser.sessionToken).apply {
                        registerCallback(MediaControllerCallback())
                    }
                    _isConnected.postValue(Event(Resource.success(true)))
            }

            override fun onConnectionSuspended() {
                    _isConnected.postValue(Event(Resource.error(
                            "The connection has been suspended",
                            false
                    )))
            }

            override fun onConnectionFailed() {
                    _isConnected.postValue(Event(Resource.error(
                            "Couldn't connect to media browser",
                            false
                    )))
            }
    }

    private inner class MediaControllerCallback: MediaControllerCompat.Callback(){
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                _currentPlayingSong.postValue(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when(event){
                Constants.NETWORK_ERROR -> _networkError.postValue(
                        Event(
                                Resource.error(
                                        "Couldn't connect to server",
                                        null
                                )
                        )
                )
            }
        }

        override fun onSessionDestroyed() {
                mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}