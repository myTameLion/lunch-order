package com.intranet.lunchorder.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.intranet.lunchorder.R
import com.intranet.lunchorder.data.api.ApiException
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.data.repo.LunchRepository
import com.intranet.lunchorder.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

/**
 * 公共聊天频道（D-013）：客户端仅展示当天消息；
 * 从今日点餐页次要入口进入，点餐功能仍是主功能。
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val repo by lazy { LunchRepository.create(PrefsStoreProvider.get(this)) }
    private val adapter = ChatAdapter()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            refresh()
            mainHandler.postDelayed(this, 10_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter
        binding.btnSend.setOnClickListener { send() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        mainHandler.postDelayed(pollRunnable, 10_000L)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(pollRunnable)
    }

    private fun refresh() {
        lifecycleScope.launch {
            try {
                val resp = repo.getTodayChat()
                binding.tvChatDate.text = getString(R.string.chat_title) + " · " + resp.date
                adapter.submitList(resp.messages)
                binding.rvMessages.scrollToPosition((resp.messages.size - 1).coerceAtLeast(0))
            } catch (e: ApiException) {
                toast(e.message)
            }
        }
    }

    private fun send() {
        val content = binding.etChatInput.text?.toString()?.trim().orEmpty()
        if (content.isEmpty()) {
            toast(getString(R.string.toast_chat_empty))
            return
        }
        binding.btnSend.isEnabled = false
        lifecycleScope.launch {
            try {
                repo.sendChat(content)
                binding.etChatInput.setText("")
                toast(getString(R.string.toast_chat_sent))
                refresh()
            } catch (e: ApiException) {
                toast(e.message)
            } finally {
                binding.btnSend.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
