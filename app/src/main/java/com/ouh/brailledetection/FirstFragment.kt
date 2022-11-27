package com.ouh.brailledetection

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ouh.brailledetection.databinding.FragmentFirstBinding
import com.ouh.brailledetection.server.BrailleAPI
import com.ouh.brailledetection.server.RetrofitClient
import okio.IOException
import retrofit2.Retrofit
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class FirstFragment : Fragment() {
    lateinit var retrofit: Retrofit
    lateinit var brailleAPI: BrailleAPI
    private var tts: TextToSpeech? = null
    private val cameraViewModel by viewModels<CameraViewModel>()
    private val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    private val STORAGE_PERMISSION = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var imageFilePath: String
    private lateinit var photoUri: Uri

    private val REQUEST_IMAGE_CAPTURE = 300

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retrofit = RetrofitClient.getInstance()
        brailleAPI = retrofit.create(BrailleAPI::class.java)

        cameraViewModel.brailleImage.observe(viewLifecycleOwner) {
            binding.image.setImageBitmap(it)
        }

        binding.cameraButton.setOnClickListener {
            sendTakePhotoIntent()
        }

        cameraViewModel.brailleData.observe(viewLifecycleOwner) {
            binding.inferText.text = it.toString()
            setTTs()
        }

        binding.inferText.setOnClickListener {
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            tts?.speak(cameraViewModel.brailleData.value, TextToSpeech.QUEUE_FLUSH, null, null)
        }

        cameraViewModel.responseUrl.observe(viewLifecycleOwner) {
            cameraViewModel.getImageFromServer(it)
        }
    }


    private fun setTTs() {
        tts = TextToSpeech(requireContext(), object : TextToSpeech.OnInitListener {
            override fun onInit(status: Int) {
                if (status != ERROR) {
                    val result = tts?.setLanguage(Locale.KOREA)
                    if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
                        Log.e("TTS", "this language is not supported!!")
                    }
                }
            }
        })
    }

    private fun checkPermission(permissions: Array<out String>, flag: Int): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(requireActivity(), permissions, flag)
                return false
            }
        }
        return true
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        https://raon-studio.tistory.com/6
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    var bitmap: Bitmap = BitmapFactory.decodeFile(imageFilePath)
                    bitmap = rotate(bitmap, 90f)
                    cameraViewModel.setImage(bitmap = bitmap)
                    try {
                        cameraViewModel.sendAddRequest()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "test_${timeStamp}"
            val storageDir: File? =
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val image: File = File.createTempFile(
                imageFileName, ".jpeg", storageDir
            )
            imageFilePath = image.absolutePath
            image
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun sendTakePhotoIntent() {
        if (checkPermission(CAMERA_PERMISSION, REQUEST_IMAGE_CAPTURE)) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (photoFile != null) {
                    photoUri =
                        FileProvider.getUriForFile(
                            requireActivity(),
                            requireActivity().packageName,
                            photoFile
                        )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } else {
                Log.d("", "")
            }
        }
    }

    private fun rotate(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroyView() {
        if (tts != null) {
            tts?.apply {
                stop()
                shutdown()
                tts = null
            }
        }
        super.onDestroyView()
        _binding = null
    }
}