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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.ouh.brailledetection.databinding.FragmentFirstBinding
import com.ouh.brailledetection.domain.Braille
import com.ouh.brailledetection.server.BrailleAPI
import com.ouh.brailledetection.server.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Url
import java.io.*
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    lateinit var f: File
    var fos: FileOutputStream? = null
    val cameraViewModel by viewModels<CameraViewModel>()
    lateinit var retrofit: Retrofit
    lateinit var brailleAPI: BrailleAPI
    private var braille = Braille(Uri.parse(""))
    private var tts: TextToSpeech? = null

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
        f = File(requireContext().cacheDir, "image1")
        brailleAPI = retrofit.create(BrailleAPI::class.java)
        setTTs()

        cameraViewModel.brailleImage.observe(viewLifecycleOwner) {
            binding.image.setImageBitmap(it)
        }

        cameraViewModel.bos.observe(viewLifecycleOwner) {
            binding.inferText.text = it.toString()
        }
        binding.cameraButton.setOnClickListener {
            openCamera()
        }

        cameraViewModel.brailleData.observe(viewLifecycleOwner) {
            binding.inferText.text = it.toString()
        }
    }

    // 서버에 있는 이미지 가져오기
    private fun getImageFromServer(){
        cameraViewModel.brailleImage.observe(viewLifecycleOwner) {
            Glide.with(this)
                .load("https://cdn.pixabay.com/photo/2021/08/03/07/03/orange-6518675_960_720.jpg")
                .into(binding.image)
        }

    }

    private fun setTTs() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.KOREA
            }
        }
        binding.inferText.setOnClickListener {
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            tts?.speak(cameraViewModel.brailleData.value, TextToSpeech.QUEUE_FLUSH, null)
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

    private fun openCamera() {
        if (checkPermission(CAMERA_PERMISSION, PERMISSION_CAMERA)) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CAMERA)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    f.createNewFile()
                    val bitmap = data?.extras?.get("data") as Bitmap

                    try {
                        Log.d("++Camera", "setImage success")
                        val bos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, bos)
                        Log.d("++bos++", "$bos")
                        val bitmapData = bos.toByteArray()

                        try {
                            fos = FileOutputStream(f)
                            cameraViewModel.setImage(bitmap)
                            cameraViewModel.setBos(bitmapData)
                            Log.d("++fos", "FileOutputStream : ${fos}")
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }

                        try {
                            fos?.apply {
                                Log.d("++fos", "fos state1 $fos")
                                write(bitmapData)
                                Log.d("++fos", "fos state2 $fos")
                                flush()
                                Log.d("++fos", "fos state3 $fos")
                                close()
                                Log.d("++fos", "fos state4 $fos")
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
//                        postImageToServer()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "이미지를 저장할 수 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                        e.printStackTrace()
                    }
                }
                else -> {
                    Toast.makeText(requireContext(), "이미지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun requestToServer() {
        Runnable {
            brailleAPI.requestData().enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        Log.d("성공", "$response")
                        val res = response.body()
                        binding.inferText.text = res
                    } else {
                        Log.d("알수없는 오류", "$response")
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.d("++onFailure", t.toString())
                }

            })
        }.run()
    }

    private fun postImageToServer() {
        val reqFile: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), f)
        val body: MultipartBody.Part =
            MultipartBody.Part.createFormData("uploaded_file", f.name, reqFile)

        Runnable {
            brailleAPI.postImage(body)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            Toast.makeText(requireContext(), "파일 업로드 성공", Toast.LENGTH_SHORT).show()
                            Log.d("++postImageToServer 파일 업로드 성공", "body : $body, $response")
                        } else {
                            Toast.makeText(requireContext(), "파일 업로드 실패", Toast.LENGTH_SHORT).show()
                            Log.d("++postImageToServer 파일 업로드 실패", "$response")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(requireContext(), "서버를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                        Log.d("++onFailure", t.toString())
                    }
                })
        }.run()

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