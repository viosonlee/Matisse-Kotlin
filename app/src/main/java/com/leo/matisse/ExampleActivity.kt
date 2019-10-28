package com.leo.matisse

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.matisse.Matisse
import com.matisse.MimeType
import com.matisse.MimeTypeManager
import com.matisse.compress.CompressHelper
import com.matisse.compress.FileUtil
import com.matisse.entity.CaptureStrategy
import com.matisse.entity.ConstValue
import com.matisse.listener.Consumer
import com.matisse.utils.PhotoMetadataUtils
import com.matisse.utils.Platform
import com.matisse.widget.CropImageView
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_example.*

class ExampleActivity : AppCompatActivity(), View.OnClickListener {
    private var showType = MimeTypeManager.ofAll()
    private var showCustomizeType: MutableList<MimeType>? = null
    private var mediaTypeExclusive = true
    private var isSingleChoose = false
    private var isCountable = true
    private var defaultTheme = R.style.Matisse_Default
    private var maxCount = 5
    private var maxImageCount = 1
    private var maxVideoCount = 1
    private var isOpenCamera = false
    private var spanCount = 3
    private var gridSizePx = 0
    private var isDarkStatus = false
    private var isCrop = false
    private var cropWidth = -1
    private var cropHeight = -1
    private var isSaveRectangle = false
    private var cropType = CropImageView.Style.RECTANGLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        initListener()
    }

    private fun initListener() {
        rg_show.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btnAll -> showType = MimeTypeManager.ofAll()
                R.id.btnVideo -> showType = MimeTypeManager.ofVideo()
                R.id.btnImage -> showType = MimeTypeManager.ofImage()
            }
        }

        rg_media_exclusive.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btnMixed -> {
                    mediaTypeExclusive = false
                    ev_max_2.visibility = View.VISIBLE
                }
                R.id.btnExclusive -> {
                    mediaTypeExclusive = true
                    ev_max_2.visibility = View.GONE
                }
            }
        }

        rg_theme.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btn_normal_theme -> defaultTheme = R.style.Matisse_Default
                R.id.btn_customize_theme -> defaultTheme = R.style.CustomMatisseStyle
            }
        }

        chb_jpeg.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_png.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_gif.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_bmp.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_webp.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_mpeg.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_mp4.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_quick_time.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_threegpp.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_threegpp2.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_mkv.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_webm.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_ts.setOnCheckedChangeListener(checkedOnCheckedListener)
        chb_avi.setOnCheckedChangeListener(checkedOnCheckedListener)

        if (showCustomizeType != null && showCustomizeType?.size ?: 0 > 0) {
            showType =
                MimeTypeManager.of(showCustomizeType!![0], showCustomizeType?.toTypedArray()!!)
        }

        switch_choose_type.setOnCheckedChangeListener { _, isChecked ->
            isSingleChoose = isChecked
            if (isSingleChoose) {
                maxCount = 1
                maxImageCount = 1
                maxVideoCount = 1
                ev_max_1.visibility = View.GONE
            } else {
                ev_max_1.visibility = View.VISIBLE
            }
        }

        switch_check_type.setOnCheckedChangeListener { _, isChecked ->
            isCountable = !isChecked
        }

        switch_capture.setOnCheckedChangeListener { _, isChecked ->
            isOpenCamera = isChecked
        }

        switch_status.setOnCheckedChangeListener { _, isChecked ->
            isDarkStatus = isChecked
        }

        switch_crop.setOnCheckedChangeListener { _, isChecked ->
            isCrop = isChecked
        }

        switch_rectangle_save.setOnCheckedChangeListener { _, isChecked ->
            isSaveRectangle = isChecked
        }

        switch_crop_type.setOnCheckedChangeListener { _, isChecked ->
            cropType = if (isChecked) {
                CropImageView.Style.CIRCLE
            } else {
                CropImageView.Style.RECTANGLE
            }
        }

        btn_open_matisse.setOnClickListener(this)
        btn_open_matisse.setOnClickListener(this)

        ev_max_1.addTextChangedListener(textChanged(ev_max_1))
        ev_max_2.addTextChangedListener(textChanged(ev_max_2))
        ev_span_1.addTextChangedListener(textChanged(ev_span_1))
        ev_span_2.addTextChangedListener(textChanged(ev_span_2))
    }

    private fun showToast(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return

        if (requestCode == ConstValue.REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            var string = ""

            // 获取uri返回值  裁剪结果不返回uri
            val uriList = Matisse.obtainResult(data)
            // 获取文件路径返回值
            val strList = Matisse.obtainPathResult(data)

            uriList.forEach {
                string += it.toString() + "\n"
            }

            string += "\n"

            strList.forEach {
                string += it + "\n"
            }

            // 原文件
            val file = FileUtil.getFileByPath(Matisse.obtainPathResult(data)[0])

            Glide.with(this).load(file).into(iv_image)
            // 压缩后的文件         （多个文件压缩可以循环压缩）
            val file1 = CompressHelper.getDefault(applicationContext)?.compressToFile(file)
            string += PhotoMetadataUtils.getSizeInMB(file.length()).toString() + " PK " +
                    PhotoMetadataUtils.getSizeInMB(file1?.length() ?: 0)
            string = "\n\n$string"

            text.text = "\n\n$string"
        }
    }

    private var checkedOnCheckedListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val mimeType = when (buttonView) {
                chb_jpeg -> MimeType.JPEG
                chb_png -> MimeType.PNG
                chb_gif -> MimeType.GIF
                chb_bmp -> MimeType.BMP
                chb_webp -> MimeType.WEBP
                chb_mpeg -> MimeType.MPEG
                chb_mp4 -> MimeType.MP4
                chb_quick_time -> MimeType.QUICKTIME
                chb_threegpp -> MimeType.THREEGPP
                chb_threegpp2 -> MimeType.THREEGPP2
                chb_mkv -> MimeType.MKV
                chb_webm -> MimeType.WEBM
                chb_ts -> MimeType.TS
                chb_avi -> MimeType.AVI
                else -> null
            } ?: return@OnCheckedChangeListener

            if (showCustomizeType == null)
                showCustomizeType = mutableListOf()

            if (isChecked) {
                showCustomizeType?.add(mimeType)
            } else {
                showCustomizeType?.remove(mimeType)
            }
        }

    @SuppressLint("CheckResult")
    override fun onClick(v: View?) {

        RxPermissions(this@ExampleActivity)
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .subscribe {
                if (!it) {
                    Toast.makeText(
                        this@ExampleActivity, R.string.permission_request_denied, Toast.LENGTH_LONG
                    ).show()
                    return@subscribe
                }
            }

        when (v) {
            btn_open_matisse -> {
                openMatisse()
            }
            btn_open_capture -> {
            }
        }
    }

    private fun textChanged(ev: EditText) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            when (ev) {
                ev_max_1 -> {
                    if (mediaTypeExclusive) maxImageCount = formatStrTo0(s.toString())
                    else maxCount = formatStrTo0(s.toString())
                }
                ev_max_2 -> maxVideoCount = formatStrTo0(s.toString())
                ev_span_1 -> {
                    spanCount = formatStrTo0(s.toString())
                    gridSizePx = 0
//                    ev_span_1.setText("0")
                }
                ev_span_2 -> {
                    gridSizePx = formatStrTo0(s.toString())
                    spanCount = 0
//                    ev_span_2.setText("0")
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    private fun openMatisse() {
        setEditText()

        Matisse.from(this@ExampleActivity)
            .choose(showType, mediaTypeExclusive)
            .theme(defaultTheme)
            .countable(isCountable)
            .capture(isOpenCamera)
            .isCrop(isCrop)
            .cropStyle(cropType)
            .cropFocusWidthPx(cropWidth)
            .cropFocusHeightPx(cropHeight)
            .isCropSaveRectangle(isSaveRectangle)
            .maxSelectable(maxCount)
            .maxSelectablePerMediaType(maxImageCount, maxVideoCount)
            .setStatusIsDark(isDarkStatus)
            .captureStrategy(
                CaptureStrategy(
                    true,
                    "${Platform.getPackageName(this@ExampleActivity)}.fileprovider"
                )
            )
            .thumbnailScale(0.6f)
            .spanCount(spanCount)
            .gridExpectedSize(gridSizePx)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .imageEngine(Glide4Engine())
            .theme(R.style.CustomMatisseStyle)
            .setNoticeConsumer(object : Consumer<String> {
                override fun accept(params: String) {
                    showToast(params)
                }
            })
            .forResult(ConstValue.REQUEST_CODE_CHOOSE)
    }

    private fun setEditText() {
        if (!mediaTypeExclusive) {
            maxImageCount = formatStrTo0(ev_max_1.text.toString())
            maxVideoCount = formatStrTo0(ev_max_2.text.toString())
        } else {
            if (!isSingleChoose) {
                maxCount = formatStrTo0(ev_max_1.text.toString())
            }
        }

        spanCount = formatStrTo0(ev_span_1.text.toString())
        if (spanCount > 0) {
            gridSizePx = 0
        }
    }

    private fun formatStrTo0(s: String?): Int {
        if (s == null || s.toString() == "") {
            return 0
        }
        return s.toString().toInt()
    }
}
