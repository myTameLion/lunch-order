import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import OrderView from '../OrderView.vue'
import { api } from '../../api'

vi.mock('../../api', () => ({
  api: {
    getToday: vi.fn(),
    getTodayAll: vi.fn(),
    getTodayChat: vi.fn(),
    sendChat: vi.fn(),
    putOrder: vi.fn(),
    cancelOrder: vi.fn(),
  },
}))

const mockedApi = vi.mocked(api, true)

/** 构造"本地时区某时刻"的 ISO 串，保证测试与机器时区无关 */
function isoAtLocal(h: number, m = 0): string {
  const d = new Date(2026, 8, 7, h, m)
  const off = -d.getTimezoneOffset()
  const sign = off >= 0 ? '+' : '-'
  const p = (n: number) => String(n).padStart(2, '0')
  return `2026-09-07T${p(h)}:${p(m)}:00${sign}${p(Math.floor(Math.abs(off) / 60))}:${p(Math.abs(off) % 60)}`
}

const todayAllData = {
  date: '2026-09-07',
  total: 3,
  spicy: 2,
  nonSpicy: 1,
  orders: [
    { loginName: 'zhangsan', displayName: '张三', spicy: true, orderedAt: '2026-09-07T14:23:05+08:00' },
    { loginName: 'lisi', displayName: '李四', spicy: false, orderedAt: '2026-09-07T15:07:41+08:00' },
    { loginName: 'wangwu', displayName: '王五', spicy: true, orderedAt: '2026-09-07T16:35:12+08:00' },
  ],
}

function mountView() {
  return mount(OrderView, { global: { plugins: [ElementPlus] } })
}

describe('OrderView 今日点餐', () => {
  it('窗口外：禁用表单、展示已截止提示与已登记回显', async () => {
    mockedApi.getToday.mockResolvedValue({
      date: '2026-09-07',
      window: { start: '14:00', end: '18:00', open: false, serverTime: isoAtLocal(19) },
      myOrder: { spicy: true, orderedAt: '2026-09-07T14:23:05+08:00' },
    })
    mockedApi.getTodayAll.mockResolvedValue(todayAllData)
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('已截止')
    expect(wrapper.text()).toContain('要辣')
    const save = wrapper.find('[data-test="save"]')
    expect((save.element as HTMLButtonElement).disabled).toBe(true)
    wrapper.unmount()
  })

  it('窗口内：可编辑、显示倒计时与未登记状态', async () => {
    mockedApi.getToday.mockResolvedValue({
      date: '2026-09-07',
      window: { start: '14:00', end: '18:00', open: true, serverTime: isoAtLocal(15) },
      myOrder: null,
    })
    mockedApi.getTodayAll.mockResolvedValue(todayAllData)
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('登记进行中')
    const remain = wrapper.find('[data-test="remain"]')
    expect(remain.text()).toMatch(/^\d{2,3}:\d{2}$/)
    expect(wrapper.text()).toContain('今日还未登记')
    expect((wrapper.find('[data-test="save"]').element as HTMLButtonElement).disabled).toBe(false)
    wrapper.unmount()
  })

  it('实时面板：全员可见当天点餐情况（D-008）', async () => {
    mockedApi.getToday.mockResolvedValue({
      date: '2026-09-07',
      window: { start: '14:00', end: '18:00', open: true, serverTime: isoAtLocal(15) },
      myOrder: null,
    })
    mockedApi.getTodayAll.mockResolvedValue(todayAllData)
    mockedApi.getTodayChat.mockResolvedValue({ date: '2026-09-07', count: 0, messages: [] })
    const wrapper = mountView()
    await flushPromises()

    expect(mockedApi.getTodayAll).toHaveBeenCalled()
    const panel = wrapper.find('[data-test="today-all"]')
    expect(panel.exists()).toBe(true)
    expect(panel.text()).toContain('已订 3 份')
    expect(panel.text()).toContain('要辣 2 份')
    expect(panel.text()).toContain('不要辣 1 份')
    // 名单包含全部三人
    for (const name of ['张三', '李四', '王五']) {
      expect(panel.text()).toContain(name)
    }
    wrapper.unmount()
  })

  it('聊天频道（D-013）：展示当天消息并可发送', async () => {
    mockedApi.getToday.mockResolvedValue({
      date: '2026-09-07',
      window: { start: '14:00', end: '18:00', open: true, serverTime: isoAtLocal(15) },
      myOrder: null,
    })
    mockedApi.getTodayChat.mockResolvedValue({
      date: '2026-09-07',
      count: 2,
      messages: [
        { loginName: 'zhangsan', displayName: '张三', content: '今天吃什么', sentAt: '2026-09-07T15:01:00+08:00' },
        { loginName: 'lisi', displayName: '李四', content: '吃食堂', sentAt: '2026-09-07T15:05:00+08:00' },
      ],
    })
    mockedApi.sendChat.mockResolvedValue({ loginName: 'zhangsan', displayName: '张三', content: '好主意', sentAt: '2026-09-07T15:06:00+08:00' })
    const wrapper = mountView()
    await flushPromises()

    const panel = wrapper.find('[data-test="chat"]')
    expect(panel.exists()).toBe(true)
    expect(panel.text()).toContain('今天吃什么')
    expect(panel.text()).toContain('吃食堂')

    // 输入并发送
    await wrapper.find('[data-test="chat-input"]').setValue('好主意')
    await wrapper.find('[data-test="chat-send"]').trigger('click')
    await flushPromises()
    expect(mockedApi.sendChat).toHaveBeenCalledWith('好主意')
    wrapper.unmount()
  })

  it('聊天频道（D-013）：空消息不调用发送接口', async () => {
    mockedApi.getToday.mockResolvedValue({
      date: '2026-09-07',
      window: { start: '14:00', end: '18:00', open: true, serverTime: isoAtLocal(15) },
      myOrder: null,
    })
    mockedApi.getTodayChat.mockResolvedValue({ date: '2026-09-07', count: 0, messages: [] })
    mockedApi.sendChat.mockResolvedValue({})
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-test="chat"]').text()).toContain('今天还没有人发言')
    mockedApi.sendChat.mockClear()
    await wrapper.find('[data-test="chat-send"]').trigger('click')
    await flushPromises()
    expect(mockedApi.sendChat).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('实时面板：无人点餐时显示空态', async () => {
    mockedApi.getToday.mockResolvedValue({
      date: '2026-09-07',
      window: { start: '14:00', end: '18:00', open: true, serverTime: isoAtLocal(15) },
      myOrder: null,
    })
    mockedApi.getTodayAll.mockResolvedValue({ date: '2026-09-07', total: 0, spicy: 0, nonSpicy: 0, orders: [] })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-test="today-all"]').text()).toContain('今天还没有人点餐')
    wrapper.unmount()
  })
})
