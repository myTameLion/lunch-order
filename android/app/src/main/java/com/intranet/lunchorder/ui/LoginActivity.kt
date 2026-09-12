package com.intranet.lunchorder.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.intranet.lunchorder.R
import com.intranet.lunchorder.data.api.ApiException
import com.intranet.lunchorder.data.api.ConnectionTester
import com.intranet.lunchorder.data.prefs.PrefsStore
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.data.repo.LunchRepository
import com.intranet.lunchorder.databinding.ActivityLoginBinding
import com.intranet.lunchorder.logic.ServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 登录页：登录名 + 密码 → POST /api/auth/login → 保存凭证 → 进主界面。
 * 401(1001) 提示”登录名或密码错误”。
 * 服务器地址（baseURL）在登录前即可配置：默认折叠，展开可修改并”测试连接”，
 * 避免默认地址不可达时”登不进也进不了设置”的死循环。
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val prefs: PrefsStore by lazy { PrefsStoreProvider.get(this) }

    private var serverSectionExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 已有 token 直接进入主界面
        if (prefs.token.isNotEmpty()) {
            goMain()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { doLogin() }
        setupServerSection()
    }

    /** 服务器地址（baseURL）配置：预填当前值，展开/收起，测试连接 */
    private fun setupServerSection() {
        binding.etServerUrl.setText(prefs.serverUrl)
        binding.tvServerToggle.setOnClickListener {
            serverSectionExpanded = !serverSectionExpanded
            binding.serverSection.isVisible = serverSectionExpanded
            binding.tvServerToggle.setText(
                if (serverSectionExpanded) R.string.login_server_toggle_hide else R.string.login_server_toggle,
            )
        }
        binding.btnTestConnection.setOnClickListener { testConnection() }
    }

    /** 把输入框里的地址规范化并落盘；非法时提示并返回 false */
    private fun persistServerUrl(): Boolean {
        val raw = binding.etServerUrl.text?.toString().orEmpty()
        val normalized = ServerUrl.normalize(raw)
        if (!ServerUrl.isValidBase(normalized)) {
            toast(getString(R.string.error_server_url_invalid))
            return false
        }
        prefs.serverUrl = normalized
        binding.etServerUrl.setText(normalized)
        return true
    }

    /** 连通性自检：任意 HTTP 响应（含 401）即可达；IO 异常不可达 */
    private fun testConnection() {
        if (!persistServerUrl()) return
        binding.tvTestResult.isVisible = true
        binding.tvTestResult.text = getString(R.string.loading)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { ConnectionTester.test(prefs.serverUrl) }
            binding.tvTestResult.text = if (result.ok) {
                getString(R.string.test_connection_ok) + "（${result.detail}）"
            } else {
                getString(R.string.test_connection_fail, result.detail)
            }
        }
    }

    private fun doLogin() {
        if (!persistServerUrl()) return
        val loginName = binding.etLoginName.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        if (loginName.isEmpty()) {
            toast(getString(R.string.error_empty_login_name))
            return
        }
        if (password.isEmpty()) {
            toast(getString(R.string.error_empty_password))
            return
        }
        binding.btnLogin.isEnabled = false
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val repo = LunchRepository.create(prefs)
                val resp = repo.login(loginName, password)
                prefs.token = resp.token
                prefs.loginName = resp.loginName
                prefs.displayName = resp.displayName
                prefs.role = resp.role
                // 登录成功后同步服务端定时通知配置（D-014），失败不阻塞登录
                try {
                    val n = repo.getNotifySettings()
                    prefs.notifyTime = n.notifyTime
                    prefs.notifyTitle = n.notifyTitle
                    prefs.notifyContent = n.notifyContent
                } catch (_: ApiException) {
                }
                goMain()
            } catch (e: ApiException) {
                val msg = if (e.code == 1001) {
                    getString(R.string.login_failed_wrong_credentials)
                } else {
                    e.message
                }
                toast(msg)
            } finally {
                binding.btnLogin.isEnabled = true
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
