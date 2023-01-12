package com.amd.simplevideocapturetest

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaScannerConnection
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.amd.simplevideocapturetest.databinding.ActivityMainBinding
import com.otaliastudios.cameraview.*
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Preview
import com.otaliastudios.cameraview.filter.Filters
import com.otaliastudios.cameraview.filters.TintFilter
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.vmadalin.easypermissions.EasyPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by viewModels()

    private val logger = CameraLogger.create("VideoTest")
    private val USE_FRAME_PROCESSOR = true
    private val DECODE_BITMAP = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        reqPermission()
        setupObserver()
    }

    private fun setupObserver() = with(mainViewModel) {
        isGranted.onEach {
            setupCamera()
            onClick()
//            binding.camera.open()
        }.launchIn(lifecycleScope)
        tintFilter.onEach {
            if (it > 0) {
                val tint = TintFilter()
                tint.tint = this@MainActivity.getColorCompact(it)
                binding.camera.filter = tint
                return@onEach
            }
            val filter = Filters.values()[it]
            binding.camera.filter = filter.newInstance()
        }.launchIn(lifecycleScope)
    }

    private fun reqPermission() {
        if (EasyPermissions.hasPermissions(this, *mainViewModel.cameraPermissions)) {
            mainViewModel.updateStatusPermission(true)
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Ijinkan aplikasi untuk mengakses kamera agar fitur berjalan dengan lancar",
                1001,
                perms = mainViewModel.cameraPermissions
            )
        }
    }

    private fun onClick() =
        with(binding) {
            btnCapture.setOnClickListener { captureVideoSnapshot() }
            btnSwitchCamera.setOnClickListener { switchCamera() }
            tintBlue.setOnClickListener { mainViewModel.updateTintFilter(R.color.blue) }
            tintGreen.setOnClickListener { mainViewModel.updateTintFilter(R.color.green) }
            tintRed.setOnClickListener { mainViewModel.updateTintFilter(R.color.red) }
        }

    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    private fun switchCamera() {
        if (binding.camera.isTakingVideo) return
        when (binding.camera.facing) {
            Facing.FRONT -> {
                binding.camera.facing = Facing.BACK
            }

            Facing.BACK -> {
                binding.camera.facing = Facing.FRONT
            }
        }
    }

    private fun captureVideoSnapshot() = with(binding) {
        if (camera.isTakingVideo) return run {
            logging("Already taking video.")
        }
        if (camera.preview != Preview.GL_SURFACE) return run {
            logging("Video snapshots are only allowed with the GL_SURFACE preview.")
        }
        logging("Recording snapshot for 5 seconds...")
        camera.takeVideoSnapshot(File(filesDir, "${UUID.randomUUID()}.mp4"), 5000)
    }

    private fun setupCamera() = lifecycleScope.launchWhenStarted {
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE)
        binding.camera.apply {
            setLifecycleOwner(this@MainActivity)
            addCameraListener(Listener())
            if (USE_FRAME_PROCESSOR) {
                addFrameProcessor(object : FrameProcessor {
                    private var lastTime = System.currentTimeMillis()
                    override fun process(frame: Frame) {
                        val newTime = frame.time
                        val delay = newTime - lastTime
                        lastTime = newTime
                        logger.v(
                            "Frame delayMillis:",
                            delay,
                            "FPS:",
                            1000 / delay
                        )
                        if (DECODE_BITMAP) {
                            if (frame.format == ImageFormat.NV21
                                && frame.dataClass == ByteArray::class.java
                            ) {
                                val data = frame.getData<ByteArray>()
                                val yuvImage = YuvImage(
                                    data,
                                    frame.format,
                                    frame.size.width,
                                    frame.size.height,
                                    null
                                )
                                val jpegStream = ByteArrayOutputStream()
                                yuvImage.compressToJpeg(
                                    Rect(
                                        0, 0,
                                        frame.size.width,
                                        frame.size.height
                                    ), 100, jpegStream
                                )
                                val jpegByteArray = jpegStream.toByteArray()
                                val bitmap = BitmapFactory.decodeByteArray(
                                    jpegByteArray,
                                    0, jpegByteArray.size
                                )
                                bitmap.toString()
                            }
                        }
                    }
                })
            }
        }
    }

    inner class Listener : CameraListener() {
        override fun onVideoRecordingStart() {
            super.onVideoRecordingStart()
            showToast("Mulai merekam video")
            binding.layoutTint.isVisible = false
            binding.btnSwitchCamera.isVisible = false
            binding.btnCapture.isVisible = false
        }

        override fun onVideoRecordingEnd() {
            super.onVideoRecordingEnd()
            showToast("Video berhasil direkam")
            binding.layoutTint.isVisible = true
            binding.btnSwitchCamera.isVisible = true
            binding.btnCapture.isVisible = true
            mainViewModel.clearTintFilter()
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            scanner(result.file.absolutePath)
            try {
                val newFile = commonDocumentDirPath("VideoPath")
                logging("newpath"+newFile?.absolutePath)
                newFile?.let { file ->
                    copyOrMoveFile(result.file, file, false)
                }
            } catch (e: Exception) {
                logging("errornih:" + e.message.toString())
            }
        }

        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
            logger.v("Camera Open")
            binding.camera.open()
            showSnackBar(binding.root, exception.message.toString())
            exception.printStackTrace()
        }
    }

    private fun scanner(path: String) {
        MediaScannerConnection.scanFile(
            this@MainActivity, arrayOf(path), null
        ) { path, uri -> logging("VIDEOPATH", "Finished scanning $path") }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        finish()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        mainViewModel.updateStatusPermission(true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}