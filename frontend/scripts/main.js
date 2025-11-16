/**
 * Главный файл для инициализации приложения
 */

// Здесь можно добавить вспомогательные функции

/**
 * Форматирование времени для комментариев
 * @param {Date} date - Дата комментария
 * @returns {string} - Отформатированное время
 */
export function formatTimeAgo(date) {
  const now = new Date()
  const diffInMs = now - date
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60))
  const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60))
  const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24))

  if (diffInMinutes < 60) {
    return `${diffInMinutes} мин`
  } else if (diffInHours < 24) {
    return `${diffInHours} ч`
  } else {
    return `${diffInDays} д`
  }
}

/**
 * Валидация email
 * @param {string} email - Email для проверки
 * @returns {boolean} - Валидный ли email
 */
export function validateEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

/**
 * Валидация пароля (минимум 8 символов)
 * @param {string} password - Пароль для проверки
 * @returns {boolean} - Валидный ли пароль
 */
export function validatePassword(password) {
  return password.length >= 8
}

/**
 * Сокращение текста до указанной длины
 * @param {string} text - Исходный текст
 * @param {number} maxLength - Максимальная длина
 * @returns {string} - Сокращенный текст
 */
export function truncateText(text, maxLength) {
  if (text.length <= maxLength) {
    return text
  }
  return text.substring(0, maxLength) + "..."
}
