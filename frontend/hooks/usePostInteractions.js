"use client"

import { useState, useEffect } from "react"
import { postAPI } from "../lib/api"
import { useUser } from "../context/UserContext"

/**
 * Хук для управления лайками и дизлайками поста
 * Сохраняет состояние на сервере
 */
export function usePostInteractions(post) {
  const { currentUser } = useUser()
  const [likes, setLikes] = useState(post?.likedUsers?.length || post?.likes || 0)
  const [dislikes, setDislikes] = useState(post?.dislikedUsers?.length || post?.dislikes || 0)
  const [isLiked, setIsLiked] = useState(false)
  const [isDisliked, setIsDisliked] = useState(false)
  const [loading, setLoading] = useState(false)

  // Инициализация состояния на основе данных поста
  useEffect(() => {
    if (post) {
      const likedCount = post.likedUsers?.length || post.likes || 0
      const dislikedCount = post.dislikedUsers?.length || post.dislikes || 0
      setLikes(likedCount)
      setDislikes(dislikedCount)

      // Проверяем, лайкнул ли текущий пользователь
      if (currentUser && post.likedUsers) {
        const userLiked = post.likedUsers.some((u) => (u.id || u) === currentUser.id)
        const userDisliked = post.dislikedUsers?.some((u) => (u.id || u) === currentUser.id) || false
        setIsLiked(userLiked)
        setIsDisliked(userDisliked)
      }
    }
  }, [post, currentUser])

  const handleLike = async () => {
    if (!currentUser || !post?.id || loading) return

    const wasLiked = isLiked
    const wasDisliked = isDisliked

    setLoading(true)

    try {
      // Вызов API для лайка
      await postAPI.like(currentUser.id, post.id)

      // Оптимистичное обновление UI
      if (wasLiked) {
        setLikes((prev) => Math.max(0, prev - 1))
        setIsLiked(false)
      } else {
        setLikes((prev) => prev + 1)
        setIsLiked(true)

        if (wasDisliked) {
          setDislikes((prev) => Math.max(0, prev - 1))
          setIsDisliked(false)
        }
      }
    } catch (error) {
      console.error("Ошибка при лайке поста:", error)
      // Откатываем изменения при ошибке
      setLikes(post?.likedUsers?.length || post?.likes || 0)
      setDislikes(post?.dislikedUsers?.length || post?.dislikes || 0)
      setIsLiked(wasLiked)
      setIsDisliked(wasDisliked)
    } finally {
      setLoading(false)
    }
  }

  const handleDislike = async () => {
    if (!currentUser || !post?.id || loading) return

    const wasDisliked = isDisliked
    const wasLiked = isLiked

    setLoading(true)

    try {
      // Вызов API для дизлайка
      await postAPI.dislike(currentUser.id, post.id)

      // Оптимистичное обновление UI
      if (wasDisliked) {
        setDislikes((prev) => Math.max(0, prev - 1))
        setIsDisliked(false)
      } else {
        setDislikes((prev) => prev + 1)
        setIsDisliked(true)

        if (wasLiked) {
          setLikes((prev) => Math.max(0, prev - 1))
          setIsLiked(false)
        }
      }
    } catch (error) {
      console.error("Ошибка при дизлайке поста:", error)
      // Откатываем изменения при ошибке
      setLikes(post?.likedUsers?.length || post?.likes || 0)
      setDislikes(post?.dislikedUsers?.length || post?.dislikes || 0)
      setIsLiked(wasLiked)
      setIsDisliked(wasDisliked)
    } finally {
      setLoading(false)
    }
  }

  return {
    likes,
    dislikes,
    isLiked,
    isDisliked,
    loading,
    handleLike,
    handleDislike,
  }
}
