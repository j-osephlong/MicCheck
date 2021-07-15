package com.example.miccheck

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.example.miccheck.ui.theme.MicCheckTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val mainActivityVM by viewModels<AppViewModel>()
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: ParcelFileDescriptor? = null

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
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
            requestPermissions(permissions, 0)
        }

        val componentName = ComponentName(this, AudioService::class.java)
        // initialize the browser
        mMediaBrowserCompat = MediaBrowserCompat(
            this, componentName, //Identifier for the service
            connectionCallback,
            null
        )

        setContent {
            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                withContext(Dispatchers.IO)
                {
                    mainActivityVM.loadRecordings(applicationContext)
                }
            }

            MicCheckTheme {
                this.window.statusBarColor = Color(0xfffbe9e7).toArgb()
                @Suppress("DEPRECATION")
                if (MaterialTheme.colors.surface.luminance() > 0.5f) {
                    window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                // A surface container using the 'background' color from the theme
                Surface(color = Color(0xfffbe9e7)) {
                    MainScreen(
                        recordings = mainActivityVM.recordings,
                        recordingsData = mainActivityVM.recordingsData,
                        recordingState = mainActivityVM.recordingState,
                        currentPlaybackRec = mainActivityVM.currentPlayBackRec,
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
                            mainActivityVM.setCurrentPlayback(it)
                            mediaController.transportControls.playFromUri(it.uri, null)
                        },
                        onStopPlayback = {
                            mainActivityVM.setCurrentPlayback(null)
                            mediaController.transportControls.pause()
                        },
                        onAddRecordingTag = mainActivityVM::addTagToRecording,
                        onSelectScreen = mainActivityVM::setScreen,
                        selectedScreen = mainActivityVM.selectedScreen
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // connect the controllers again to the session
        // without this connect() you won't be able to start the service neither control it with the controller
        mMediaBrowserCompat.connect()
    }

    override fun onStop() {
        super.onStop()
        // Release the resources
        val controllerCompat = MediaControllerCompat.getMediaController(this)
        controllerCompat?.unregisterCallback(mControllerCallback)
        mMediaBrowserCompat.disconnect()
    }

    private fun onStartRecord() {
        val values = ContentValues(4)
        values.put(MediaStore.Audio.Media.TITLE, Random.nextLong())
        values.put(
            MediaStore.Audio.Media.DATE_ADDED,
            (System.currentTimeMillis() / 1000).toInt()
        )
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/micCheck/")

        val audioUri = applicationContext.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            values
        )

        currentOutputFile = applicationContext.contentResolver.openFileDescriptor(audioUri!!, "w")
        mainActivityVM.currentRecordingUri = audioUri
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(16 * 44100)
            setAudioSamplingRate(44100)
            setOutputFile(currentOutputFile!!.fileDescriptor)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("MicCheck", "prepare() failed")
            }

            start()
        }
        mainActivityVM.recordingState = RecordingState.RECORDING
    }

    private fun onPausePlayRecord() {
        recorder?.apply {
            if (mainActivityVM.recordingState == RecordingState.RECORDING)
                pause()
            else if (mainActivityVM.recordingState == RecordingState.PAUSED)
                resume()
        }
        if (mainActivityVM.recordingState != RecordingState.WAITING)
            mainActivityVM.recordingState =
                if (mainActivityVM.recordingState == RecordingState.PAUSED) RecordingState.RECORDING
                else RecordingState.PAUSED
    }

    private suspend fun onStopRecord() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        withContext(Dispatchers.IO) {
            currentOutputFile!!.close()
        }
        currentOutputFile = null

        mainActivityVM.recordingState = RecordingState.STOPPED
    }


    private val mControllerCallback = object : MediaControllerCompat.Callback() {
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
                }
//            playPauseBuild()
                Log.d("onConnected", "Controller Connected")
            }

            override fun onConnectionFailed() {
                super.onConnectionFailed()
                Log.d("onConnectionFailed", "Connection Failed")

            }

        }

}