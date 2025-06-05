package com.example.test4

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test4.adapter.NoteAdapter
import com.example.test4.data.Note
import com.example.test4.databinding.ActivityMainBinding
import com.example.test4.ui.LoginActivity
import com.example.test4.ui.NoteDetailActivity
import com.example.test4.utils.BackupRestoreHelper
import com.example.test4.utils.PreferencesHelper
import com.example.test4.viewmodel.NoteViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var backupRestoreHelper: BackupRestoreHelper
    private val noteViewModel: NoteViewModel by viewModels()
    
    private var currentNotesLiveData: LiveData<List<Note>>? = null
    private var currentObserver: Observer<List<Note>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesHelper = PreferencesHelper(this)
        backupRestoreHelper = BackupRestoreHelper(this)
        
        val fromLogin = intent.getBooleanExtra("from_login", false)
        if (!fromLogin && !preferencesHelper.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        setupToolbar()
        setupRecyclerView()
        setupViews()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter { note ->
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra("note_id", note.id)
            startActivity(intent)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }
    }

    private fun setupViews() {
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, NoteDetailActivity::class.java)
            startActivity(intent)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    loadNotes()
                } else {
                    searchNotes(query)
                }
            }
        })

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }

        // 
        setupCategorySpinner()
    }

    private fun setupCategorySpinner() {
        noteViewModel.getAllCategories().observe(this) { categories ->
            val allCategories = mutableListOf("全部").apply { addAll(categories) }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allCategories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
            
            binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedCategory = allCategories[position]
                    preferencesHelper.setSelectedCategory(selectedCategory)
                    filterByCategory(selectedCategory)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            val savedCategory = preferencesHelper.getSelectedCategory()
            val savedIndex = allCategories.indexOf(savedCategory)
            if (savedIndex >= 0) {
                binding.spinnerCategory.setSelection(savedIndex)
            }
        }
    }

    private fun observeData() {
        loadNotes()
    }

    private fun loadNotes() {
        currentObserver?.let { observer ->
            currentNotesLiveData?.removeObserver(observer)
        }
        
        val sortOrder = preferencesHelper.getSortOrder()
        val selectedCategory = preferencesHelper.getSelectedCategory()
        
        currentNotesLiveData = when (sortOrder) {
            "title_asc" -> noteViewModel.getNotesSortedByTitle()
            "created_desc" -> noteViewModel.getNotesSortedByCreatedDate()
            else -> noteViewModel.getAllNotes()
        }
        
        currentObserver = Observer<List<Note>> { notes ->
            val filteredNotes = if (selectedCategory == "全部") {
                notes
            } else {
                notes.filter { it.category == selectedCategory }
            }
            
            noteAdapter.submitList(filteredNotes)
            
            if (filteredNotes.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
        
        currentNotesLiveData?.observe(this, currentObserver!!)
    }

    private fun searchNotes(query: String) {
        currentObserver?.let { observer ->
            currentNotesLiveData?.removeObserver(observer)
        }
        
        currentNotesLiveData = noteViewModel.searchNotes(query)
        currentObserver = Observer<List<Note>> { notes ->
            noteAdapter.submitList(notes)
            
            if (notes.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "未找到匹配的备忘录"
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
        
        currentNotesLiveData?.observe(this, currentObserver!!)
    }

    private fun filterByCategory(category: String) {
        loadNotes()
    }

    private fun showSortDialog() {
        val options = arrayOf("按更新时间", "按创建时间", "按标题")
        val currentSort = preferencesHelper.getSortOrder()
        val currentIndex = when (currentSort) {
            "title_asc" -> 2
            "created_desc" -> 1
            else -> 0
        }
        
        AlertDialog.Builder(this)
            .setTitle("选择排序方式")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val sortOrder = when (which) {
                    1 -> "created_desc"
                    2 -> "title_asc"
                    else -> "updated_desc"
                }
                preferencesHelper.setSortOrder(sortOrder)
                loadNotes()
                dialog.dismiss()
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                backupData()
                true
            }
            R.id.action_restore -> {
                restoreData()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun backupData() {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("正在备份")
            .setMessage("正在备份数据，请稍候...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        backupRestoreHelper.backupData { success ->
            progressDialog.dismiss()
            
            if (success) {
                AlertDialog.Builder(this)
                    .setTitle("备份成功")
                    .setMessage("数据已成功备份到本地存储。\n备份文件保存在Documents/NoteAppBackup目录。")
                    .setPositiveButton("确定", null)
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("备份失败")
                    .setMessage("数据备份失败，可能的原因：\n• 没有数据需要备份\n• 存储空间不足\n• 权限不足")
                    .setPositiveButton("确定", null)
                    .show()
            }
        }
    }

    private fun restoreData() {
        AlertDialog.Builder(this)
            .setTitle("恢复数据")
            .setMessage("这将删除当前所有数据并从备份文件恢复，确定要继续吗？")
            .setPositiveButton("确定") { _, _ ->
                val progressDialog = AlertDialog.Builder(this)
                    .setTitle("正在恢复")
                    .setMessage("正在恢复数据，请稍候...")
                    .setCancelable(false)
                    .create()
                progressDialog.show()
                
                backupRestoreHelper.restoreData { success ->
                    progressDialog.dismiss()
                    
                    if (success) {
                        AlertDialog.Builder(this)
                            .setTitle("恢复成功")
                            .setMessage("数据已成功从备份文件恢复。")
                            .setPositiveButton("确定") { _, _ ->
                                loadNotes() // 刷新数据显示
                            }
                            .show()
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("恢复失败")
                            .setMessage("数据恢复失败，可能的原因：\n• 没有找到备份文件\n• 备份文件损坏\n• 备份文件为空")
                            .setPositiveButton("确定", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                preferencesHelper.setLoggedIn(false)
                navigateToLogin()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}