package jp.techacademy.naoya.konita.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val PERMISSIONS_REQUEST_CODE = 100

    // cursorをすべてのボタンで取り回せるようにここで宣言しているが、もっといい方法はあるのか？→これでOK
    private var cursor: Cursor? = null

    private var mTimer: Timer? = null
    private var mHandler = Handler()

    private var autoPlaying = false // スライドショー再生中か否か

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        forward_button.setOnClickListener(this)
        back_button.setOnClickListener(this)
        play_button.setOnClickListener(this)

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo()
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo()
        }

    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.forward_button -> getNextInfo()
            R.id.back_button -> getPreviousInfo()
            R.id.play_button -> playOrStopSlideShow()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getContentsInfo()
            } else {
                // 権限を許可しない場合は、どのように実装するのがよいのか？とりあえずボタンを非表示して操作できないようにしてみる→ボタンを押したときにアラートを上げる方がいい
                forward_button.visibility = View.INVISIBLE
                back_button.visibility = View.INVISIBLE
                play_button.visibility = View.INVISIBLE
            }
        }
    }

    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor_tmp = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目（null = 全項目）
            null, // フィルタ条件（null = フィルタなし）
            null, // フィルタ用パラメータ
            null // ソート (nullソートなし）
        )

        var picture_num = 0
        if (cursor_tmp!!.moveToFirst()) {
            val fieldIndex = cursor_tmp.getColumnIndex(MediaStore.Images.Media._ID)
            val id = cursor_tmp.getLong(fieldIndex)
            val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            image_view.setImageURI(imageUri)
        }

        cursor = cursor_tmp
    }

    private fun getNextInfo() {
        Log.d("PIC_DEBUG","進むボタンが押されました")
        if (!autoPlaying) {
            if (!cursor!!.moveToNext()) {
                cursor!!.moveToFirst()
            }

            val fieldIndex = cursor!!.getColumnIndex(MediaStore.Images.Media._ID)
            val id = cursor!!.getLong(fieldIndex)
            val imageUri =
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            image_view.setImageURI(imageUri)
        }
    }

    private fun getPreviousInfo() {
        Log.d("PIC_DEBUG","戻るボタンが押されました")
        if (!autoPlaying) {
            if (!cursor!!.moveToPrevious()) {
                cursor!!.moveToLast()
            }

            val fieldIndex = cursor!!.getColumnIndex(MediaStore.Images.Media._ID)
            val id = cursor!!.getLong(fieldIndex)
            val imageUri =
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            image_view.setImageURI(imageUri)
        }
    }

    private fun playOrStopSlideShow() {
        if (mTimer == null) {
            Log.d("PIC_DEBUG", "再生ボタンが押されました")
            play_button.text = "停止"
            autoPlaying = true

            mTimer = Timer()
            mTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    Log.d("PIC_DEBUG", "Timer called")

                    if (!cursor!!.moveToNext()) {
                        cursor!!.moveToFirst()
                    }

                    val fieldIndex = cursor!!.getColumnIndex(MediaStore.Images.Media._ID)
                    val id = cursor!!.getLong(fieldIndex)
                    val imageUri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    mHandler.post {
                        image_view.setImageURI(imageUri)
                    }
                }
            }, 2000, 2000)
        } else {
            Log.d("PIC_DEBUG", "停止ボタンが押されました")
            play_button.text = "再生"
            autoPlaying = false

            mTimer!!.cancel()
            mTimer = null
        }
    }
}