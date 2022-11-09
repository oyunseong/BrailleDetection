package com.ouh.brailledetection

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.ouh.brailledetection.databinding.FragmentFirstBinding
import com.ouh.brailledetection.model.Braille
import com.ouh.brailledetection.server.BrailleAPI
import com.ouh.brailledetection.server.RetrofitClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    val cameraViewModel by viewModels<CameraViewModel>()
    lateinit var retrofit: Retrofit
    lateinit var brailleAPI: BrailleAPI

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

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retrofit = RetrofitClient.getInstance()
        brailleAPI = retrofit.create(BrailleAPI::class.java)

//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }

        cameraViewModel.brailleImage.observe(viewLifecycleOwner) {
            binding.textviewFirst.text = it.toString()
        }

        binding.cameraButton.setOnClickListener {
            openCamera()
        }

        cameraViewModel.brailleData.observe(viewLifecycleOwner) {
            binding.textviewFirst.text = it.toString()
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
                    val bitmap = data?.extras?.get("data") as Bitmap
                    try {
                        Log.d("++Camera", "setImage success")
                        cameraViewModel.setImage(bitmap)
                        callServerData()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "이미지를 저장할 수 없습니다.", Toast.LENGTH_SHORT)
                            .show()
//                        e.printStackTrace()
                    }
                }
                else -> {
                    Toast.makeText(requireContext(), "이미지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun callServerData() {
        Runnable {
            brailleAPI.getBrailleData().enqueue(object : retrofit2.Callback<Braille> {
                override fun onResponse(call: Call<Braille>, response: Response<Braille>) {
                    val brailleData = response.body()
                    if (brailleData == null) {
                        Toast.makeText(requireContext(), "데이터 is null", Toast.LENGTH_SHORT).show()
                    }
                    cameraViewModel.setData(brailleData!!)
                }

                override fun onFailure(call: Call<Braille>, t: Throwable) {
                    Toast.makeText(requireContext(), "Retrofit2 Error!", Toast.LENGTH_SHORT).show()
                    Log.d("++initServer", "onFailure message : " + t.message.toString())
                }

            })
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}