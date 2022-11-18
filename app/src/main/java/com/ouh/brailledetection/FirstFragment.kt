package com.ouh.brailledetection

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.ouh.brailledetection.databinding.FragmentFirstBinding
import com.ouh.brailledetection.server.BrailleDao
import com.ouh.brailledetection.server.RetrofitClient
import retrofit2.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class FirstFragment : Fragment() {
    lateinit var retrofit: Retrofit
    lateinit var brailleDao: BrailleDao
    private var tts: TextToSpeech? = null
    val cameraViewModel by viewModels<CameraViewModel>()
    lateinit var imageFilePath: String
    lateinit var photoUri: Uri

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
        brailleDao = retrofit.create(BrailleDao::class.java)
        setTTs()
        cameraViewModel.brailleImage.observe(viewLifecycleOwner) {
            binding.image.setImageBitmap(it)
        }

        cameraViewModel.bos.observe(viewLifecycleOwner) {
            binding.inferText.text = it.toString()
        }
        binding.cameraButton.setOnClickListener {
            openCamera()
//            sendTakePhotoIntent()
//            openGallery()
        }

        cameraViewModel.brailleData.observe(viewLifecycleOwner) {
            binding.inferText.text = it.toString()
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
        tts = TextToSpeech(requireContext()) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.KOREA
            }
        }
        binding.inferText.setOnClickListener {
            Log.d("inferText", "클릭")
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            tts?.speak(cameraViewModel.bos.value.toString(), TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    private fun createCacheFile(context: Context): Uri {
        val url = "tmp_${System.currentTimeMillis()}jpg"
        val tempFile = File(context.externalCacheDir, url)
        return FileProvider.getUriForFile(context, "com.ouh.brailledetection", tempFile)
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "TEST_" + timeStamp + "_"
        val storageDir: File? =
            MyApplication.applicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        imageFilePath = image.absolutePath
        return image
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
//            imageCaptureUri = createCacheFile(requireContext())
//            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageCaptureUri)
            startActivityForResult(intent, REQUEST_CAMERA)
        }
    }

    private fun sendTakePhotoIntent() {
        Log.d("++sendTakePhotoIntent", "call")
        if (checkPermission(CAMERA_PERMISSION, PERMISSION_CAMERA)) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireActivity().packageName,
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(takePictureIntent, REQUEST_CAMERA)
            }
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

//                    val img: Bitmap = BitmapFactory.decodeFile(imageFilePath)
//                    val img: Bitmap = BitmapFactory.decodeStream(ins)

//                    val changeType = Bitmap.createScaledBitmap(img, 256, 256, true)
                    val byteArrayOutputStream = ByteArrayOutputStream()
//                    changeType.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
                    val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
//                    val outStream = ByteArrayOutputStream()
//                    val res = resources
                    val imageBase64 = Base64.getEncoder().encodeToString(byteArray)
                    val decoder = Base64.getDecoder().decode(imageBase64)

                    binding.image.setImageURI(currentImageURL)
                    cameraViewModel.setBos(byteArray)
//                    cameraViewModel.setImage(bitmap = img)
                    cameraViewModel.setImage(bitmap = bitmap)
                    cameraViewModel.sendAddRequest()

                    try {
//                        cameraViewModel.sendAddRequest()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }



                    Log.d("++encoding", imageBase64)
                    Log.d("++decoding", decoder.toString())
                    Log.d("++byteArray", byteArray.toString())
                    Log.d("++image URi", "$currentImageURL")
                }
            }
        }
    }

    fun requestToServer() {
        Runnable {
            brailleDao.requestData().enqueue(object : Callback<String> {
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

//
//    private fun postImageToServer(image: MultipartBody.Part) {
//
//        Runnable {
//            brailleAPI.postImage(body)
//                .enqueue(object : Callback<ResponseBody> {
//                    override fun onResponse(
//                        call: Call<ResponseBody>,
//                        response: Response<ResponseBody>
//                    ) {
//                        if (response.isSuccessful) {
//                            val body = response.body()
//                            Toast.makeText(requireContext(), "파일 업로드 성공", Toast.LENGTH_SHORT)
//                                .show()
//                            Log.d("++postImageToServer 파일 업로드 성공", "body : $body, $response")
//                        } else {
//                            Toast.makeText(requireContext(), "파일 업로드 실패", Toast.LENGTH_SHORT)
//                                .show()
//                            Log.d("++postImageToServer 파일 업로드 실패", "$response")
//                        }
//                    }
//
//                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                        Toast.makeText(requireContext(), "서버를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
//                            .show()
//                        Log.d("++onFailure", t.toString())
//                    }
//                })
//        }.run()
//    }

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

//private fun getPath(context: Context, uri: Uri): String? {
//        val isKitKat: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
//
//        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
//            if (isExternalStorageDocument(uri)) {
//                val docId: String = DocumentsContract.getDocumentId(uri)
//                val split = docId.split(":")
//                val type: String = split[0]
//
//                if ("primary".equals(type, true)) {
//                    return "${Environment.getExternalStorageDirectory()}/${split[1]}"
//                }
//            } else if (isDownloadsDocument(uri)) {
//                val id: String = DocumentsContract.getDocumentId(uri)
//                val contentUri: Uri = ContentUris.withAppendedId(
//                    Uri.parse("content://downloads/public_downloads"), id.toLong()
//                )
//            } else if (isMediaDocument(uri)) {
//                val docId: String = DocumentsContract.getDocumentId(uri)
//                val split = docId.split(":")
//                val type: String = split[0]
//
//                var contentUri: Uri? = null
//                if ("image".equals(type)) {
//                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                } else {
//                    Log.e("++getPath", "경로를 찾을 수 없습니다. ${contentUri}")
//                }
//                val selection: String = "_id=?"
//                val selectionArgs: Array<String> = arrayOf(split[1])
//                return getDataColumn(context, contentUri!!, selection, selectionArgs)
//            } else if ("content".equals(uri.scheme, true)) {
//                return getDataColumn(context, uri, null, null)
//            } else if ("file".equals(uri.scheme, true)) {
//                return uri.path
//            }
//            return null
//        }
//
//        return null
//    }
//
//    private fun getDataColumn(
//        context: Context,
//        uri: Uri,
//        selection: String?,
//        selectionArgs: Array<String>?
//    ): String? {
//        var cursor: Cursor? = null
//        val column: String = "_data"
//        val projection: Array<String> = arrayOf(column)
//        try {
//            cursor =
//                requireContext().contentResolver.query(
//                    uri,
//                    projection,
//                    selection,
//                    selectionArgs,
//                    null
//                )
//            if (cursor != null && cursor.moveToFirst()) {
//                val column_index: Int = cursor.getColumnIndexOrThrow(column)
//                return cursor.getString(column_index)
//            }
//        } finally {
//            if (cursor != null) {
//                cursor.close()
//            }
//            return null
//        }
//    }
//
//    private fun isExternalStorageDocument(uri: Uri): Boolean {
//        return "com.android.externalstorage.documents".equals(uri.authority)
//    }
//
//    private fun isDownloadsDocument(uri: Uri): Boolean {
//        return "com.android.providers.downloads.documents".equals(uri.authority)
//    }
//
//    private fun isMediaDocument(uri: Uri): Boolean {
//        return "com.android.providers.media.documents".equals(uri.authority)
//    }
//
//
//    private fun getLastCaptureImageUri(): Uri? {
//        var uri: Uri? = null
//        val IMAGE_PROJECTION: Array<String> = arrayOf(
//            MediaStore.Images.ImageColumns.DATA,
//            MediaStore.Images.ImageColumns._ID
//        )
//        try {
//            val cursorImage: Cursor? = requireContext().contentResolver.query(
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                IMAGE_PROJECTION,
//                null,
//                null,
//                null
//            )
//            if (cursorImage != null && cursorImage.moveToLast()) {
//                uri = Uri.parse(cursorImage.getString(0)) // 경로
//                val id: Int = cursorImage.getInt(1) // 아이디
//                cursorImage.close()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return uri
//    }