package com.intranet.lunchorder.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.intranet.lunchorder.R
import com.intranet.lunchorder.alarm.ReminderScheduler
import com.intranet.lunchorder.data.api.ConnectionTester
import com.intranet.lunchorder.data.prefs.PrefsStore
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.databinding.FragmentSettingsBinding
import com.intranet.lunchorder.logic.ReminderTime
import com.intranet.lunchorder.logic.ServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置：服务器地址（保存 + 连通性自检）、每日提醒开关与提醒时间。
 * 任何变更后都调用 ReminderScheduler 重新注册闹钟。
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val prefs: PrefsStore by lazy { PrefsStoreProvider.get(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.etServerUrl.setText(prefs.serverUrl)
        binding.switchNotify.isChecked = prefs.notifyEnabled
        binding.tvNotifyTime.text = prefs.notifyTime

        binding.btnSaveServer.setOnClickListener { saveServer() }
        binding.btnTestConnection.setOnClickListener { testConnection() }
        binding.btnPickTime.setOnClickListener { pickTime() }
        binding.switchNotify.setOnCheckedChangeListener { _, checked ->
            prefs.notifyEnabled = checked
            reschedule()
        }
        renderExactAlarmHint()
    }

    private fun saveServer() {
        val raw = binding.etServerUrl.text?.toString().orEmpty()
        val normalized = try {
            ServerUrl.normalize(raw)
        } catch (e: Exception) {
            toast(getString(R.string.error_server_url_invalid))
            return
        }
        if (!ServerUrl.isValidBase(normalized)) {
            toast(getString(R.string.error_server_url_invalid))
            return
        }
        prefs.serverUrl = normalized
        toast(getString(R.string.toast_server_saved))
    }

    /** 连通性自检：任意 HTTP 响应（含 401）即视为可达；IO 异常视为不可达 */
    private fun testConnection() {
        val url = binding.etServerUrl.text?.toString().orEmpty()
        binding.tvTestResult.isVisible = true
        binding.tvTestResult.text = getString(R.string.loading)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { ConnectionTester.test(url) }
            binding.tvTestResult.text = if (result.ok) {
                getString(R.string.test_connection_ok) + "（${result.detail}）"
            } else {
                getString(R.string.test_connection_fail, result.detail)
            }
        }
    }

    private fun pickTime() {
        val current = ReminderTime.parseTime(prefs.notifyTime)
        TimePickerDialog(
            requireContext(),
            { _: TimePicker, hour: Int, minute: Int ->
                val value = "%02d:%02d".format(hour, minute)
                prefs.notifyTime = value
                binding.tvNotifyTime.text = value
                reschedule()
            },
            current.hour,
            current.minute,
            true,
        ).show()
    }

    private fun reschedule() {
        val exact = ReminderScheduler.schedule(requireContext())
        if (prefs.notifyEnabled && !exact) {
            binding.tvExactAlarmHint.isVisible = true
            toast(getString(R.string.toast_exact_alarm_fallback))
        } else {
            binding.tvExactAlarmHint.isVisible = false
        }
    }

    private fun renderExactAlarmHint() {
        binding.tvExactAlarmHint.isVisible = prefs.notifyEnabled && !ReminderScheduler.canScheduleExact(requireContext())
        binding.tvExactAlarmHint.setOnClickListener {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
