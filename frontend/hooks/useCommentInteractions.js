"use client"

import { useState, useEffect } from "react"
import { commentAPI } from "../lib/api"
import { useUser } from "../context/UserContext"

/**
 * Хук для управления лайками и дизлайками комментария
 * Сохраняет состояние на сервере
 */
export function useCommentInteractions(comment, postId) {
  const { currentUser } = useUser()
  const [likes, setLikes] = useState(comment?.likedUsers?.length || comment?.likes || 0)
  const [isLiked, setIsLiked] = useState(false)
  const [loading, setLoading] = useState(false)

  // Инициализация состояния на основе данных комментария
  useEffect(() => {
    if (comment) {
      const likedCount = comment.likedUsers?.length || comment.likes || 0
      setLikes(likedCount)

      // Проверяем, лайкнул ли текущий пользователь
      if (currentUser && comment.likedUsers) {
        const userLiked = comment.likedUsers.some((u) => (u.id || u) === currentUser.id)
        setIsLiked(userLiked)
      }
    }
  }, [comment, currentUser])

  const handleLike = async () => {
    if (!currentUser || !comment?.id || !postId || loading) return

    const wasLiked = isLiked

    setLoading(true)

    try {
      // Вызов API для лайка комментария
      await commentAPI.like(postId, comment.id, currentUser.id)

      // Оптимистичное обновление UI
      if (wasLiked) {
        setLikes((prev) => Math.max(0, prev - 1))
        setIsLiked(false)
      } else {
        setLikes((prev) => prev + 1)
        setIsLiked(true)
      }
    } catch (error) {
      console.error("Ошибка при лайке комментария:", error)
      // Откатываем изменения при ошибке
      setLikes(comment?.likedUsers?.length || comment?.likes || 0)
      setIsLiked(wasLiked)
    } finally {
      setLoading(false)
    }
  }

  return {
    likes,
    isLiked,
    loading,
    handleLike,
  }
}
