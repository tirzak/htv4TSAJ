package com.multimeleon.pjapples.automldemo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.hardware.Camera
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.IOException



class MainActivity : AppCompatActivity() {

    private val GALLERY = 1
    private var AutoMLEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        callAutoMLModelLocally()
        callAutoMLModelRemotely()
        uploadImageButton.setOnClickListener {
            showPictureDialog()
        }
    }

    private fun choosePhotoFromGallary() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        result_textview.setText("")

        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun openCamera(){
            ;

    }
    val REQUEST_IMAGE_CAPTURE = 1

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }


    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(
            pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallary()
                1 -> openCamera()

            }
        }
        pictureDialog.show()
    }

    private fun callAutoMLModelLocally() {
        val localModel = FirebaseLocalModel.Builder("mylml")
            .setAssetFilePath("manifest.json")
            .build()
        FirebaseModelManager.getInstance().registerLocalModel(localModel)

    }


    fun imageFromArray(byteArray: ByteArray) {
        val metadata = FirebaseVisionImageMetadata.Builder()
            .setWidth(480) // 480x360 is typically sufficient for
            .setHeight(360) // image recognition
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(FirebaseVisionImageMetadata.ROTATION_0)
            .build()
        val image = FirebaseVisionImage.fromByteArray(byteArray, metadata)
        // [END image_from_array]


        val labelerOptions = FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder()
            .setLocalModelName("mylml")
            .setRemoteModelName("saw")  // Skip to not use a remote model
            .setConfidenceThreshold(0F)  // Evaluate your model in the Firebase console
            // to determine an appropriate value.
            .build()
        val labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(labelerOptions)

        labeler.processImage(image)
            .addOnSuccessListener { labels ->
                // Task completed successfully
                // ...
                val max=0;
                var text="";
                for (label in labels) {
                    if(label.confidence>max){
                       text =label.text;
                    }

                    val confidence = label.confidence


                    result_textview.text = " ${result_textview.text} $text $confidence \n"
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }

    }

    private fun downloadRemoteModel(remoteModel: FirebaseRemoteModel) {
        val optionsBuilder =
            FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder().setConfidenceThreshold(0.4f)

        optionsBuilder.setLocalModelName("mylml").setRemoteModelName("saw")

        Toast.makeText(this, "Begin downloading the remote AutoML model.", Toast.LENGTH_SHORT)
            .show()

        FirebaseModelManager.getInstance().downloadRemoteModelIfNeeded(remoteModel)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Download remote AutoML model success.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    val downloadingError =
                        "Error downloading remote model."

                    Toast.makeText(this, downloadingError, Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun callAutoMLModelRemotely() {
        val conditions = FirebaseModelDownloadConditions.Builder()
            .requireWifi()
            .build()
        val remoteModel = FirebaseRemoteModel.Builder("saw")
            .enableModelUpdates(true)
            .setInitialDownloadConditions(conditions)
            .setUpdatesDownloadConditions(conditions)
            .build()
        FirebaseModelManager.getInstance().registerRemoteModel(remoteModel)
        downloadRemoteModel(remoteModel)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY) {
            if (data != null) {
                val contentURI = data.data
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)

                    Toast.makeText(this@MainActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
                    imageView.setImageBitmap(bitmap)


                    val imgString = Base64.encodeToString(
                        getBytesFromBitmap(bitmap),
                        Base64.NO_WRAP
                    )

                    if (AutoMLEnabled) {
                        imageFromArray(getBytesFromBitmap(bitmap))
                    } else {
                        val requestBody = ModelRequestBody(PayloadRequest(ModelImage(imgString)))
                        Network("https://automl.googleapis.com/v1beta1/", true)
                            .getRetrofitClient()
                            .create(Endpoint::class.java)
                            .classifyImage(requestBody)
                            .enqueue(object : Callback<PayloadResult> {

                                override fun onResponse(
                                    call: Call<PayloadResult>?,
                                    response: Response<PayloadResult>?
                                ) {
                                    if (response!!.isSuccessful) {
                                        Log.d("Hello", response.body().toString())
                                        result_textview.setText("")
                                        result_textview.text =
                                            "${response.body()?.items?.first()?.displayName} Score: ${(response.body()?.items?.first()?.classification?.let { it.score * 100 })}"
                                    }
                                }

                                override fun onFailure(call: Call<PayloadResult>, t: Throwable) {
                                    print(t.message)
                                }
                            }
                            )
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun getBytesFromBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 70, stream)
        return stream.toByteArray()
    }


}