package com.intranet.lunchorder.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.intranet.lunchorder.data.model.ChatMessage
import com.intranet.lunchorder.databinding.ItemChatMessageMineBinding
import com.intranet.lunchorder.databinding.ItemChatMessageTheirsBinding

/**
 * 聊天消息适配器（D-018）：微信式布局——自己的消息右侧蓝底气泡，他人消息左侧灰底气泡。
 */
class ChatAdapter(private val myLoginName: String) :
    ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).loginName == myLoginName) TYPE_MINE else TYPE_THEIRS

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_MINE) {
            ViewHolderMine(ItemChatMessageMineBinding.inflate(inflater, parent, false))
        } else {
            ViewHolderTheirs(ItemChatMessageTheirsBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val time = message.sentAt.substringAfter('T').take(5)
        when (holder) {
            is ViewHolderMine -> {
                holder.binding.tvContent.text = message.content
                holder.binding.tvTime.text = time
            }
            is ViewHolderTheirs -> {
                holder.binding.tvContent.text = message.content
                holder.binding.tvName.text = message.displayName
                holder.binding.tvTime.text = time
            }
        }
    }

    class ViewHolderMine(val binding: ItemChatMessageMineBinding) : RecyclerView.ViewHolder(binding.root)
    class ViewHolderTheirs(val binding: ItemChatMessageTheirsBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_MINE = 1
        private const val TYPE_THEIRS = 2

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem.loginName == newItem.loginName && oldItem.sentAt == newItem.sentAt

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
        }
    }
}
