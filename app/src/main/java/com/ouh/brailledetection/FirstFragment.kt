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
import androidx.navigation.fragment.findNavController
import com.ouh.brailledetection.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    val cameraViewModel by viewModels<CameraViewModel>()
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

//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }

        cameraViewModel.braille.observe(viewLifecycleOwner) {
            binding.textviewFirst.text = it.toString()
        }

        binding.cameraButton.setOnClickListener {
            openCamera()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}