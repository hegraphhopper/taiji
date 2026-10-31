/**
 * 仅允许站内绝对路径作为登录后的跳转目标，避免重定向到外站或回到登录/无权限页造成循环。
 */
export function safeRedirect(target: unknown, fallback = '/dashboard'): string {
  if (typeof target !== 'string' || target === '') {
    return fallback
  }
  if (!target.startsWith('/') || target.startsWith('//') || target.startsWith('/\\')) {
    return fallback
  }
  if (target === '/login' || target.startsWith('/login?') || target.startsWith('/403')) {
    return fallback
  }
  return target
}
