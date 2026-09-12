/**
 * Excel 导出下载工具。
 * 文件名优先取响应 Content-Disposition（RFC 5987 filename* 解码），兜底 `点餐统计_{from}_{to}.xlsx`。
 */

const FILENAME_UTF8_RE = /filename\*\s*=\s*(?:UTF-8|utf-8)''([^;\s]+)/
const FILENAME_PLAIN_RE = /filename\s*=\s*"?([^";]+)"?/

/**
 * 从 Content-Disposition 头解析文件名。
 * - 优先 filename*=UTF-8''<percent-encoded>（RFC 5987）；
 * - 否则回退 filename="xxx" / filename=xxx；
 * - 解析失败返回 null。
 */
export function parseFilenameFromDisposition(header?: string | null): string | null {
  if (!header) return null
  const utf8Match = FILENAME_UTF8_RE.exec(header)
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      // 解码失败则回退到 filename=
    }
  }
  const plainMatch = FILENAME_PLAIN_RE.exec(header)
  if (plainMatch) {
    const name = plainMatch[1].trim()
    try {
      return decodeURIComponent(name)
    } catch {
      return name
    }
  }
  return null
}

/** 触发浏览器下载（Blob → ObjectURL → a[download]） */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  // 立即回收可能造成 Safari 下载中断，延迟一帧释放
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** 导出 Excel 的默认兜底文件名 */
export function fallbackExportName(from: string, to: string): string {
  return `点餐统计_${from}_${to}.xlsx`
}
