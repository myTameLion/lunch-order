import { describe, expect, it } from 'vitest'
import { fallbackExportName, parseFilenameFromDisposition } from '@/utils/download'

describe('Content-Disposition 文件名解析', () => {
  it('优先 RFC 5987 filename* 并解码中文', () => {
    const header = `attachment; filename="lunch_1_2.xlsx"; filename*=UTF-8''${encodeURIComponent('点餐统计_1_2.xlsx')}`
    expect(parseFilenameFromDisposition(header)).toBe('点餐统计_1_2.xlsx')
  })

  it('无 filename* 时回退 filename="..."', () => {
    expect(parseFilenameFromDisposition('attachment; filename="lunch.xlsx"')).toBe('lunch.xlsx')
    expect(parseFilenameFromDisposition('attachment; filename=lunch.xlsx')).toBe('lunch.xlsx')
  })

  it('null / 空头返回 null', () => {
    expect(parseFilenameFromDisposition(null)).toBeNull()
    expect(parseFilenameFromDisposition('')).toBeNull()
  })

  it('非法编码返回 null（调用方回退默认文件名）', () => {
    expect(parseFilenameFromDisposition("attachment; filename*=UTF-8''%zz")).toBeNull()
  })
})

describe('fallbackExportName', () => {
  it('兜底名格式', () => {
    expect(fallbackExportName('2026-09-01', '2026-09-07')).toBe('点餐统计_2026-09-01_2026-09-07.xlsx')
  })
})
