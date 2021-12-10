package com.frpc.android.ui

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.afollestad.materialdialogs.MaterialDialog
import com.frpc.android.R
import com.frpc.android.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mAppBarConfiguration: AppBarConfiguration? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        mAppBarConfiguration = AppBarConfiguration.Builder(
            R.id.nav_home
        )
            .setOpenableLayout(binding.drawerLayout)
            .build()
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration!!)
        NavigationUI.setupWithNavController(binding.navView, navController)
        binding.navView.setNavigationItemSelectedListener(this)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_text -> {
                checkPermissions {
                    val intent = Intent(this@MainActivity, IniEditActivity::class.java)
                    intent.putExtra("isOverwrite", false)
                    startActivity(intent)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        return (NavigationUI.navigateUp(navController, mAppBarConfiguration!!)
                || super.onSupportNavigateUp())
    }

    private fun checkPermissions(successful: () -> Unit) {
        PermissionX.init(this)
            .permissions(
                READ_EXTERNAL_STORAGE,
                WRITE_EXTERNAL_STORAGE
            )
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, "核心基础都是基于这些权限", "确定", "取消")
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(deniedList, "您需要在“设置”中手动授予必要的权限", "确定", "取消")
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    successful.invoke()
                } else {
                    Toast.makeText(this, "这些权限被拒绝: $deniedList", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logcat -> {
                startActivity(Intent(this, LogcatActivity::class.java))
                return true
            }
            R.id.about -> {
                showAbout()
                binding.drawerLayout.close()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAbout() {
        MaterialDialog(this).show {
            title(text = "Frp 版本")
            message(text = "0.33.0")
        }
    }
}