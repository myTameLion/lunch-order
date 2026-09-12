package com.intranet.lunchorder.ui.mine

import android.os.Bundle
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
import com.intranet.lunchorder.data.prefs.PrefsStore
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.data.repo.LunchRepository
import com.intranet.lunchorder.databinding.FragmentMineBinding
import kotlinx.coroutines.launch

/**
 * 我的：登录名只读；修改姓名（PUT /api/me/profile）；
 * 修改密码（PUT /api/me/password，成功后旧 token 失效并保存新 token，D-004）；退出登录。
 */
class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!
    private val prefs: PrefsStore by lazy { PrefsStoreProvider.get(requireContext()) }
    private val repo by lazy { LunchRepository.create(prefs) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        renderAccount()
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnChangePassword.setOnClickListener { changePassword() }
        binding.btnHistory.setOnClickListener { showHistory() }
        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    /** 我的历史点餐（D-011）：服务端缺省返回最近 30 天 */
    private fun showHistory() {
        setBusy(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = repo.getMyOrders(null, null)
                setBusy(false)
                val sb = StringBuilder()
                sb.append(getString(R.string.today_stats_line, resp.totalDays, resp.spicyDays, resp.nonSpicyDays))
                if (resp.records.isEmpty()) {
                    sb.append("\n\n").append(getString(R.string.history_empty))
                } else {
                    sb.append('\n')
                    for (r in resp.records) {
                        val time = r.orderedAt.substringAfter('T').take(5)
                        sb.append('\n').append(r.date).append("  ")
                            .append(if (r.spicy) "🌶️ 要辣" else "不辣").append("（").append(time).append(" 登记）")
                    }
                }
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.history_title)
                    .setMessage(sb.toString())
                    .setPositiveButton(R.string.btn_confirm, null)
                    .show()
            } catch (e: ApiException) {
                setBusy(false)
                toast(e.message)
            }
        }
    }

    private fun renderAccount() {
        binding.etLoginName.setText(prefs.loginName)
        binding.etLoginName.isEnabled = false
        binding.etDisplayName.setText(prefs.displayName)
    }

    private fun saveProfile() {
        val name = binding.etDisplayName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            toast(getString(R.string.error_empty_display_name))
            return
        }
        setBusy(true)
        lifecycleScope.launch {
            try {
                val updated = repo.updateProfile(name)
                prefs.displayName = updated.displayName
                toast(getString(R.string.toast_profile_saved))
            } catch (e: ApiException) {
                toast(e.message)
            } finally {
                setBusy(false)
            }
        }
    }

    private fun changePassword() {
        val old = binding.etOldPassword.text?.toString().orEmpty()
        val next = binding.etNewPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()
        when {
            old.isEmpty() -> { toast(getString(R.string.error_empty_old_password)); return }
            next.length < 6 -> { toast(getString(R.string.error_password_too_short)); return }
            next != confirm -> { toast(getString(R.string.error_password_confirm_mismatch)); return }
        }
        setBusy(true)
        lifecycleScope.launch {
            try {
                repo.changePassword(old, next)
                toast(getString(R.string.toast_password_changed))
                binding.etOldPassword.setText("")
                binding.etNewPassword.setText("")
                binding.etConfirmPassword.setText("")
            } catch (e: ApiException) {
                toast(e.message)
            } finally {
                setBusy(false)
            }
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_logout_title)
            .setMessage(R.string.dialog_logout_message)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                prefs.clearSession()
                requireActivity().recreate()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun setBusy(busy: Boolean) {
        binding.btnSaveProfile.isEnabled = !busy
        binding.btnChangePassword.isEnabled = !busy
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
