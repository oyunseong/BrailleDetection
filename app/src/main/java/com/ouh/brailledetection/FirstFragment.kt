package com.ouh.brailledetection

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.ouh.brailledetection.databinding.FragmentFirstBinding
import com.ouh.brailledetection.server.BrailleAPI
import com.ouh.brailledetection.server.RetrofitClient
import retrofit2.Retrofit
import java.io.InputStream
import java.util.*


class FirstFragment : Fragment() {
    lateinit var retrofit: Retrofit
    lateinit var brailleAPI: BrailleAPI
    private var tts: TextToSpeech? = null
    private val cameraViewModel by viewModels<CameraViewModel>()
    private val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    private val STORAGE_PERMISSION = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val PERMISSION_CAMERA = 1
    private val PERMISSION_STORAGE = 2
    private val REQUEST_CAMERA = 3
    private val REQUEST_STORAGE = 4

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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
            openCamera()
        }

        cameraViewModel.brailleData.observe(viewLifecycleOwner) {
            binding.inferText.text = it.toString()
            setTTs()
        }

        binding.inferText.setOnClickListener {
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            Log.d("테스트", "${cameraViewModel.brailleData.value}")
            tts?.speak(cameraViewModel.brailleData.value, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // 서버에 있는 이미지 가져오기
    private fun getImageFromServer() {
        cameraViewModel.brailleImage.observe(viewLifecycleOwner) {
            Glide.with(this)
                .load("https://cdn.pixabay.com/photo/2021/08/03/07/03/orange-6518675_960_720.jpg")
                .into(binding.image)
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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun openCamera() {
        if (checkPermission(CAMERA_PERMISSION, PERMISSION_CAMERA)) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CAMERA)
        }
    }

    private fun checkPermission(permissions: Array<out String>, flag: Int): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(requireActivity(), permissions, flag)
                return false
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val currentImageURL: Uri?
            when (requestCode) {
                REQUEST_CAMERA -> {
                    currentImageURL = data?.data
                    val ins: InputStream? = currentImageURL?.let {
                        MyApplication.applicationContext().contentResolver.openInputStream(it)
                    }
                    Log.d("++image ins", "$ins")
                    binding.inferText.text = ins.toString()

                    val bitmap: Bitmap = data?.extras?.get("data") as Bitmap

                    binding.image.setImageURI(currentImageURL)
                    cameraViewModel.setImage(bitmap = bitmap)
                    cameraViewModel.setBrailleData(bitmap.toString())

                    try {
                        cameraViewModel.sendAddRequest()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
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