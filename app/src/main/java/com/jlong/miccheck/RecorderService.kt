package com.jlong.miccheck

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
import android.widget.Toast
import java.io.IOException

enum class RecorderActions {
    START, PAUSE, RESUME, STOP
}

enum class RecordingState {
    RECORDING, PAUSED, STOPPED, WAITING, ELAPSED_TIME
}

class RecorderService : Service() {

    lateinit var mServiceMessenger: Messenger
    var mActivityMessenger: Messenger? = null

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: ParcelFileDescriptor? = null

    private val notificationChannelId = "micCheckRecordingControls"
    private val notificationId = 102
    private var notification: Notification? = null

    var recordTimeHandler: Handler? = null
    var recordTime: Long = 0

    var sampleRate: Int = 0
    var encodingBitRate: Int = 0

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.obj as RecorderActions) {
                RecorderActions.START -> onStartRecord(msg.data)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RecorderService", "onStartCommand")
        mActivityMessenger = intent?.getParcelableExtra("Messenger")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        mServiceMessenger = Messenger(IncomingHandler())
    }

    fun onStartRecord(params: Bundle) {
        val uri = createRecordingFile()
        val failToast: () -> Unit = {
            Toast.makeText(
                applicationContext,
                "Could not start recording - are you on a call?",
                Toast.LENGTH_LONG
            ).show()
        }

        if (uri == null) {
            failToast()
            return
        }
        sampleRate = params.getInt("sampleRate")
        encodingBitRate = params.getInt("encodingBitRate")
        if (recorder == null)
            if (!prepareRecorder()) {
                failToast()
                return
            } else {
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
        postElapsed()

        moveToForeground()

        Log.i("RecorderService", "Recording started.")
    }

    private fun postElapsed() {
        if (recordTimeHandler == null) {
            recordTimeHandler = Handler(Looper.getMainLooper())
        }
        recordTimeHandler?.postDelayed({
            recordTime += 1000
            val lMsg = Message().apply {
                obj = RecordingState.ELAPSED_TIME
                data = Bundle().apply {
                    putLong("ELAPSED", recordTime)
                }
            }
            mActivityMessenger?.apply {
                send(lMsg)
            }
            postElapsed()
        }, 1000)
    }

    private fun stopPostElapsed() {
        recordTimeHandler?.removeCallbacksAndMessages(null)
        recordTimeHandler = null
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
        stopPostElapsed()
        Log.i("RecorderService", "Recording paused.")
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
        postElapsed()
        Log.i("RecorderService", "Recording resumed.")

    }

    fun onStopRecord() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        currentOutputFile?.close()
        currentOutputFile = null

        stopForeground(true)

        val lMsg = Message().apply {
            obj = RecordingState.STOPPED
        }
        mActivityMessenger?.apply {
            send(lMsg)
        }
        stopPostElapsed()
        recordTime = 0
        Log.i("RecorderService", "Recording stopped.")

    }

    private fun prepareRecorder(): Boolean {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(encodingBitRate)
            setAudioSamplingRate(sampleRate)
            setOutputFile(currentOutputFile!!.fileDescriptor)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("MicCheck", "prepare() failed")
                return@prepareRecorder false
            }
        }
        return true
    }

    private fun createRecordingFile(): Uri? {
        val values = ContentValues(4)
        values.put(MediaStore.Audio.Media.TITLE, "Untitled Recording")
        values.put(
            MediaStore.Audio.Media.DATE_ADDED,
            (System.currentTimeMillis() / 1000).toInt()
        )
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/micCheck/")
        }

        val audioUri = applicationContext.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return null

        currentOutputFile = applicationContext.contentResolver.openFileDescriptor(audioUri, "w")

        return audioUri
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(
            this,
            MainActivity::class.java
        )
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this@RecorderService, 0, openActivityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun moveToForeground() {
        val pendingIntent = getNotificationIntent()

        notification = Notification.Builder(this, notificationChannelId)
            .setContentTitle("MicCheck Recording")
            .setContentText("Currently Recording")
            .setSmallIcon(R.drawable.ic_notification_temp)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) startForeground(
            notificationId,
            notification!!, FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
        else
            startForeground(notificationId, notification)
    }
}