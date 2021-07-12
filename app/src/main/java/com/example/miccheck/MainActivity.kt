package com.example.miccheck

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.miccheck.ui.theme.MicCheckTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import android.provider.MediaStore

import android.content.ContentValues
import android.os.ParcelFileDescriptor
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    val mainActivityVM by viewModels<AppViewModel>()
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
            requestPermissions(permissions, 0)
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                withContext(Dispatchers.IO)
                {
                    mainActivityVM.loadRecordings(applicationContext)
                }
            }

            MicCheckTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    LazyColumn() {
                        itemsIndexed(mainActivityVM.recordings)
                        {
                            _, item ->
                            Text(item.name)
                            Text(item.uri.toString())
                        }

                        item {
                            if (mainActivityVM.currentlyRecording)
                                Button(onClick = {
                                    coroutineScope.launch {
                                        onStopRecord()
                                    }
                                    mainActivityVM.currentlyRecording = false
                                }) {
                                    Text("Stop?")
                                }
                            else
                                Button(onClick = {
                                    onStartRecord()
                                    mainActivityVM.currentlyRecording = true
                                }) {
                                    Text("Record?")
                                }
                        }
                    }
                }
            }
        }
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

        val audioUri = applicationContext.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        currentOutputFile = applicationContext.contentResolver.openFileDescriptor(audioUri!!, "w");

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(16*44100);
            setAudioSamplingRate(44100);
            setOutputFile(currentOutputFile!!.fileDescriptor)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("MicCheck", "prepare() failed")
            }

            start()
        }

    }

    private suspend fun onStopRecord() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        withContext(Dispatchers.IO) { currentOutputFile!!.close() }
        currentOutputFile = null

        mainActivityVM.loadRecordings(applicationContext)
    }

}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MicCheckTheme {
        Greeting("Android")
    }
}