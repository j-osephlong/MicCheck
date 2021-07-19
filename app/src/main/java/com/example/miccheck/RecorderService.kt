package com.example.miccheck

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import java.io.IOException
import kotlin.random.Random

enum class RecorderActions {
    START, PAUSE, RESUME, STOP
}

enum class RecordingState {
    RECORDING, PAUSED, STOPPED, WAITING
}

class RecorderService : Service() {

    lateinit var mServiceMessenger: Messenger
    var mActivityMessenger: Messenger? = null

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: ParcelFileDescriptor? = null

    private val notificationChannelId = "micCheckRecordingControls"
    private val notificationId = 102
    private var notification: Notification? = null

    @SuppressLint("HandlerLeak")
    /*TODO*/
    inner class IncomingHandler : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.obj as RecorderActions) {
                RecorderActions.START -> onStartRecord()
                RecorderActions.PAUSE -> onPause()
                RecorderActions.RESUME -> onResume()
                RecorderActions.STOP -> onStopRecord()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("RecorderService", "onBind")
        return mServiceMessenger.binder
    }

    override fun onCreate() {
        super.onCreate()
        mServiceMessenger = Messenger(IncomingHandler())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RecorderService", "onStartCommand")
        mActivityMessenger = intent?.getParcelableExtra("Messenger")
        return super.onStartCommand(intent, flags, startId)
    }

    fun onStartRecord() {
        val uri = createRecordingFile()
        if (recorder == null)
            prepareRecorder()
        else {
            recorder!!.reset()
        }
        recorder!!.start()

        val lMsg = Message().apply {
            obj = RecordingState.RECORDING
            data = Bundle().apply {
                putString("URI", uri.toString())
            }
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }

        moveToForeground()
    }

    fun onPause() {
        recorder?.apply {
            pause()
        }
        val lMsg = Message().apply {
            obj = RecordingState.PAUSED
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }
    }

    fun onResume() {
        recorder?.apply {
            resume()
        }
        val lMsg = Message().apply {
            obj = RecordingState.RECORDING
            data = Bundle().apply {
                putString("URI", "")
            }       // Tell the client we don't have a new URI
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }
    }

    fun onStopRecord() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        currentOutputFile!!.close()
        currentOutputFile = null

        stopForeground(true)

        val lMsg = Message().apply {
            obj = RecordingState.STOPPED
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }
    }

    private fun prepareRecorder() {
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
        }
    }

    private fun createRecordingFile(): Uri {
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

        return audioUri
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    private fun moveToForeground() {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        notification = Notification.Builder(this, notificationChannelId)
            .setContentTitle("MicCheck Recording")
            .setContentText("Currently Recording")
            .setSmallIcon(R.drawable.ic_notification_temp)
            .setContentIntent(pendingIntent)
            .build()

// Notification ID cannot be 0.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) startForeground(
            notificationId,
            notification!!, FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
        else
            startForeground(notificationId, notification)
    }
}