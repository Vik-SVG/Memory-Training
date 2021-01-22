package com.vkpriesniakov.memorytraining

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.vkpriesniakov.memorytraining.adapters.ImagePickerAdapter
import com.vkpriesniakov.memorytraining.databinding.ActivityCreateBinding
import com.vkpriesniakov.memorytraining.models.BoardSize
import com.vkpriesniakov.memorytraining.utils.*
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
    private const val PICK_PHOTOS_REQUEST_CODE = 655
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val READ_EXTERNAL_PHOTOS_CODE = 244
        private const val TAG = "CreateActivity"
        private const val MIN_GAME_LENGTH = 3
        private const val MAX_GAME_LENGTH = 14
    }
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private lateinit var bdn: ActivityCreateBinding
    private val chosenImageUris = mutableListOf<Uri>()
    private lateinit var mAdapter: ImagePickerAdapter
    private val storage  = Firebase.storage
    private val db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bdn = ActivityCreateBinding.inflate(layoutInflater)
        val view = bdn.root
        setContentView(view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"


        bdn.btnSave.setOnClickListener{
            saveDataToFirebase()
        }
        bdn.edIdGame.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_LENGTH))

        bdn.btnSave.isEnabled =false
        bdn.edIdGame.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                bdn.btnSave.isEnabled = shouldEnableSaveButton()
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {} })

        mAdapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object:ImagePickerAdapter.ImageClickListener {
            override fun onPlaceholderClicked() {
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                    launchIntentForPhotos()
                } else{
                    requestPermissionForApp(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }

        })
        bdn.rvImagePicker.adapter = mAdapter
        bdn.rvImagePicker.setHasFixedSize(true)
        bdn.rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            } else{
                Toast.makeText(this, "In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTOS_REQUEST_CODE || resultCode !=Activity.RESULT_OK || data == null){
            Log.w(TAG, "Did not get data back from activity")
            return
        }
        val selectedUri = data.data
        val clipData = data.clipData
        if (clipData!=null){
            Log.i(TAG, "ClipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount){
                val clipItem = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired){
                    chosenImageUris.add(clipItem.uri)
                } else {
                    //TODO: message toast for 'too much images'
                }
            }
        } else if (selectedUri!= null){
            Log.i(TAG, "$selectedUri")
            chosenImageUris.add(selectedUri)
        }
        mAdapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pick (${chosenImageUris.size} / $numImagesRequired)"
        bdn.btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size!=numImagesRequired){
            return false
        }
        if (bdn.edIdGame.text!!.isBlank() || bdn.edIdGame.text!!.length < MIN_GAME_LENGTH){
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val chooserIntent = Intent()
        chooserIntent.type = "image/*"
        chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        chooserIntent.action = (Intent.ACTION_GET_CONTENT) //Intent.ACTION_PICK isn't work
        val finalIntent = Intent.createChooser(chooserIntent, "Choose pics")
        startActivityForResult(finalIntent, PICK_PHOTOS_REQUEST_CODE)
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scalded width ${scaledBitmap.width} and height ${scaledBitmap.height}")

        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun saveDataToFirebase() {
        bdn.btnSave.isEnabled =false
        Log.i(TAG, "saveDataForFirebase")
        val customGameName = bdn.edIdGame.text.toString()
        // Check that we-re not over writing data
        db.collection("games").document(customGameName).get().addOnSuccessListener {document ->
            if (document !=null && document.data != null){
                AlertDialog.Builder(this)
                        .setTitle("Name taken")
                        .setMessage("A game already exist with name $customGameName")
                        .setPositiveButton("Ok", null)
                        .show()
                bdn.btnSave.isEnabled =true
            } else{
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener { exc ->
            Log.e(TAG, "Error in saving game", exc)
            Toast.makeText(this, "Error in saving game", Toast.LENGTH_SHORT).show()
            bdn.btnSave.isEnabled =true
        }
    }

    private fun handleImageUploading(gameName: String) {
        bdn.pbUploading.visibility = View.VISIBLE

        var didEncounter = false
        val uploadedImageUrls = mutableListOf<String>()
        for((index, photoUri) in chosenImageUris.withIndex()){
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                    .continueWithTask { photoUploadTask ->
                        Log.i(TAG, "Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                        photoReference.downloadUrl
                    }.addOnCompleteListener { downloadUrlTask ->
                        if (!downloadUrlTask.isSuccessful){
                            Log.e(TAG, "Exception", downloadUrlTask.exception)
                            Toast.makeText(this, "Downloading failed", Toast.LENGTH_SHORT).show()
                            didEncounter = true
                            return@addOnCompleteListener
                        }
                        if (didEncounter){
                            bdn.pbUploading.visibility = View.GONE
                            return@addOnCompleteListener
                        }

                        val downloadUrl = downloadUrlTask.result.toString()
                        uploadedImageUrls.add(downloadUrl)
                        bdn.pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
                        Log.i(TAG, "Finished upload $photoUri")

                        if (uploadedImageUrls.size == chosenImageUris.size){
                            handleAllImagesUploaded(gameName, uploadedImageUrls)
                        }
                    }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        db.collection("games").document(gameName)
                .set(mapOf("images" to imageUrls))
                .addOnCompleteListener { gameCreationTask ->
                    bdn.pbUploading.visibility = View.GONE
                    if (!gameCreationTask.isSuccessful){
                        Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                        Toast.makeText(this,"Failed game creation", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }
                    Log.i(TAG, "Successfully created game $gameName")
                    AlertDialog.Builder(this)
                            .setTitle("Upload complete! Let's play your game $gameName")
                            .setPositiveButton("Ok"){_,_, ->
                                val resultData = Intent()
                                resultData.putExtra(EXTRA_GAME_NAME, gameName)
                                setResult(Activity.RESULT_OK, resultData)
                                finish()
                            }.show()
                }
    }
}