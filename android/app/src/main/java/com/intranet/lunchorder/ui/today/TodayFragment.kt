package com.intranet.lunchorder.ui.today

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.intranet.lunchorder.R
import com.intranet.lunchorder.data.api.ApiException
import com.intranet.lunchorder.data.model.TodayAllResponse
import com.intranet.lunchorder.data.model.TodayStatus
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.data.repo.LunchRepository
import com.intranet.lunchorder.databinding.FragmentTodayBinding
import com.intranet.lunchorder.logic.TimeMath
import kotlinx.coroutines.launch

/**
 * 今日点餐：GET /api/orders/today 渲染窗口状态、倒计时、要辣/不要辣选择；
 * PUT 登记/修改、DELETE 取消（窗口关闭时控件禁用，仅展示状态）。
 * 另加载全员当日点餐情况（D-008）；聊天频道为次要入口（D-013）。
 */
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private val repo by lazy { LunchRepository.create(PrefsStoreProvider.get(requireContext())) }

    private var status: TodayStatus? = null
    private var todayAll: TodayAllResponse? = null

    /** 设备时钟 - 服务器时钟 的偏移，用于倒计时校准 */
    private var serverOffsetMillis: Long = 0L
    private var windowStartMillis: Long = 0L
    private var windowEndMillis: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            updateCountdown()
            mainHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnOrder.setOnClickListener { submitOrder() }
        binding.btnCancel.setOnClickListener { confirmCancel() }
        binding.btnRefresh.setOnClickListener { refresh() }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(tickRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainHandler.removeCallbacks(tickRunnable)
        _binding = null
    }

    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                status = repo.getToday()
                calibrate()
                render()
            } catch (e: ApiException) {
                showError(e)
            } finally {
                setLoading(false)
            }
        }
        // 全员当日情况并行加载，失败不影响个人状态展示
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                todayAll = repo.getTodayAll()
                renderTodayAll()
            } catch (e: ApiException) {
                binding.tvTodayStats.text = e.message
                binding.tvTodayNames.isVisible = false
            }
        }
    }

    /** 渲染全员当日统计（D-008） */
    private fun renderTodayAll() {
        val all = todayAll ?: return
        binding.tvTodayStats.text = getString(
            R.string.today_stats_line,
            all.total,
            all.spicy,
            all.nonSpicy,
        )
        val names = all.orders.joinToString("、") {
            "${it.displayName}（${if (it.spicy) "辣" else "不辣"}）"
        }
        if (names.isEmpty()) {
            binding.tvTodayNames.text = getString(R.string.status_not_registered)
        } else {
            binding.tvTodayNames.text = getString(R.string.today_stats_names, names)
        }
        binding.tvTodayNames.isVisible = true
    }

    private fun calibrate() {
        val s = status ?: return
        val serverNow = TimeMath.isoToMillis(s.window.serverTime)
        serverOffsetMillis = System.currentTimeMillis() - serverNow
        windowStartMillis = TimeMath.windowEdgeMillis(s.window.serverTime, s.window.start)
        windowEndMillis = TimeMath.windowEdgeMillis(s.window.serverTime, s.window.end)
    }

    private fun render() {
        val s = status ?: return
        binding.tvDate.text = getString(R.string.today_title_with_date, s.date)
        binding.tvWindow.text = getString(R.string.window_label, s.window.start, s.window.end)

        // 已登记状态（窗口关闭时仍显示）
        val myOrder = s.myOrder
        if (myOrder != null) {
            binding.tvStatus.text = getString(
                R.string.status_registered,
                getString(if (myOrder.spicy) R.string.spicy_yes else R.string.spicy_no),
            )
            binding.tvStatus.setBackgroundResource(R.drawable.bg_banner_ok)
            binding.tvStatus.setTextColor(requireContext().getColor(R.color.banner_ok_text))
        } else {
            binding.tvStatus.setText(R.string.status_not_registered)
            binding.tvStatus.setBackgroundResource(R.drawable.bg_banner_warn)
            binding.tvStatus.setTextColor(requireContext().getColor(R.color.banner_warn_text))
        }

        // 回显当前选择
        if (myOrder != null) {
            if (myOrder.spicy) binding.rbSpicy.isChecked = true else binding.rbNotSpicy.isChecked = true
        }
        binding.btnOrder.setText(
            if (myOrder != null) R.string.btn_modify_order else R.string.btn_register_order,
        )
        binding.btnCancel.isVisible = myOrder != null

        // 窗口外：横幅提示 + 控件禁用
        val open = s.window.open
        if (open) {
            binding.tvBanner.isVisible = false
        } else {
            val serverNow = System.currentTimeMillis() - serverOffsetMillis
            binding.tvBanner.text = if (serverNow < windowStartMillis) {
                getString(R.string.banner_not_started, s.window.start)
            } else {
                getString(R.string.banner_closed, s.window.end)
            }
            binding.tvBanner.isVisible = true
        }
        binding.rbSpicy.isEnabled = open
        binding.rbNotSpicy.isEnabled = open
        binding.btnOrder.isEnabled = open
        binding.btnCancel.isEnabled = open

        updateCountdown()
    }

    /** 剩余时间倒计时（按 serverTime 校准后的设备时钟计算） */
    private fun updateCountdown() {
        val s = status ?: return
        if (!isAdded || _binding == null) return
        val now = System.currentTimeMillis() - serverOffsetMillis
        binding.tvCountdown.text = when {
            s.window.open -> {
                val remaining = windowEndMillis - now
                if (remaining > 0) {
                    getString(R.string.countdown_to_end, TimeMath.formatRemaining(remaining))
                } else {
                    getString(R.string.countdown_over)
                }
            }
            now < windowStartMillis ->
                getString(R.string.countdown_to_start, TimeMath.formatRemaining(windowStartMillis - now))
            else -> getString(R.string.countdown_over)
        }
    }

    private fun submitOrder() {
        val s = status ?: return
        val spicy = when {
            binding.rbSpicy.isChecked -> true
            binding.rbNotSpicy.isChecked -> false
            else -> {
                Toast.makeText(requireContext(), R.string.error_select_spicy, Toast.LENGTH_SHORT).show()
                return
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val order = repo.putOrder(spicy)
                Toast.makeText(
                    requireContext(),
                    if (order.updated) R.string.toast_modified else R.string.toast_registered,
                    Toast.LENGTH_SHORT,
                ).show()
                refresh()
            } catch (e: ApiException) {
                showError(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun confirmCancel() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_cancel_title)
            .setMessage(R.string.dialog_cancel_message)
            .setPositiveButton(R.string.btn_confirm) { _, _ -> cancelOrder() }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun cancelOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                repo.cancelOrder()
                Toast.makeText(requireContext(), R.string.toast_cancelled, Toast.LENGTH_SHORT).show()
                refresh()
            } catch (e: ApiException) {
                showError(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        if (_binding == null) return
        binding.progress.isVisible = loading
        binding.btnOrder.isEnabled = if (loading) false else (status?.window?.open ?: true)
        binding.btnRefresh.isEnabled = !loading
    }

    /** 业务错误 {code,message} → Toast 展示 message */
    private fun showError(e: ApiException) {
        if (!isAdded) return
        val msg = if (e.isNetworkError) {
            getString(R.string.error_network, e.message)
        } else {
            e.message
        }
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
