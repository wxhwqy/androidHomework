package com.example.test4.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.test4.R
import com.example.test4.data.Note
import com.example.test4.databinding.ActivityNoteDetailBinding
import com.example.test4.utils.ImageUtils
import com.example.test4.viewmodel.NoteViewModel
import java.io.File

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var imageUtils: ImageUtils
    
    private var currentNote: Note? = null
    private var noteId: Long = -1L
    private var isEditMode = false
    private var selectedImagePath: String? = null
    
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        imageUtils = ImageUtils(this)
        setupImagePicker()
        setupBackPressedCallback()
        
        noteId = intent.getLongExtra("note_id", -1L)
        isEditMode = noteId != -1L
        
        setupToolbar()
        setupViews()
        
        if (isEditMode) {
            loadNote()
        } else {
            setupForNewNote()
        }
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                imageUri?.let {
                    handleImageSelection(it)
                }
            }
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            handleBackPressed()
        }
        
        val title = if (isEditMode) "编辑备忘录" else "新建备忘录"
        supportActionBar?.title = title
    }

    private fun setupViews() {
        val categories = arrayOf("默认", "工作", "生活", "学习", "重要")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)

        binding.btnSelectImage.setOnClickListener {
            selectImage()
        }

        binding.btnSave.setOnClickListener {
            saveNote()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
        
        binding.btnDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE
    }

    private fun setupForNewNote() {
        binding.actvCategory.setText("默认", false)
    }

    private fun loadNote() {
        noteViewModel.getNoteById(noteId).observe(this) { note ->
            note?.let {
                currentNote = it
                populateFields(it)
            }
        }
    }

    private fun populateFields(note: Note) {
        binding.etTitle.setText(note.title)
        binding.etContent.setText(note.content)
        binding.actvCategory.setText(note.category, false)
        
        selectedImagePath = note.imagePath
        if (!note.imagePath.isNullOrEmpty() && File(note.imagePath).exists()) {
            binding.ivSelectedImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(note.imagePath)
                .centerCrop()
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .into(binding.ivSelectedImage)
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageSelection(imageUri: Uri) {
        try {
            selectedImagePath = imageUtils.saveImageToInternalStorage(imageUri)
            
            if (selectedImagePath != null) {
                binding.ivSelectedImage.visibility = View.VISIBLE
                Glide.with(this)
                    .load(selectedImagePath)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .into(binding.ivSelectedImage)
                
                Toast.makeText(this, "图片选择成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "图片处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilTitle.error = "请输入标题"
            return
        }

        if (content.isEmpty()) {
            binding.tilContent.error = "请输入内容"
            return
        }

        binding.tilTitle.error = null
        binding.tilContent.error = null

        val currentTime = System.currentTimeMillis()

        if (isEditMode && currentNote != null) {
            val updatedNote = currentNote!!.copy(
                title = title,
                content = content,
                category = category.ifEmpty { "默认" },
                imagePath = selectedImagePath,
                updatedAt = currentTime
            )
            noteViewModel.updateNote(updatedNote)
            Toast.makeText(this, "备忘录已更新", Toast.LENGTH_SHORT).show()
        } else {
            val newNote = Note(
                title = title,
                content = content,
                category = category.ifEmpty { "默认" },
                imagePath = selectedImagePath,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            noteViewModel.insertNote(newNote).observe(this) { id ->
                if (id > 0) {
                    Toast.makeText(this, "备忘录已保存", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            return
        }
        
        finish()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("删除备忘录")
            .setMessage("确定要删除这条备忘录吗？此操作无法撤销。")
            .setPositiveButton("删除") { _, _ ->
                deleteNote()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteNote() {
        currentNote?.let { note ->
            noteViewModel.deleteNote(note)
            
            note.imagePath?.let { imagePath ->
                imageUtils.deleteImageFile(imagePath)
            }
            
            Toast.makeText(this, "备忘录已删除", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun shareNote() {
        currentNote?.let { note ->
            val shareText = "${note.title}\n\n${note.content}\n\n类别: ${note.category}\n时间: ${note.getFormattedDate()}"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, note.title)
            }
            startActivity(Intent.createChooser(shareIntent, "分享备忘录"))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isEditMode) {
            menuInflater.inflate(R.menu.detail_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                shareNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleBackPressed() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle("放弃更改")
                .setMessage("您有未保存的更改，确定要退出吗？")
                .setPositiveButton("退出") { _, _ ->
                    finish()
                }
                .setNegativeButton("继续编辑", null)
                .show()
        } else {
            finish()
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        if (!isEditMode) {
            return binding.etTitle.text.toString().trim().isNotEmpty() ||
                   binding.etContent.text.toString().trim().isNotEmpty()
        } else {
            currentNote?.let { note ->
                return binding.etTitle.text.toString().trim() != note.title ||
                       binding.etContent.text.toString().trim() != note.content ||
                       binding.actvCategory.text.toString().trim() != note.category ||
                       selectedImagePath != note.imagePath
            }
        }
        return false
    }
} 