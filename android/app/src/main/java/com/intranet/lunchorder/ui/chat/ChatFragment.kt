package com.intranet.lunchorder.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.intranet.lunchorder.R
import com.intranet.lunchorder.data.api.ApiException
import com.intranet.lunchorder.data.model.ChatMessage
import com.intranet.lunchorder.data.prefs.PrefsStore
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.data.repo.LunchRepository
import com.intranet.lunchorder.data.ws.ChatSocketClient
import com.intranet.lunchorder.databinding.FragmentChatBinding
import kotlinx.coroutines.launch

/**
 * 聊天频道（D-013/D-018）：底部导航独立 Tab，WebSocket 实时收发，
 * 微信式气泡布局（自己右侧 / 他人左侧）；进入页面时 REST 拉取当天历史兜底。
 */
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val prefs: PrefsStore by lazy { PrefsStoreProvider.get(requireContext()) }
    private val repo by lazy { LunchRepository.create(prefs) }

    private lateinit var adapter: ChatAdapter
    private var socket: ChatSocketClient? = null
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ChatAdapter(prefs.loginName)
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = adapter
        binding.btnSend.setOnClickListener { send() }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
        connectSocket()
    }

    override fun onPause() {
        super.onPause()
        socket?.close()
        socket = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = repo.getTodayChat()
                if (_binding == null) return@launch
                messages.clear()
                messages.addAll(resp.messages)
                renderMessages()
                binding.tvChatDate.text = getString(R.string.chat_title) + " · " + resp.date
            } catch (e: ApiException) {
                toast(e.message)
            }
        }
    }

    private fun connectSocket() {
        socket?.close()
        socket = ChatSocketClient(prefs.serverUrl, prefs.token, object : ChatSocketClient.Listener {
            override fun onChatMessage(message: ChatMessage) {
                if (_binding == null) return
                requireActivity().runOnUiThread {
                    if (messages.none { it.loginName == message.loginName && it.sentAt == message.sentAt }) {
                        messages.add(message)
                        renderMessages()
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
            }

            override fun onError(message: String) {
                if (_binding == null) return
                requireActivity().runOnUiThread { toast(message) }
            }

            override fun onClosedTemporarily() {
                /* ChatSocketClient 自动重连 */
            }
        })
        socket?.connect()
    }

    private fun send() {
        val content = binding.etChatInput.text?.toString()?.trim().orEmpty()
        if (content.isEmpty()) {
            toast(getString(R.string.toast_chat_empty))
            return
        }
        if (socket?.send(content) == true) {
            binding.etChatInput.setText("")
            /* 消息经 WS 广播回来后统一追加 */
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val saved = repo.sendChat(content)
                    binding.etChatInput.setText("")
                    if (messages.none { it.sentAt == saved.sentAt }) {
                        messages.add(saved)
                        renderMessages()
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                } catch (e: ApiException) {
                    toast(e.message)
                }
            }
        }
    }

    private fun renderMessages() {
        if (_binding == null) return
        adapter.submitList(messages.toList())
        binding.rvMessages.scrollToPosition((messages.size - 1).coerceAtLeast(0))
        binding.tvChatEmpty.isVisible = messages.isEmpty()
    }

    private fun toast(msg: String) {
        if (_binding == null) return
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
