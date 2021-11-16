package com.frpc.android.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.afollestad.materialdialogs.MaterialDialog
import com.frpc.android.Constants
import com.frpc.android.R
import com.frpc.android.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.functions.Consumer

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
        init()
    }

    private fun init() {
        checkPermissions(Consumer { aBoolean ->
            if (!aBoolean!!) {
                Constants.tendToSettings(this@MainActivity)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_text -> actionNewText()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun actionNewText() {
        checkPermissions { aBoolean: Boolean? ->
            if (!aBoolean!!) {
                Constants.tendToSettings(this@MainActivity)
                return@checkPermissions
            }
            startActivity(Intent(this@MainActivity, IniEditActivity::class.java))
        }
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

    private fun checkPermissions(consumer: Consumer<Boolean>) {
        val subscribe = RxPermissions(this)
            .request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE
            )
            .subscribe(consumer)
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
        MaterialDialog.Builder(this)
            .title("Frp 版本")
            .content("0.33.0")
            .show()
    }
}