/**
 * Генерирует случайный эмодзи-аватар на основе ID пользователя
 * Для одного и того же пользователя всегда будет один и тот же эмодзи
 */
export function getAvatarEmoji(userId) {
  if (!userId) {
    // Если ID нет, используем случайный
    return getRandomEmoji()
  }

  // Список эмодзи-аватаров
  const emojis = [
    '😀', '😃', '😄', '😁', '😊', '😉', '😍', '🤗', '🤔', '😇',
    '🙂', '🙃', '😋', '😛', '😜', '🤪', '😎', '🤩', '🥳', '😏',
    '😮', '🤐', '😯', '😴', '😪', '🤤', '😌', '🤓', '🧐', '🤨',
    '😐', '😑', '😶', '😒', '🙄', '😬', '🤥', '😌', '😔', '😷',
    '🤒', '🤕', '🤢', '🤮', '🤧', '🥵', '🥶', '😱', '😰', '😳',
    '🤯', '😨', '😭', '😤', '😠', '😡', '🤬', '😈', '👿', '💀',
    '☠️', '👻', '👽', '🤖', '💩', '😺', '😸', '😹', '😻', '😼',
    '😽', '🙀', '😿', '😾', '👶', '👧', '🧒', '👦', '👩', '🧑',
    '👨', '👵', '🧓', '👴', '👲', '👳', '🧕', '👮', '👷', '💂',
    '🕵️', '👩‍⚕️', '👨‍⚕️', '👩‍🌾', '👨‍🌾', '👩‍🍳', '👨‍🍳', '👩‍🎓', '👨‍🎓', '👩‍🎤',
    '👨‍🎤', '👩‍🏫', '👨‍🏫', '👩‍🏭', '👨‍🏭', '👩‍💻', '👨‍💻', '👩‍💼', '👨‍💼', '👩‍🔧',
  ]

  // Преобразуем ID в число для стабильного выбора
  const numId = typeof userId === 'number' ? userId : 
                typeof userId === 'string' ? parseInt(userId) || 0 : 0
  
  // Используем модуло для выбора эмодзи
  const index = Math.abs(numId) % emojis.length
  return emojis[index]
}

/**
 * Получает случайный эмодзи (для случаев без ID)
 */
function getRandomEmoji() {
  const emojis = [
    '😀', '😃', '😄', '😁', '😊', '😉', '😍', '🤗', '🤔', '😇',
    '🙂', '🙃', '😋', '😛', '😜', '🤪', '😎', '🤩', '🥳', '😏',
  ]
  return emojis[Math.floor(Math.random() * emojis.length)]
}

/**
 * Компонент аватара с эмодзи (для использования в JSX)
 */
export function AvatarEmoji({ userId, className = "", style = {}, size = "md" }) {
  const emoji = getAvatarEmoji(userId)
  const sizeClasses = {
    sm: { fontSize: "var(--font-size-sm)", width: "32px", height: "32px" },
    md: { fontSize: "var(--font-size-base)", width: "40px", height: "40px" },
    lg: { fontSize: "var(--font-size-lg)", width: "48px", height: "48px" },
    xl: { fontSize: "var(--font-size-xl)", width: "64px", height: "64px" },
  }

  const sizeStyle = sizeClasses[size] || sizeClasses.md

  return (
    <div
      className={`avatar avatar-${size} ${className}`}
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: "var(--bg-tertiary)",
        border: "1px solid var(--border-primary)",
        borderRadius: "50%",
        fontSize: sizeStyle.fontSize,
        width: sizeStyle.width,
        height: sizeStyle.height,
        flexShrink: 0,
        ...style,
      }}
      title={userId ? `User ${userId}` : ""}
    >
      {emoji}
    </div>
  )
}

