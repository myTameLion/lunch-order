package com.intranet.lunchorder.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.intranet.lunchorder.R
import com.intranet.lunchorder.alarm.ReminderScheduler
import com.intranet.lunchorder.data.api.ApiException
import com.intranet.lunchorder.data.api.AuthEvents
import com.intranet.lunchorder.data.model.NoticeItem
import com.intranet.lunchorder.data.prefs.PrefsStore
import com.intranet.lunchorder.data.repo.LunchRepository
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.databinding.ActivityMainBinding
import com.intranet.lunchorder.ui.mine.MineFragment
import com.intranet.lunchorder.ui.settings.SettingsFragment
import com.intranet.lunchorder.ui.today.TodayFragment
import kotlinx.coroutines.launch

/**
 * 主界面：顶部显示姓名与重要通知横幅（折叠显示最新一条，可展开全部，D-015），
 * 底部三 Tab（今日点餐 / 我的 / 设置）。收到全局 401 登出事件后跳回登录页。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs: PrefsStore by lazy { PrefsStoreProvider.get(this) }

    /** 重要通知（D-015） */
    private var notices: List<NoticeItem> = emptyList()
    private var noticeExpanded = false

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, R.string.notification_permission_hint, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefs.token.isEmpty()) {
            goLogin()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderHeader()

        binding.bottomNav.setOnItemSelectedListener { item ->
            switchTo(item.itemId)
        }
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_today
        } else {
            // 进程重建后恢复选中项（Fragment 由系统自动恢复）
            renderHeader()
        }

        // 全局 401 → 跳登录页
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AuthEvents.logout.collect {
                    goLogin()
                }
            }
        }

        // 通知权限（API 33+ 运行时请求）
        requestNotificationPermissionIfNeeded()

        // 重要通知（D-015）：顶部突出展示，折叠显示最新一条
        binding.tvNoticeToggle.setOnClickListener {
            noticeExpanded = !noticeExpanded
            renderNotices()
        }
        loadNotices()

        // App 启动后同步服务端定时通知配置（D-014）并重新注册每日提醒
        lifecycleScope.launch {
            try {
                val n = LunchRepository.create(prefs).getNotifySettings()
                prefs.notifyTime = n.notifyTime
                prefs.notifyTitle = n.notifyTitle
                prefs.notifyContent = n.notifyContent
            } catch (_: ApiException) {
                /* 同步失败保持本地配置 */
            }
            ReminderScheduler.schedule(this@MainActivity)
        }
    }

    /** 拉取重要通知（失败静默，不打扰主流程） */
    private fun loadNotices() {
        lifecycleScope.launch {
            try {
                notices = LunchRepository.create(prefs).getNotices().notices
                renderNotices()
            } catch (_: ApiException) {
                binding.noticeCard.isVisible = false
            }
        }
    }

    /** 折叠：只显示最新一条；展开：显示全部（最新在前） */
    private fun renderNotices() {
        val latest = notices.firstOrNull()
        if (latest == null) {
            binding.noticeCard.isVisible = false
            return
        }
        binding.noticeCard.isVisible = true
        binding.tvNoticeTitle.text = latest.title
        binding.tvNoticeLatest.text = latest.content
        if (noticeExpanded) {
            binding.tvNoticeToggle.text = getString(R.string.notice_collapse)
            binding.tvNoticeAll.isVisible = true
            binding.tvNoticeAll.text = notices.joinToString("\n\n") { n ->
                "📢 ${n.title}\n${n.content}\n—— ${n.createdAt.take(10)} · ${n.createdBy}"
            }
        } else {
            binding.tvNoticeToggle.text = getString(R.string.notice_expand_all, notices.size)
            binding.tvNoticeAll.isVisible = false
        }
    }

    private fun switchTo(itemId: Int): Boolean {
        val fragment: Fragment = when (itemId) {
            R.id.nav_today -> TodayFragment()
            R.id.nav_mine -> MineFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> return false
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }

    private fun renderHeader() {
        val name = prefs.displayName.ifEmpty { prefs.loginName }
        binding.tvGreeting.text = getString(R.string.greeting, name)
        binding.tvRole.text = getString(
            if (prefs.role == "ADMIN") R.string.role_admin else R.string.role_user,
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun goLogin() {
        val intent = Intent(this, LoginActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}
