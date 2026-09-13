/**
 * 聊天窗口滚动策略（F-CHAT-03/04）：
 * - 视口处于底部附近（距底 ≤ threshold 像素，灵活容忍）→ 新消息自动跟随滚动；
 * - 用户正在向上翻看历史 → 不自动滚动，避免打断。
 */
export function isNearBottom(
  scrollTop: number,
  scrollHeight: number,
  clientHeight: number,
  threshold = 40,
): boolean {
  if (clientHeight <= 0 || scrollHeight <= 0) return true
  return scrollHeight - scrollTop - clientHeight <= threshold
}
