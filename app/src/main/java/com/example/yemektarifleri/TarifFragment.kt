package com.example.yemektarifleri

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import java.io.ByteArrayOutputStream


class TarifFragment : Fragment() {
    private lateinit var imageButton: ImageView
    private lateinit var kaydetButton: Button
    private lateinit var yemekIsmiText: EditText
    private lateinit var yemekMalzemeText: EditText



    var secilenGorsel: Uri? = null
    var secilenBitmap: Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tarif, container, false)


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        kaydetButton = view.findViewById(R.id.kaydetButton)
        imageButton = view.findViewById(R.id.gorsel)
        yemekIsmiText = view.findViewById(R.id.yemekIsmiText)
        yemekMalzemeText = view.findViewById(R.id.yemekMalzemeText)
        kaydetButton.setOnClickListener {
            kaydet(it)
        }

        imageButton.setOnClickListener {
            gorselSec(it)
        }
        arguments?.let {
            var gelenBilgi = TarifFragmentArgs.fromBundle(it).bilgi
            if(gelenBilgi.equals("menudengeldim")){
                //yeni yemek eklemeye geldi
                yemekIsmiText.setText("")
                yemekMalzemeText.setText("")
                kaydetButton.visibility = View.VISIBLE


                val gorselSecmeArkaPlani = BitmapFactory.decodeResource(context?.resources, R.drawable.gorselsec)
                imageButton.setImageBitmap(gorselSecmeArkaPlani)


            }else{
                //daha önce oluşturulan yemeği görmeye geldi
                kaydetButton.visibility = View.INVISIBLE

                val secilenId = TarifFragmentArgs.fromBundle(it).id

                context?.let {
                    try {
                        val db = it.openOrCreateDatabase("Yemekler",Context.MODE_PRIVATE,null)
                        val cursor = db.rawQuery("SELECT * FROM yemekler  WHERE id = ?", arrayOf(secilenId.toString()))
                        val yemekIsmiIndex = cursor.getColumnIndex("yemekismi")
                        val yemekMalzemeIndex = cursor.getColumnIndex("yemekmalzemesi")
                        val yemekGorseli = cursor.getColumnIndex("gorsel")

                        while(cursor.moveToNext()){
                            yemekIsmiText.setText(cursor.getString(yemekIsmiIndex))
                            yemekMalzemeText.setText(cursor.getString(yemekMalzemeIndex))

                            val byteDizisi = cursor.getBlob(yemekGorseli)
                            val bitmap = BitmapFactory.decodeByteArray(byteDizisi,0,byteDizisi.size)
                            imageButton.setImageBitmap(bitmap)

                        }
                        cursor.close()

                    }catch (e: Exception){
                        e.printStackTrace()
                    }

                }

            }
        }

    }

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->

            if (isGranted) {
                println("izin verildi")
                val galeriIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                //startActivityForResult(galeriIntent,2)
                resultLauncher.launch(galeriIntent)
            } else {
                println("izin verilmedi")
            }
        }
    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            imageButton = view!!.findViewById(R.id.gorsel)

            if (it.resultCode == Activity.RESULT_OK && it.data != null) {

                secilenGorsel = it.data!!.data
                try {

                    context?.let {

                        if (secilenGorsel != null && context != null) {
                            if (Build.VERSION.SDK_INT >= 28) {

                                val source =
                                    ImageDecoder.createSource(it.contentResolver, secilenGorsel!!)
                                secilenBitmap = ImageDecoder.decodeBitmap(source)

                                imageButton.setImageBitmap(secilenBitmap)
                            } else {
                                secilenBitmap = MediaStore.Images.Media.getBitmap(
                                    it.contentResolver,
                                    secilenGorsel
                                )

                                imageButton.setImageBitmap(secilenBitmap)
                            }
                        }
                    }


                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    private fun gorselSec(view: View) {
        println("gorsel tıklandı")

        activity?.let {
            if (ContextCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //izin verilmedi, izin iste
                //requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                //izin verilmiş direk galeriye git
                val galeriIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                //startActivityForResult(galeriIntent,2)
                resultLauncher.launch(galeriIntent)

            }
        }
    }

    private fun kaydet(view: View) {
        println("kaydet tıklandı")
        kaydetButton = view.findViewById(R.id.kaydetButton)
        println("ilginç")
        val yemekIsmi = yemekIsmiText.text.toString()
        val yemekMalzemeleri = yemekMalzemeText.text.toString()


        if (secilenBitmap != null) {
            val kucukBitmap = bitmapiKucult(secilenBitmap!!, 300)

            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteDizisi = outputStream.toByteArray()

            try {
                context?.let {
                    println("create kısmına girdi")
                    val database = it.openOrCreateDatabase("Yemekler", Context.MODE_PRIVATE, null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS yemekler (id INTEGER PRIMARY KEY, yemekismi VARCHAR, yemekmalzemesi VARCHAR, gorsel BLOB)")
                    val sqlString =
                        "INSERT INTO yemekler (yemekismi, yemekmalzemesi, gorsel) VALUES (?, ?, ?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1, yemekIsmi)
                    statement.bindString(2, yemekMalzemeleri)
                    statement.bindBlob(3, byteDizisi)
                    statement.execute()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
            val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
            Navigation.findNavController(view).navigate(action)

        }
    }

    fun bitmapiKucult(gelenBitmap: Bitmap, maxBoyut: Int): Bitmap {
        var width = gelenBitmap.width
        var height = gelenBitmap.height

        val bitmapOran: Double = width.toDouble() / height.toDouble()

        if (bitmapOran > 1) {
            //yatay
            width = maxBoyut
            val kısaHeight = width / bitmapOran
            height = kısaHeight.toInt()

        } else {
            //dikey
            height = maxBoyut
            val kisaWidth = height * bitmapOran
            width = kisaWidth.toInt()
        }
        return Bitmap.createScaledBitmap(gelenBitmap, width, height, true)
    }


}




