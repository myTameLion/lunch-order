import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import NoticeBanner from '../NoticeBanner.vue'
import { api } from '../../api'
import { userStore } from '../../store/user'

vi.mock('../../api', () => ({ api: { getNotices: vi.fn() } }))
const mockedApi = vi.mocked(api, true)

const noticesData = {
  notices: [
    { id: 2, title: '新功能', content: '聊天频道上线啦', createdAt: '2026-09-11T10:00:00+08:00', createdBy: 'admin' },
    { id: 1, title: '系统维护', content: '周日 22:00 停机维护', createdAt: '2026-09-10T10:00:00+08:00', createdBy: 'admin' },
  ],
}

beforeEach(() => {
  userStore.loaded = true
  vi.clearAllMocks()
})

describe('NoticeBanner 重要通知（D-015）', () => {
  it('未登录时不显示、不请求', async () => {
    userStore.loaded = false
    const wrapper = mount(NoticeBanner)
    await flushPromises()
    expect(wrapper.find('[data-test="notice-banner"]').exists()).toBe(false)
    expect(mockedApi.getNotices).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('折叠时只显示最新一条，并提示可展开', async () => {
    mockedApi.getNotices.mockResolvedValue(noticesData)
    const wrapper = mount(NoticeBanner)
    await flushPromises()

    const banner = wrapper.find('[data-test="notice-banner"]')
    expect(banner.exists()).toBe(true)
    expect(banner.text()).toContain('新功能')
    expect(banner.text()).not.toContain('系统维护')
    expect(banner.text()).toContain('展开全部（2 条）')
    wrapper.unmount()
  })

  it('展开显示全部，可收起', async () => {
    mockedApi.getNotices.mockResolvedValue(noticesData)
    const wrapper = mount(NoticeBanner)
    await flushPromises()

    await wrapper.find('[data-test="notice-toggle"]').trigger('click')
    const all = wrapper.find('[data-test="notice-all"]')
    expect(all.exists()).toBe(true)
    expect(all.text()).toContain('系统维护')
    expect(wrapper.find('[data-test="notice-toggle"]').text()).toBe('收起')

    await wrapper.find('[data-test="notice-toggle"]').trigger('click')
    expect(wrapper.find('[data-test="notice-all"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('无通知时不显示横幅', async () => {
    mockedApi.getNotices.mockResolvedValue({ notices: [] })
    const wrapper = mount(NoticeBanner)
    await flushPromises()
    expect(wrapper.find('[data-test="notice-banner"]').exists()).toBe(false)
    wrapper.unmount()
  })
})
