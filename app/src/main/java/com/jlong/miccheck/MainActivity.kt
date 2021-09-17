package com.jlong.miccheck

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaMetadata.METADATA_KEY_MEDIA_URI
import android.net.Uri
import android.os.*
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.android.material.datepicker.MaterialDatePicker
import com.jlong.miccheck.ui.compose.AppUI
import com.jlong.miccheck.ui.compose.FirstLaunchScreen
import com.jlong.miccheck.ui.compose.StatusBarColor
import com.jlong.miccheck.ui.theme.MicCheckTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.*

//import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    private val mainActivityVM by viewModels<AppViewModel>()

    private var mActivityMessenger: Messenger? = null
    private var mServiceMessenger: Messenger? = null
    private var recorderServiceConnection: RecorderServiceConnection? = null

    private lateinit var imageResultLauncher: ActivityResultLauncher<Intent>
    private var currentImageCallback: ((Uri) -> Unit)? = null

    private lateinit var dirResultLauncher: ActivityResultLauncher<Intent>
    private var currentDirCallback: ((Uri) -> Unit)? = null

    @ExperimentalPagerApi
    @ExperimentalFoundationApi
    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        installSplashScreen()

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        /**
         * #1
         * Setup permissions
         */
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
            var permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                permissions += Manifest.permission.FOREGROUND_SERVICE
            }

            requestPermissions(permissions, 0)
        }

        /**
         * #5
         * Recall and validate user data
         */
        mainActivityVM.serializeAndSave = this@MainActivity::serializeAndSaveData
        mainActivityVM.requestFilePermission = this@MainActivity::requestFilePermission
        loadSettings()
        loadData()
        CoroutineScope(Dispatchers.IO).launch {
            mainActivityVM.loadRecordings(applicationContext)
            verifyData()
        }

        /**
         * #2
         * Setup notification channels
         */
        createNotificationChannel()

        /**
         * #3
         * Connect to AudioService
         */
        val componentName = ComponentName(this, AudioService::class.java)
        // initialize the browser
        mMediaBrowserCompat = MediaBrowserCompat(
            this, componentName, //Identifier for the service
            connectionCallback,
            null
        )

        /**
         * #4
         * Connect to RecorderService
         */
        mActivityMessenger = Messenger(recorderClient.RecorderHandler())
        val lIntent = Intent(this@MainActivity, RecorderService::class.java)
        lIntent.putExtra("Messenger", mActivityMessenger)
        startService(lIntent)

        /**
         * #6
         * Setup image chooser, dir chooser &
         * Create compose UI with callbacks
         */
        imageResultLauncher = setupImageChooser()
        dirResultLauncher = setupDirChooser()

        setContent {
            App()
        }
    }

    //region Activity Lifecycle
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
    //endregion

    private fun requestFilePermission(intentSender: IntentSender) {
        ActivityCompat.startIntentSenderForResult(
            this,
            intentSender,
            1020,
            null,
            0,
            0,
            0,
            null
        )
    }

    //region Serialization functions
    private suspend fun serializeAndSaveData() {
        val packagedData = Json.encodeToString(
            PackagedData.serializer(), PackagedData(
                recordingsData = mainActivityVM.recordingsData.toList() as List<VersionedRecordingData>,
                tags = mainActivityVM.tags.toList() as List<VersionedTag>,
                groups = mainActivityVM.groups.toList() as List<VersionedRecordingGroup>
            )
        )
        val settings = Json.encodeToString(
            UserAndSettings.serializer(), mainActivityVM.settings
        )

        val dataFile = File(applicationContext.filesDir, "MicCheckAppData.json")
        if (!dataFile.exists()) dataFile.createNewFile()
        dataFile.writeText(packagedData)

        val settingsFile = File(applicationContext.filesDir, "MicCheckSettings.json")
        if (!settingsFile.exists()) settingsFile.createNewFile()
        settingsFile.writeText(settings)
    }

    private fun loadSettings() {
        val settingsFile = File(applicationContext.filesDir, "MicCheckSettings.json")
        if (!settingsFile.exists())
            return
        val rawData = settingsFile.readText()
        val settingsData: UserAndSettings =
            try {
                Json.decodeFromString(UserAndSettings.serializer(), rawData)
            } catch (e: SerializationException) {
                Toast.makeText(applicationContext, "Unable to read settings data.", Toast.LENGTH_LONG).show()
                return
            }
        mainActivityVM.settings = settingsData
    }

    private fun loadData() {
        val dataFile = File(applicationContext.filesDir, "MicCheckAppData.json")
        if (!dataFile.exists())
            return
        val packagedData = dataFile.readText()
        val unpackedData: PackagedData =
            try {
                Json.decodeFromString(PackagedData.serializer(), packagedData)
            } catch (e: SerializationException) {
                Toast.makeText(applicationContext, "Unable to read recording data.", Toast.LENGTH_LONG).show()
                PackagedData(
                    listOf(),
                    listOf(),
                    listOf()
                )
            }
        val currentVersionTags = unpackedData.tags.let { tags ->
            val list = mutableListOf<Tag>()
            tags.forEach {
                list.add(it.toLatestVersion())
            }
            list
        }
        val currentVersionRecordingData = unpackedData.recordingsData.let { recData ->
            val list = mutableListOf<RecordingData>()
            recData.forEach {
                list.add(it.toLatestVersion())
            }
            list
        }
        val currentVersionGroups = unpackedData.groups.let { groups ->
            val list = mutableListOf<RecordingGroup>()
            groups.forEach {
                list.add(it.toLatestVersion())
                if (it is VersionedRecordingGroup.V2)
                {
                    val groupRecs = currentVersionRecordingData.filter { recData ->
                        recData.groupUUID == it.uuid
                    }
                    groupRecs.forEach { recData ->
                        if (recData.groupOrderNumber == -1)
                            recData.groupOrderNumber = groupRecs.maxOf { i -> i.groupOrderNumber } + 1
                    }
                }
            }
            list
        }
        mainActivityVM.tags = currentVersionTags.toMutableStateList()
        mainActivityVM.recordingsData = currentVersionRecordingData.toMutableStateList()
        mainActivityVM.groups = currentVersionGroups.toMutableStateList()
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

        mainActivityVM.groups.forEach {
            mainActivityVM.orderGroup(applicationContext, it)
            it.imgUri?.let { uri ->
                val file = File(applicationContext.filesDir, Uri.parse(uri).lastPathSegment ?: return@let)
                if (!file.exists())
                {
                    val index = mainActivityVM.groups.indexOf(it)
                    mainActivityVM.groups[index].imgUri = null
                }
            }
        }

        serializeAndSaveData()
    }

    fun exportData () {
        val packagedData = Json.encodeToString(
            PackagedData.serializer(), PackagedData(
                recordingsData = mainActivityVM.recordingsData.toList() as List<VersionedRecordingData>,
                tags = mainActivityVM.tags.toList() as List<VersionedTag>,
                groups = mainActivityVM.groups.toList() as List<VersionedRecordingGroup>
            )
        )
        var dirUri: Uri? = null
        onChooseDir { dirUri = it }
        dirUri?.also {
            contentResolver.openOutputStream(it)?.apply {
                write(packagedData.toByteArray())
                close()
            }
        }
    }

    fun importData() {

    }
    //endregion

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

    private fun onShareAsAudio(recording: Recording?) {
        fun shareOne(rec: Recording) {
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, rec.uri)
                type = "audio/*"
            }
            startActivity(Intent.createChooser(shareIntent, "Share your recording."))
        }
        if (recording != null) {
            shareOne(recording)
            return
        }

        if (mainActivityVM.selectedRecordings.size < 1)
            return

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList<Uri>().also {
                    it.addAll(
                        mainActivityVM.selectedRecordings.map { it.uri })
                }
            )
            type = "audio/*"
        }
        startActivity(Intent.createChooser(shareIntent, "Share your recordings."))

    }

    private fun showDatePicker(onSelect: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker().setTheme(R.style.ThemeOverlay_App_DatePicker).build()
        this.let {
            picker.show(it.supportFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener { milli ->
                onSelect(milli)
            }
        }
    }

    //region Image chooser functions
    private fun setupImageChooser() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Uri = result.data?.data ?: return@registerForActivityResult
                val destFile = File(
                    applicationContext.filesDir.absolutePath,
                    data.lastPathSegment!! + data.scheme
                )
                    .also { it.createNewFile() }
                val outStream = FileOutputStream(destFile)
                val inStream = contentResolver.openInputStream(data)?.also {
                    it.copyTo(outStream)
                }
                inStream?.close()
                outStream.close()

                currentImageCallback?.invoke(Uri.fromFile(destFile))
                currentImageCallback = null
            }
        }

    private fun onChooseImage(callback: (Uri) -> Unit) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        currentImageCallback = callback
        imageResultLauncher.launch(intent)
    }
    //endregion

    private fun setupDirChooser() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    currentDirCallback?.invoke(uri)
                    currentDirCallback = null
                }
            }
        }

    private fun onChooseDir(callback: (Uri) -> Unit) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "micCheckExportedData.json")
        }

        currentDirCallback = callback
        dirResultLauncher.launch(intent)
    }

    private fun trim(rec: Recording, start: Long, end: Long, title: String) {
        val startInSec = start / 1000f
        val endInSec = end / 1000f
        val values = ContentValues(4)
        values.put(MediaStore.Audio.Media.TITLE, title)
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, title)
        values.put(
            MediaStore.Audio.Media.DATE_ADDED,
            (System.currentTimeMillis() / 1000).toInt()
        )
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.IS_PENDING, 1)
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/micCheck/")
        }

        val audioUri = applicationContext.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return

        val path = applicationContext.filesDir.absolutePath + "/$title.m4a"
        Log.i("TRIM", "path -> $path\nstart -> $startInSec\nend -> $endInSec")
        FFmpeg.execute("-i \"${rec.path}\" -vn -acodec copy -ss $startInSec -t $endInSec \"$path\"")

        val localFile = File(path).inputStream()
        val mediaStoreFile = applicationContext.contentResolver.openOutputStream(audioUri) ?: return
        localFile.copyTo(mediaStoreFile)

        localFile.close()
        mediaStoreFile.close()

        File(path).also {
            if (it.exists())
                it.delete()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            applicationContext.contentResolver.update(
                audioUri,
                ContentValues().also {
                    it.put(MediaStore.Audio.Media.IS_PENDING, 0)
                },
                null, null
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            mainActivityVM.loadRecordings(applicationContext)
            mainActivityVM.recordingsData.find { it.recordingUri == audioUri.toString() }?.let {
                mainActivityVM.recordingsData[
                        mainActivityVM.recordingsData.indexOf(it)
                ] = it.copy(clipParentUri = rec.uri.toString())
            }
        }


    }

    private val mControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            mainActivityVM.currentPlaybackState = state?.state ?: PlaybackStateCompat.STATE_NONE
            mainActivityVM.playbackProgress = state?.position ?: 0L
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            mainActivityVM.setCurrentPlayback(
                mainActivityVM.recordings.find {
                    it.uri.toString() == metadata?.getString(METADATA_KEY_MEDIA_URI)
                }
            )
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

    private val recorderClient =
        object {
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
                        RecordingState.ELAPSED_TIME -> {
                            val time = msg.data.getLong("ELAPSED")
                            mainActivityVM.recordTime = time
                        }
                        else -> mainActivityVM.recordingState = msg.obj as RecordingState
                    }
                }
            }

            fun onStartRecord() {
                val msg = Message().apply {
                    obj = RecorderActions.START
                    data = Bundle().apply {
                        putInt("sampleRate", mainActivityVM.settings.sampleRate)
                        putInt("encodingBitRate", mainActivityVM.settings.encodingBitRate)
                    }
                }
                mServiceMessenger?.apply {
                    send(msg)
                }
                mainActivityVM.recordingState = RecordingState.RECORDING
            }

            fun onPausePlayRecord() {
                if (mainActivityVM.recordingState != RecordingState.RECORDING &&
                    mainActivityVM.recordingState != RecordingState.PAUSED
                )
                    return
                val msg = Message().apply {
                    if (mainActivityVM.recordingState == RecordingState.RECORDING) {
                        obj = RecorderActions.PAUSE
                    } else if (mainActivityVM.recordingState == RecordingState.PAUSED) {
                        obj = RecorderActions.RESUME
                    }
                }
                mServiceMessenger?.send(msg)
            }

            suspend fun onStopRecord() {
                val lMsg = Message().apply {
                    obj = RecorderActions.STOP
                }
                mServiceMessenger?.apply {
                    send(lMsg)
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

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @ExperimentalPagerApi
    @Composable
    fun App() {
        val coroutineScope = rememberCoroutineScope()

        MicCheckTheme {
            if (mainActivityVM.settings.firstLaunch)
                FirstLaunchScreen {
                    mainActivityVM.settings = mainActivityVM.settings.copy(firstLaunch = false)
                    coroutineScope.launch { serializeAndSaveData() }
                }
            else {
                StatusBarColor()
                Surface {
                    AppUI(
                        viewModel = mainActivityVM,
                        onStartRecord = { recorderClient.onStartRecord() },
                        onPausePlayRecord = { recorderClient.onPausePlayRecord() },
                        onStopRecord = {
                            coroutineScope.launch {
                                recorderClient.onStopRecord()
                            }
                        },
                        onStartPlayback = {
                            Log.i("onStartPlayback@MainActivity", "Playing from recording.")
                            mainActivityVM.isGroupPlayback = false
                            val currGroup = it.let {
                                val recData = mainActivityVM.recordingsData.find { rec ->
                                    rec.recordingUri == it.uri.toString()
                                }
                                mainActivityVM.groups.find { group ->
                                    group.uuid == recData?.groupUUID
                                }
                            }

                            mediaController.transportControls.playFromUri(
                                it.uri,
                                Pair(it, currGroup).toMetaData()
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                mediaController.transportControls.setPlaybackSpeed(mainActivityVM.playbackSpeed)
                            }
                            mainActivityVM.onSetBackdrop(1)
                        },
                        onPausePlayPlayback = {
                            val currGroup = mainActivityVM.currentPlaybackRec.let {
                                val recData = if (it == null)
                                    null
                                else {
                                    mainActivityVM.recordingsData.find { rec ->
                                        rec.recordingUri == it.uri.toString()
                                    }
                                }
                                mainActivityVM.groups.find { group ->
                                    group.uuid == recData?.groupUUID
                                }
                            }

                            if (mainActivityVM.currentPlaybackState == PlaybackStateCompat.STATE_PLAYING)
                                mediaController.transportControls.pause()
                            else if ((mainActivityVM.currentPlaybackState == PlaybackStateCompat.STATE_PAUSED ||
                                        mainActivityVM.currentPlaybackState == PlaybackStateCompat.STATE_STOPPED) &&
                                mainActivityVM.currentPlaybackRec != null
                            )
                                mediaController.transportControls.playFromUri(
                                    mainActivityVM.currentPlaybackRec!!.uri,
                                    Pair(
                                        mainActivityVM.currentPlaybackRec!!,
                                        currGroup
                                    ).toMetaData()
                                )
                        },
                        onSeekPlayback = {
                            mediaController.transportControls.seekTo(
                                (it * (mainActivityVM.currentPlaybackRec?.duration ?: 0)).toLong()
                            )
                        },
                        onShareRecordings = { onShareAsAudio(it) },
                        onChooseImage = this::onChooseImage,
                        onStartGroupPlayback = { index, group ->
                            mainActivityVM.isGroupPlayback = true
                            val recsData =
                                mainActivityVM.recordingsData.filter { it.groupUUID == group.uuid }

                            val recs = mainActivityVM.recordings.filter { rec ->
                                recsData.find { rec.uri.toString() == it.recordingUri } != null
                            }.sortedBy { rec ->
                                recsData.find { it.recordingUri == rec.uri.toString() }!!.groupOrderNumber
                            }
                            if (recs.isNotEmpty()) {
                                val bundleList = arrayListOf<Bundle>()
                                recs.forEach { rec ->
                                    bundleList += Pair(rec, group).toMetaData().also {
                                        it.putString(
                                            MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                                            rec.uri.toString()
                                        )
                                        it.putBoolean("isOfPlaybackList", true)
                                    }
                                }
                                mediaController.transportControls.playFromUri(
                                    recs[0].uri,
                                    Bundle().apply {
                                        putParcelableArrayList("playbackList", bundleList)
                                        putInt("listIndex", index)
                                    })
                            }
                        },
                        showDatePicker = this::showDatePicker,
                        onSkipPlayback = {
                            if (it == PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                                mediaController.transportControls.skipToPrevious()
                            else if (it == PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                                mediaController.transportControls.skipToNext()
                        },
                        onTrim = this::trim,
                        onStopPlayback = {
                            mediaController.transportControls.pause()
                        },
                        onSetPlaybackSpeed = { speed ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                mediaController.transportControls.setPlaybackSpeed(speed)
                            }
                            mainActivityVM.playbackSpeed = speed
                        }
                    )
                }
            }
        }
    }
}