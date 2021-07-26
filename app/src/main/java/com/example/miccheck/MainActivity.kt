package com.example.miccheck

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.core.content.ContextCompat
import com.example.miccheck.ui.theme.MicCheckTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File


class MainActivity : AppCompatActivity() {
    private val mainActivityVM by viewModels<AppViewModel>()

    private var mActivityMessenger: Messenger? = null
    private var mServiceMessenger: Messenger? = null
    private var recorderServiceConnection: RecorderServiceConnection? = null

    @ExperimentalFoundationApi
    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE
            )
            requestPermissions(permissions, 0)
        }

        createNotificationChannel()
        val componentName = ComponentName(this, AudioService::class.java)
        // initialize the browser
        mMediaBrowserCompat = MediaBrowserCompat(
            this, componentName, //Identifier for the service
            connectionCallback,
            null
        )

        mActivityMessenger = Messenger(RecorderHandler())
        val lIntent = Intent(this@MainActivity, RecorderService::class.java)
        lIntent.putExtra("Messenger", mActivityMessenger)
        startService(lIntent)

        mainActivityVM.serializeAndSave = this@MainActivity::serializeAndSaveData
        loadData()
        CoroutineScope(Dispatchers.IO).launch {
            mainActivityVM.loadRecordings(applicationContext)
            verifyData()
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()

            MicCheckTheme {
                val systemUiController = rememberSystemUiController()
                val statusBarColor = MaterialTheme.colors.primary
                SideEffect {
                    // Update all of the system bar colors to be transparent, and use
                    // dark icons if we're in light theme
                    systemUiController.setStatusBarColor(
                        color = statusBarColor,
                        darkIcons = true
                    )

                }
                // A surface container using the 'background' color from the theme
                Surface {
                    MainScreen(
                        recordings = mainActivityVM.recordings,
                        recordingsData = mainActivityVM.recordingsData,
                        tags = mainActivityVM.tags,
                        recordingState = mainActivityVM.recordingState,
                        currentPlaybackRec = mainActivityVM.currentPlayBackRec,
                        playbackState = mainActivityVM.currentPlaybackState,
                        playbackProgress = mediaController?.playbackState?.position?.toInt() ?: 0,
                        onStartRecord = { onStartRecord() },
                        onPausePlayRecord = { onPausePlayRecord() },
                        onStopRecord = {
                            coroutineScope.launch {
                                onStopRecord()
                            }
                        },
                        onFinishedRecording = { title, desc ->
                            mainActivityVM.onRecordingFinished(applicationContext, title, desc)
                            coroutineScope.launch {
                                mainActivityVM.loadRecordings(applicationContext)
                            }
                        },
                        onStartPlayback = {
                            val currRecData = it.let {
                                mainActivityVM.recordingsData.find { rec ->
                                    rec.recordingUri == it.uri.toString()
                                }
                            }

                            mainActivityVM.setCurrentPlayback(it)
                            mediaController.transportControls.playFromUri(it.uri,
                                Bundle().apply {
                                    putString(MediaMetadataCompat.METADATA_KEY_TITLE, it.name)
                                    putString(
                                        MediaMetadataCompat.METADATA_KEY_ALBUM,
                                        currRecData?.group?.name ?: "No Group"
                                    )
                                    putString(
                                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                                        "Me" /*TODO*/
                                    )
                                    putLong(
                                        MediaMetadataCompat.METADATA_KEY_DURATION,
                                        it.duration.toLong()
                                    )
                                })
                            mainActivityVM.setBackdrop(1)
                        },
                        onPausePlayPlayback = {

                            val currRec = mainActivityVM.currentPlayBackRec
                            val currRecData = currRec.let {
                                if (it == null)
                                    null
                                else {
                                    mainActivityVM.recordingsData.find { rec ->
                                        rec.recordingUri == it.uri.toString()
                                    }
                                }
                            }

                            if (mainActivityVM.currentPlaybackState == PlaybackStateCompat.STATE_PLAYING)
                                mediaController.transportControls.pause()
                            else if (mainActivityVM.currentPlaybackState == PlaybackStateCompat.STATE_PAUSED ||
                                mainActivityVM.currentPlaybackState == PlaybackStateCompat.STATE_STOPPED
                            )
                                mediaController.transportControls.playFromUri(mainActivityVM.currentPlayBackRec!!.uri,
                                    Bundle().apply {
                                        putString(
                                            MediaMetadataCompat.METADATA_KEY_TITLE,
                                            mainActivityVM.currentPlayBackRec!!.name
                                        )
                                        putString(
                                            MediaMetadataCompat.METADATA_KEY_ALBUM,
                                            currRecData?.group?.name ?: "No Group"
                                        )
                                        putString(
                                            MediaMetadataCompat.METADATA_KEY_ARTIST,
                                            "Me" /*TODO*/
                                        )
                                        putLong(
                                            MediaMetadataCompat.METADATA_KEY_DURATION,
                                            mainActivityVM.currentPlayBackRec!!.duration.toLong()
                                        )
                                    }
                                )
                        },
                        onSeekPlayback = {
                            mediaController.transportControls.seekTo(it)
                        },
                        onAddRecordingTag = mainActivityVM::addTagToRecording,
                        onEditFinished = { rec, title, desc ->
                            mainActivityVM.onEditRecordingDataFinished(
                                applicationContext,
                                rec,
                                title,
                                desc
                            )
                        },
                        onDeleteRecording = {
                            mainActivityVM.onDeleteRecording(applicationContext, it)
                        },
                        onSelectBackdrop = mainActivityVM::setBackdrop,
                        selectedBackdrop = mainActivityVM.selectedBackdrop
                    )
                }
            }
        }
    }

    private suspend fun serializeAndSaveData() {
        val packagedData = Json.encodeToString(
            PackagedData.serializer(), PackagedData(
                recordingsData = mainActivityVM.recordingsData,
                tags = mainActivityVM.tags
            )
        )

        val dataFile = File(applicationContext.filesDir, "MicCheckAppData.json")
        if (!dataFile.exists()) dataFile.createNewFile()
        dataFile.writeText(packagedData)
    }

    private fun loadData() {
        val dataFile = File(applicationContext.filesDir, "MicCheckAppData.json")
        if (!dataFile.exists())
            return
        val packagedData = dataFile.readText()
        val unpackagedData: PackagedData =
            try {
                Json.decodeFromString(PackagedData.serializer(), packagedData)
            } catch (e: SerializationException) {
                PackagedData(
                    listOf(),
                    listOf()
                )
            }

        mainActivityVM.tags = unpackagedData.tags.toMutableStateList()
        mainActivityVM.recordingsData = unpackagedData.recordingsData.toMutableStateList()
    }

    private suspend fun verifyData() {
        mainActivityVM.recordingsData.removeIf { recData ->
            mainActivityVM.recordings.find {
                it.uri.toString() == recData.recordingUri
            } == null
        }

        mainActivityVM.tags.removeIf { tag ->
            mainActivityVM.recordingsData.find { recData ->
                recData.tags.find { tag.name == it.name } != null
            } == null
        }

        serializeAndSaveData()
    }

    override fun onStart() {
        super.onStart()
        // connect the controllers again to the session
        // without this connect() you won't be able to start the service neither control it with the controller
        mMediaBrowserCompat.connect()
    }

    override fun onResume() {
        super.onResume()
        val lIntent = Intent(this@MainActivity, RecorderService::class.java)
        if (recorderServiceConnection == null)
            recorderServiceConnection = RecorderServiceConnection()
        bindService(
            lIntent,
            recorderServiceConnection!!,
            0
        ) // mCon is an object of MyServiceConnection Class
    }

    override fun onPause() {
        super.onPause()
        if (recorderServiceConnection != null)
            unbindService(recorderServiceConnection!!)
    }

    override fun onStop() {
        super.onStop()
        // Release the resources
        val controllerCompat = MediaControllerCompat.getMediaController(this)
        controllerCompat?.unregisterCallback(mControllerCallback)
        mMediaBrowserCompat.disconnect()
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        notificationManager.apply {
            createNotificationChannel(
                NotificationChannel(
                    "micCheckAudioServiceControls",
                    "MicCheck AudioService Controls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Playback controls"
                }
            )
            createNotificationChannel(
                NotificationChannel(
                    "micCheckRecordingControls",
                    "MicCheck Recording Controls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Recording controls"
                }
            )
        }

    }

    private val mControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            mainActivityVM.currentPlaybackState = state?.state ?: PlaybackStateCompat.STATE_NONE

            Log.i("mControllerCallback", "State change: " + state?.state)
        }
    }

    private lateinit var mMediaBrowserCompat: MediaBrowserCompat
    private val connectionCallback: MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {

                // The browser connected to the session successfully, use the token to create the controller
                super.onConnected()
                mMediaBrowserCompat.sessionToken.also { token ->
                    val mediaController = MediaControllerCompat(this@MainActivity, token)
                    MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
                    mediaController.registerCallback(
                        mControllerCallback
                    )
                    mControllerCallback.onPlaybackStateChanged(mediaController.playbackState)
                }
                Log.d("onConnected", "Controller Connected")
            }

            override fun onConnectionFailed() {
                super.onConnectionFailed()
                Log.d("onConnectionFailed", "Connection Failed")

            }

        }

    @SuppressLint("HandlerLeak")
    inner class RecorderHandler : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.obj as RecordingState) {
                RecordingState.RECORDING -> {
                    val uri = msg.data.getString("URI")
                    if ((uri ?: "").isNotBlank())
                        mainActivityVM.currentRecordingUri = Uri.parse(uri)
                    mainActivityVM.recordingState = RecordingState.RECORDING
                }
                else -> mainActivityVM.recordingState = msg.obj as RecordingState
            }
        }
    }

    inner class RecorderServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mServiceMessenger = Messenger(service)
            // where mServiceMessenger is used to send messages to Service
            // service is the binder returned from onBind method in the Service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceMessenger = null
            unbindService(this)
        }
    }

    private fun onStartRecord() {
        val msg = Message().apply {
            obj = RecorderActions.START
        }
        mServiceMessenger?.apply {
            send(msg)
        }
        mainActivityVM.recordingState = RecordingState.RECORDING
    }

    private fun onPausePlayRecord() {
        if (mainActivityVM.recordingState != RecordingState.RECORDING &&
            mainActivityVM.recordingState != RecordingState.PAUSED
        )
            return
        val msg = Message().apply {
            if (mainActivityVM.recordingState == RecordingState.RECORDING)
                obj = RecorderActions.PAUSE
            else if (mainActivityVM.recordingState == RecordingState.PAUSED)
                obj = RecorderActions.RESUME
        }
        mServiceMessenger?.send(msg)
    }

    private suspend fun onStopRecord() {
        val lMsg = Message().apply {
            obj = RecorderActions.STOP
        }
        mServiceMessenger?.apply {
            send(lMsg)
        }
    }
}