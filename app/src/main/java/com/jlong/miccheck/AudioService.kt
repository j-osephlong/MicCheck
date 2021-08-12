package com.jlong.miccheck

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class AudioService : MediaBrowserServiceCompat() {

    private var mMediaSession: MediaSessionCompat? = null
    private lateinit var mStateBuilder: PlaybackStateCompat.Builder
    private var mExoPlayer: SimpleExoPlayer? = null
    private var oldUri: Uri? = null
    private var currUri: Uri? = null
    private var mediaExtras: Bundle? = null

    private var notificationId = 101
    private var channelId = "micCheckAudioServiceControls"
    private var notificationBuilder: Notification.Builder? = null

    private val emptyRootMediaId = "micCheck_empty_root_media_id"

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
        private var handler: Handler? = null

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)

            uri?.let {
                val mediaSource = extractMediaSourceFromUri(uri)
                currUri = uri
                mediaExtras = extras
                setMetadataFromExtras()
                if (uri != oldUri || mMediaSession!!.controller.playbackState.state == PlaybackStateCompat.STATE_STOPPED)
                    play(mediaSource)
                else play() // this song was paused so we don't need to reload it
                oldUri = uri
            }
            displayNotification()
            updateCurrentPosition()
        }

        override fun onPlay() {
            super.onPlay()
            currUri?.let {
                onPlayFromUri(currUri, mediaExtras)
                displayNotification()
                updateCurrentPosition()
            }
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            seek(pos)
            updateCurrentPosition()
        }

        override fun onPause() {
            super.onPause()
            pause()
            stopPlaybackStateUpdate()
            displayNotification()
            stopForeground(false)
        }

        override fun onRewind() {
            super.onRewind()

            val duration =
                mMediaSession?.controller?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    ?: 0L
            val diff = if (duration < (1000 * 60 * 1.5))
                -5
            else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                -10
            else
                -30

            seek(
                ((mExoPlayer?.currentPosition ?: 0L) + diff * 1000)
            )

            updateCurrentPosition()
        }

        override fun onFastForward() {
            super.onFastForward()
            val duration =
                mMediaSession?.controller?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    ?: 0L
            val diff = if (duration < (1000 * 60 * 1.5))
                5
            else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                10
            else
                30

            seek(
                ((mExoPlayer?.currentPosition ?: 0L) + diff * 1000)
            )

            updateCurrentPosition()
        }

        override fun onStop() {
            super.onStop()
            stop()
            stopPlaybackStateUpdate()
            stopForeground(true)
        }


        private fun updateCurrentPosition() {
            if (mExoPlayer == null) {
                return
            }
            if (handler == null) {
                handler = Handler(Looper.getMainLooper())
            }
            handler?.postDelayed({
                updatePlaybackState(null)
                updateCurrentPosition()
            }, 250)
        }

        private fun stopPlaybackStateUpdate() {
            handler?.removeCallbacksAndMessages(null)
            handler = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeExtractor()
        initializeAttributes()
        mMediaSession = MediaSessionCompat(baseContext, "AudioService").apply {

            // Set initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            mStateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_REWIND or PlaybackStateCompat.ACTION_FAST_FORWARD
                )
//                .addCustomAction(PlaybackStateCompat.CustomAction.Builder(
//                    CUSTOM_ACTION_REPLAY,
//                    "Replay",
//                    R.drawable.ic_baseline_replay_24
//                ).build())
//                .setState(PlaybackStateCompat.STATE_NONE, mExoPlayer!!.currentPosition, 1f)
            setPlaybackState(mStateBuilder.build())

            // methods that handle callbacks from a media controller
            setCallback(mMediaSessionCallback)

            // Set the session's token so that client activities can communicate with it
            setSessionToken(sessionToken)
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mMediaSession, intent)
            Log.e(
                "AudioService",
                "onStartCommand(): received intent " + intent.action + " with flags " + flags + " and startId " + startId
            )
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

    private fun setMetadataFromExtras() {
        mediaExtras?.let { mediaExtras ->
            mMediaSession?.let { mMediaSession ->
                mMediaSession.setMetadata(
                    MediaMetadataCompat.Builder().apply {
                        putString(
                            MediaMetadataCompat.METADATA_KEY_TITLE,
                            mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_ALBUM,
                            mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_ARTIST,
                            mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                        )
                        putLong(
                            MediaMetadataCompat.METADATA_KEY_DURATION,
                            mediaExtras.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                            currUri.toString()
                        )
                    }.build()
                )
            }
        }
    }

    private fun play(mediaSource: MediaSource) {
        if (mExoPlayer == null) initializePlayer()
        mExoPlayer?.apply {
            // AudioAttributes here from exoplayer package !!!
            mAttrs?.let { initializeAttributes() }
            // In 2.9.X you don't need to manually handle audio focus :D
            setAudioAttributes(mAttrs!!, true)
            setMediaSource(mediaSource)
            prepare()
            play()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    private fun play() {
        mExoPlayer?.apply {
            mExoPlayer?.playWhenReady = true
            mMediaSession?.isActive = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

    }

    private fun pause() {
        mExoPlayer?.apply {
            playWhenReady = false
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }
    }

    private fun seek(pos: Long) {
        Log.i("AudioService", "Seek $pos")
        mExoPlayer?.apply {
            seekTo(pos)
        }
    }

    private fun stop() {
        // release the resources when the service is destroyed
        mExoPlayer?.apply {
            playWhenReady = false
            release()
        }

        mExoPlayer = null
        mMediaSession?.isActive = false
        mMediaSession?.release()

        Log.i("AudioService", "Stopping playback.")
    }

    private fun updatePlaybackState(state: Int?) {
        // You need to change the state because the action taken in the controller depends on the state !!!
        mMediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO or
                            PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND
                )
                .setState(
                    state
                        ?: mMediaSession!!.controller.playbackState.state // this state is handled in the media controller
                    , mExoPlayer?.currentPosition ?: 0L, 1.0f // Speed playing
                ).build()
        )
    }

    private var mAttrs: AudioAttributes? = null

    private fun initializePlayer() {
        mExoPlayer = SimpleExoPlayer.Builder(
            this
        ).build()

        mExoPlayer!!.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    when (mExoPlayer!!.playbackState) {
                        Player.STATE_IDLE -> updatePlaybackState(PlaybackStateCompat.STATE_NONE)
                        Player.STATE_ENDED -> {
                            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                            mExoPlayer!!.stop()
                            displayNotification()
                        }
                        else ->
                            if (isPlaying)
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            else
                                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    }
                }
            }
        )
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(emptyRootMediaId, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == emptyRootMediaId) {
            result.sendResult(null)
        }

    }

    private fun initializeAttributes() {
        mAttrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
    }

    private lateinit var mExtractorFactory: ProgressiveMediaSource.Factory

    private fun initializeExtractor() {
        val userAgent = Util.getUserAgent(baseContext, "MicCheck")
        mExtractorFactory = ProgressiveMediaSource.Factory(
            DefaultDataSourceFactory(this, userAgent),
            DefaultExtractorsFactory()
        )
    }

    private fun extractMediaSourceFromUri(uri: Uri): MediaSource {

        return mExtractorFactory.createMediaSource(MediaItem.fromUri(uri))
    }

    private fun getPausePlayActions():
            Pair<Notification.Action, Notification.Action> {
        val pauseAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_pause), "Pause",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PAUSE
            )
        ).build()

        val playAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_play_arrow), "Play",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PLAY
            )
        ).build()

        return Pair(pauseAction, playAction)
    }

    private fun getReplayForwardActions():
            Pair<Notification.Action, Notification.Action> {
        val duration =
            mMediaSession?.controller?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                ?: 0L
        val replay = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                if (duration < (1000 * 60 * 1.5))
                    R.drawable.ic_baseline_replay_5_24
                else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                    R.drawable.ic_baseline_replay_10_24
                else
                    R.drawable.ic_baseline_replay_30_24
            ), "Skip Backward",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_REWIND
            )
        ).build()

        val forward = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                if (duration < (1000 * 60 * 1.5))
                    R.drawable.ic_baseline_forward_5_24
                else if (duration > (1000 * 60 * 1.5) && duration < (1000 * 60 * 3))
                    R.drawable.ic_baseline_forward_10_24
                else
                    R.drawable.ic_baseline_forward_30_24
            ), "Skip Forward",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_FAST_FORWARD
            )
        ).build()

        return Pair(replay, forward)
    }

    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(
            this,
            MainActivity::class.java
        )
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this@AudioService, 0, openActivityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun initializeNotification(
        mediaDescription: MediaDescriptionCompat,
        bitmap: Bitmap?
    ) {

        val notificationIntent = getNotificationIntent()
        // 3
        val (pauseAction, playAction) = getPausePlayActions()
        val (replayAction, forwardAction) = getReplayForwardActions()
        // 4
        val notification = Notification.Builder(
            this@AudioService, channelId
        )

        notification
            .setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
            .setLargeIcon(bitmap)
            .setContentIntent(notificationIntent)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent
                    (this, PlaybackStateCompat.ACTION_STOP)
            )
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_notification_temp)
            .also {
                if (mMediaSession!!.controller.playbackState.state != PlaybackStateCompat.STATE_STOPPED)
                    it.addAction(replayAction)
                it.addAction(
                    when (mMediaSession!!.controller.playbackState.state) {
                        PlaybackStateCompat.STATE_PLAYING -> pauseAction
                        else -> playAction
                    }
                )
                if (mMediaSession!!.controller.playbackState.state != PlaybackStateCompat.STATE_STOPPED)
                    it.addAction(forwardAction)
            }
            .style =
            Notification.MediaStyle()
                .setMediaSession(mMediaSession!!.sessionToken.token as MediaSession.Token?)
                .also {
                    if (mMediaSession!!.controller.playbackState.state != PlaybackStateCompat.STATE_STOPPED)
                        it.setShowActionsInCompactView(0, 1, 2)
                    else
                        it.setShowActionsInCompactView(0)
                }

        notificationBuilder = notification
    }

    private fun displayNotification() {
        // 1
        if (mMediaSession == null)
            return
        if (mMediaSession!!.controller.metadata == null) {
            return
        }

        // 3
        val mediaDescription =
            mMediaSession!!.controller.metadata.description
        // 4
//        GlobalScope.launch {
//            // 5

        val bitmap =
            BitmapFactory.decodeResource(
                this@AudioService.resources,
                R.drawable.ic_notification_temp
            )
        // 7
        initializeNotification(
            mediaDescription,
            bitmap
        )
        // 8
        ContextCompat.startForegroundService(
            this@AudioService,
            Intent(
                this@AudioService,
                AudioService::class.java
            )
        )
        // 9
        startForeground(
            notificationId,
            notificationBuilder!!.build()
        )
//        }
    }

}