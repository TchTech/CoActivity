"use client"

import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { commentAPI, postAPI, imageAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { usePostInteractions } from "../hooks/usePostInteractions"
import { useCommentInteractions } from "../hooks/useCommentInteractions"
import { ConfirmDeleteDialog } from "../components/ConfirmDeleteDialog"
import { getAvatarEmoji } from "../utils/avatarUtils"
import { AlertDialog } from "../components/ui/AlertDialog"
import { ConfirmDialog } from "../components/ui/ConfirmDialog"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/post.css"
import "../styles/navigation.css"

function Comments({ onNavigate, postId, currentPage }) {
  const { currentUser } = useUser()
  const [postData, setPostData] = useState(null)
  const [comments, setComments] = useState([])
  const [postComments, setPostComments] = useState(0)
  const [commentText, setCommentText] = useState("")
  const [loading, setLoading] = useState(true)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isDeleted, setIsDeleted] = useState(false)
  const [showAlert, setShowAlert] = useState(false)
  const [alertData, setAlertData] = useState({ title: "", message: "", variant: "info" })
  const [showDeleteCommentConfirm, setShowDeleteCommentConfirm] = useState(false)
  const [commentToDelete, setCommentToDelete] = useState(null)
  
  // Hook for post interactions (likes/dislikes)
  const postInteractions = usePostInteractions(postData)

  // Function to load only comments (for real-time updates)
  const fetchComments = async () => {
    try {
      const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
      if (!postIdValue) {
        return
      }
      
      console.log("[Comments] Loading comments for post:", postIdValue)
      const apiComments = await commentAPI.getByPost(postIdValue)
      console.log("[Comments] Loaded comments from API:", apiComments)
      
      // Map and sort comments by creation date (oldest first for chronological order)
      const sortedComments = Array.isArray(apiComments) && apiComments.length > 0
        ? apiComments.map(c => {
            const authorId = c.author?.id || c.authorId
            const authorName = c.author?.name || c.author?.username || "Пользователь"
            
            return {
              id: c.id,
              userId: authorId,
              author: {
                id: authorId,
                name: authorName,
                username: c.author?.username,
                avatar: c.author?.avatar,
                rating: c.author?.rating,
              },
              text: c.text || c.content || "",
              createdAt: c.createdAt || c.created_at,
              time: "",
              likes: c.likedUsers?.length || 0,
              likedUsers: c.likedUsers || [],
              dislikedUsers: c.dislikedUsers || [],
              dislikes: c.dislikedUsers?.length || 0,
              isLiked: false,
              isDisliked: false,
            }
          }).sort((a, b) => {
            const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
            const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
            return dateA - dateB
          })
        : []
      
      console.log("[Comments] Mapped and sorted comments:", sortedComments.length)
      setComments(sortedComments)
      setPostComments(sortedComments.length)
    } catch (error) {
      console.error("Ошибка загрузки комментариев:", error)
    }
  }

  const fetchPostData = async () => {
    setLoading(true)
    try {
      // Ensure postId is a primitive value
      const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
      if (!postIdValue) {
        console.error("[Comments] Invalid postId:", postId)
        setLoading(false)
        return
      }
      
      console.log("[Comments] Loading post with ID:", postIdValue)
      // Load post from backend
      try {
        const post = await postAPI.getById(postIdValue)
        console.log("[Comments] Loaded post:", post)
        setPostData(post)
      } catch (apiError) {
        // Если пост не найден, выбрасываем ошибку с статусом 404
        if (apiError.status === 404 || (apiError.message && apiError.message.includes("404"))) {
          const notFoundError = new Error("Post not found")
          notFoundError.status = 404
          throw notFoundError
        }
        throw apiError
      }
      
      // Load comments from backend
      await fetchComments()
    } catch (error) {
      console.error("Ошибка загрузки поста:", error)
      // Если пост не найден (404), редиректим на главную страницу
      if (error.status === 404 || (error.message && error.message.includes("404"))) {
        console.warn("Пост не найден, перенаправление на главную страницу")
        setPostData(null)
        setComments([])
        setPostComments(0)
        // Небольшая задержка перед редиректом, чтобы пользователь увидел, что что-то происходит
        setTimeout(() => {
          onNavigate("home")
        }, 1000)
      } else {
        // Для других ошибок просто очищаем данные
        setPostData(null)
        setComments([])
        setPostComments(0)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    // Не загружаем данные, если пост был удален
    if (isDeleted) {
      return
    }
    
    // Ensure postId is a primitive value
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    console.log("[Comments] useEffect triggered, postId:", postId, "postIdValue:", postIdValue)
    if (postIdValue) {
      fetchPostData()
    } else {
      console.warn("[Comments] No valid postId provided")
      setLoading(false)
    }
  }, [postId, isDeleted])

  // Real-time comments update (polling)
  useEffect(() => {
    // Не обновляем комментарии, если пост был удален или еще загружается
    if (isDeleted || loading || !postData) {
      return
    }
    
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    if (!postIdValue) {
      return
    }

    // Initial load
    fetchComments()
    
    // Poll every 5 seconds for real-time updates
    const interval = setInterval(fetchComments, 5000)
    
    // Also reload when window gains focus (user switches back to tab)
    const handleFocus = () => {
      fetchComments()
    }
    const handleVisibilityChange = () => {
      if (!document.hidden) {
        fetchComments()
      }
    }
    
    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    
    return () => {
      clearInterval(interval)
      window.removeEventListener('focus', handleFocus)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [postId, isDeleted, loading, postData])

  if (loading) {
    return (
      <div>
        <div className="top-nav">
          <button className="btn-icon" onClick={() => onNavigate("home")}>
            ←
          </button>
          <div className="top-nav-title">Загрузка...</div>
          <div style={{ width: "40px" }}></div>
        </div>
        <div style={{ padding: "var(--spacing-lg)", textAlign: "center" }}>Загрузка поста...</div>
      </div>
    )
  }

  if (!postData) {
    return (
      <div>
        <div className="top-nav">
          <button className="btn-icon" onClick={() => onNavigate("home")}>
            ←
          </button>
          <div className="top-nav-title">Пост не найден</div>
          <div style={{ width: "40px" }}></div>
        </div>
        <div style={{ padding: "var(--spacing-lg)", textAlign: "center" }}>Пост не найден</div>
      </div>
    )
  }


  const handleCommentLike = async (commentId) => {
    if (!currentUser || !postId) return

    try {
      // Ensure all IDs are primitive values
      const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
      const commentIdValue = typeof commentId === "object" ? (commentId?.id || commentId?.commentId || null) : commentId
      const userIdValue = typeof currentUser.id === "object" ? (currentUser.id?.id || currentUser.id?.userId || null) : currentUser.id
      
      if (!postIdValue || !commentIdValue || !userIdValue) {
        console.error("[Comments] Invalid IDs for like:", { postId, commentId, userId: currentUser.id })
        return
      }
      
      await commentAPI.like(postIdValue, commentIdValue, userIdValue)
      
      // Оптимистичное обновление UI
      setComments((prevComments) => {
        return prevComments.map((comment) => {
          if (comment.id === commentId) {
            const wasLiked = comment.isLiked
            return {
              ...comment,
              likes: wasLiked ? comment.likes - 1 : comment.likes + 1,
              isLiked: !wasLiked,
              isDisliked: false,
              dislikes: comment.isDisliked ? Math.max(0, comment.dislikes - 1) : comment.dislikes,
            }
          }
          return comment
        })
      })
    } catch (error) {
      console.error("Ошибка при лайке комментария:", error)
    }
  }

  const handleCommentDislike = async (commentId) => {
    if (!currentUser || !postId) return

    try {
      // Ensure all IDs are primitive values
      const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
      const commentIdValue = typeof commentId === "object" ? (commentId?.id || commentId?.commentId || null) : commentId
      const userIdValue = typeof currentUser.id === "object" ? (currentUser.id?.id || currentUser.id?.userId || null) : currentUser.id
      
      if (!postIdValue || !commentIdValue || !userIdValue) {
        console.error("[Comments] Invalid IDs for dislike:", { postId, commentId, userId: currentUser.id })
        return
      }
      
      await commentAPI.dislike(postIdValue, commentIdValue, userIdValue)
      
      // Оптимистичное обновление UI
      setComments((prevComments) => {
        return prevComments.map((comment) => {
          if (comment.id === commentId) {
            const wasDisliked = comment.isDisliked
            return {
              ...comment,
              dislikes: wasDisliked ? comment.dislikes - 1 : comment.dislikes + 1,
              isDisliked: !wasDisliked,
              isLiked: false,
              likes: comment.isLiked ? Math.max(0, comment.likes - 1) : comment.likes,
            }
          }
          return comment
        })
      })
    } catch (error) {
      console.error("Ошибка при дизлайке комментария:", error)
    }
  }

  const confirmDeleteComment = async () => {
    if (!commentToDelete || !currentUser) return

    setShowDeleteCommentConfirm(false)
    try {
      const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
      const commentIdValue = typeof commentToDelete.id === "object" ? (commentToDelete.id?.id || commentToDelete.id?.commentId || null) : commentToDelete.id
      const userIdValue = typeof currentUser.id === "object" ? (currentUser.id?.id || currentUser.id?.userId || null) : currentUser.id
      
      if (!postIdValue || !commentIdValue || !userIdValue) {
        console.error("[Comments] Invalid IDs for delete:", { postId, commentId: commentToDelete.id, userId: currentUser.id })
        setAlertData({ title: "Ошибка", message: "Не удалось удалить комментарий: некорректные данные", variant: "error" })
        setShowAlert(true)
        return
      }
      
      await commentAPI.delete(postIdValue, commentIdValue, userIdValue)
      // Remove comment from local state
      setComments((prev) => prev.filter((c) => c.id !== commentToDelete.id))
      setPostComments((prev) => Math.max(0, prev - 1))
      setCommentToDelete(null)
    } catch (error) {
      console.error("Ошибка при удалении комментария:", error)
      setAlertData({ title: "Ошибка", message: "Не удалось удалить комментарий: " + (error.message || "Неизвестная ошибка"), variant: "error" })
      setShowAlert(true)
    }
  }

  const handleCommentSubmit = async () => {
    if (!commentText.trim() || !currentUser) return

    try {
      // Ensure postId is a primitive value
      const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
      if (!postIdValue) {
        console.error("[Comments] Invalid postId for comment creation:", postId)
        return
      }
      
      const commentData = {
        text: commentText,
        author: { id: currentUser.id },
      }

      // Создание комментария через API
      await commentAPI.create(postIdValue, commentData)

      // Clear comment input
      setCommentText("")
      
      // Reload comments to get the latest from server (including the new one)
      await fetchComments()
    } catch (error) {
      console.error("Ошибка при создании комментария:", error)
      // Можно показать уведомление об ошибке
    }
  }

  return (
    <div>
      <div className="top-nav">
        <button className="btn-icon" onClick={() => onNavigate("home")}>
          ←
        </button>
        <div className="top-nav-title">Пост</div>
        <div style={{ width: "40px" }}></div>
      </div>

      {/* Начало блока содержимого поста и комментариев */}
      <div style={{ padding: "var(--spacing-lg)", paddingBottom: "80px" }} className="comments-content">
        <div className="post-card">
          <div className="post-header">
            {postData.author?.avatar?.id ? (
              <img
                src={imageAPI.getImageUrl(postData.author.avatar.id)}
                alt={postData.author?.name || "Пользователь"}
                className="avatar avatar-md avatar-clickable"
                onClick={(e) => {
                  e.stopPropagation()
                  onNavigate("profile", postData.userId || postData.author?.id)
                }}
              />
            ) : (
              <div
                className="avatar avatar-md avatar-clickable"
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  backgroundColor: "var(--bg-tertiary)",
                  border: "1px solid var(--border-primary)",
                  borderRadius: "50%",
                  fontSize: "var(--font-size-base)",
                  width: "40px",
                  height: "40px",
                  cursor: "pointer"
                }}
                onClick={(e) => {
                  e.stopPropagation()
                  onNavigate("profile", postData.userId || postData.author?.id)
                }}
              >
                {getAvatarEmoji(postData.userId || postData.author?.id)}
              </div>
            )}
            <div className="post-user-info">
              <div className="post-username">
                {postData.author?.name || postData.author?.username || "Пользователь"}
                {postData.author?.rating && (
                  <span className="badge badge-rating">{postData.author.rating.toFixed(1)}</span>
                )}
              </div>
              <div className="post-time">{postData.time || (postData.createdAt ? new Date(postData.createdAt).toLocaleDateString("ru-RU") : "")}</div>
            </div>
            <div style={{ display: "flex", gap: "var(--spacing-sm)", alignItems: "center" }}>
              {currentUser && (postData.author?.id === currentUser.id || postData.userId === currentUser.id) && (
                <>
                  <button
                    className="btn btn-secondary"
                    onClick={(e) => {
                      e.stopPropagation()
                      setShowDeleteDialog(true)
                    }}
                    disabled={isDeleting}
                    style={{ fontSize: "var(--font-size-sm)", padding: "6px 12px" }}
                    title="Удалить пост"
                  >
                    {isDeleting ? "..." : "🗑️"}
                  </button>
                  <ConfirmDeleteDialog
                    open={showDeleteDialog}
                    onOpenChange={setShowDeleteDialog}
                    onConfirm={async () => {
                      setIsDeleting(true)
                      try {
                        const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
                        if (postIdValue) {
                          await postAPI.delete(postIdValue, currentUser.id)
                          // Устанавливаем флаг, что пост удален, чтобы предотвратить повторную загрузку
                          setIsDeleted(true)
                          setShowDeleteDialog(false)
                          // Редирект на главную страницу
                          onNavigate("home")
                        }
                      } catch (error) {
                        console.error("Ошибка при удалении поста:", error)
                        // Если ошибка 404, значит пост уже удален - просто редиректим
                        if (error.status === 404 || (error.message && error.message.includes("404"))) {
                          setIsDeleted(true)
                          setShowDeleteDialog(false)
                          onNavigate("home")
                        } else {
                          setAlertData({ title: "Ошибка", message: "Не удалось удалить пост. Попробуйте еще раз.", variant: "error" })
                          setShowAlert(true)
                        }
                      } finally {
                        setIsDeleting(false)
                      }
                    }}
                  />
                </>
              )}
              {currentUser && (postData.author?.id === currentUser.id || postData.userId === currentUser.id) ? (
                <div className="post-own-label">
                  Мой пост
                </div>
              ) : (
                <button className="btn btn-primary">подписаться</button>
              )}
            </div>
          </div>

          <h3 className="post-title">{postData.name || postData.title}</h3>
          <p className="post-content">{postData.text || postData.content}</p>

          {postData.image && (
            <img 
              src={
                typeof postData.image === "object" && postData.image.id
                  ? imageAPI.getImageUrl(postData.image.id)
                  : postData.image?.url || postData.image || "/placeholder.svg"
              } 
              alt={postData.name || postData.title} 
              className="post-image" 
            />
          )}

          {postData.externalLinks && (
            <div className="post-external-links" style={{ marginTop: "var(--spacing-sm)", marginBottom: "var(--spacing-sm)" }}>
              {(() => {
                try {
                  const links = typeof postData.externalLinks === "string" 
                    ? JSON.parse(postData.externalLinks) 
                    : postData.externalLinks
                  if (Array.isArray(links) && links.length > 0) {
                    return (
                      <div>
                        <strong style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)" }}>Ссылки:</strong>
                        {links.map((link, idx) => (
                          <a
                            key={idx}
                            href={link}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                              display: "block",
                              color: "var(--accent-blue)",
                              textDecoration: "underline",
                              marginTop: "var(--spacing-xs)",
                              fontSize: "var(--font-size-sm)",
                            }}
                            onClick={(e) => e.stopPropagation()}
                          >
                            {link}
                          </a>
                        ))}
                      </div>
                    )
                  }
                } catch (e) {
                  // If parsing fails, try to display as plain text
                  return (
                    <div>
                      <strong style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)" }}>Ссылка:</strong>
                      <a
                        href={postData.externalLinks}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{
                          display: "block",
                          color: "var(--accent-blue)",
                          textDecoration: "underline",
                          marginTop: "var(--spacing-xs)",
                          fontSize: "var(--font-size-sm)",
                        }}
                        onClick={(e) => e.stopPropagation()}
                      >
                        {postData.externalLinks}
                      </a>
                    </div>
                  )
                }
                return null
              })()}
            </div>
          )}

          <div className="post-actions">
            {/* Кнопка "Лайк" */}
            <button 
              className={`post-action-btn ${postInteractions.isLiked ? "liked" : ""}`} 
              onClick={postInteractions.handleLike}
              disabled={postInteractions.loading}
            >
              <img 
                src="/like.png" 
                alt="Лайк" 
                className="post-action-icon"
                style={{ width: '20px', height: '20px', objectFit: 'contain' }}
              />
              <span>{postInteractions.likes}</span>
            </button>
            {/* Кнопка "Дизлайк" */}
            <button 
              className={`post-action-btn ${postInteractions.isDisliked ? "disliked" : ""}`} 
              onClick={postInteractions.handleDislike}
              disabled={postInteractions.loading}
            >
              <img 
                src="/dislike.png" 
                alt="Дизлайк" 
                className="post-action-icon"
                style={{ width: '20px', height: '20px', objectFit: 'contain' }}
              />
              <span>{postInteractions.dislikes}</span>
            </button>
            {/* Кнопка "Комментарии" */}
            <button className="post-action-btn active">
              <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
              <span>{postComments}</span>
            </button>
          </div>
        </div> {/* <-- ЗАКРЫВАЕТ post-card */}

        <div className="comments-section"> {/* <-- СТРОКА, ГДЕ БЫЛА ОШИБКА */}
          <h2 className="comments-header">КОММЕНТАРИИ</h2>

          {comments.length > 0 ? comments.map((comment) => {
            // Ensure comment has required fields
            if (!comment || !comment.id) {
              return null
            }
            
            const authorId = comment.userId || comment.author?.id
            const authorName = comment.author?.name || comment.author?.username || "Пользователь"
            const authorAvatar = comment.author?.avatar
            const commentText = comment.text || comment.content || ""
            // Время комментария не отображается
            const commentTime = ""
            
            // Проверяем, есть ли аватарка у автора комментария
            const hasAvatar = authorAvatar?.id
            
            return (
              <div key={comment.id}>
                <div className="comment">
                  <div className="comment-header">
                    {hasAvatar ? (
                      <img
                        src={imageAPI.getImageUrl(authorAvatar.id)}
                        alt={authorName}
                        className="avatar avatar-md avatar-clickable"
                        onClick={() => {
                          if (authorId) {
                            onNavigate("profile", authorId)
                          }
                        }}
                      />
                    ) : (
                      // Если аватарки нет, показываем эмодзи-аватар
                      <div 
                        className="avatar avatar-md avatar-clickable"
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          backgroundColor: "var(--bg-tertiary)",
                          border: "1px solid var(--border-primary)",
                          borderRadius: "50%",
                          fontSize: "var(--font-size-base)",
                          cursor: "pointer"
                        }}
                        onClick={() => {
                          if (authorId) {
                            onNavigate("profile", authorId)
                          }
                        }}
                      >
                        {getAvatarEmoji(authorId)}
                      </div>
                    )}
                    <div style={{ flex: 1 }}>
                      <div className="post-username">
                        {authorName}
                        {comment.author?.rating && comment.author.rating > 0 && (
                          <span className="badge badge-rating">{comment.author.rating.toFixed(1)}</span>
                        )}
                      </div>
                      {commentTime && <div className="post-time">{commentTime}</div>}
                    </div>
                  </div>

                  <p className="comment-content">{commentText}</p>

                <div className="comment-actions">
                  <button
                    className={`comment-action ${comment.isLiked ? "liked" : ""}`}
                    onClick={() => handleCommentLike(comment.id)}
                  >
                    <img 
                      src="/like.png" 
                      alt="Лайк" 
                      className="comment-icon"
                      style={{ width: '18px', height: '18px', objectFit: 'contain' }}
                    />
                    {comment.likedUsers?.length || comment.likes || 0}
                  </button>
                  <button
                    className={`comment-action ${comment.isDisliked ? "disliked" : ""}`}
                    onClick={() => handleCommentDislike(comment.id)}
                  >
                    <img 
                      src="/dislike.png" 
                      alt="Дизлайк" 
                      className="comment-icon"
                      style={{ width: '18px', height: '18px', objectFit: 'contain' }}
                    />
                    {comment.dislikedUsers?.length || comment.dislikes || 0}
                  </button>
                  {currentUser && (authorId === currentUser.id) && (
                    <button
                      className="comment-action"
                      onClick={() => {
                        setCommentToDelete(comment)
                        setShowDeleteCommentConfirm(true)
                      }}
                      style={{ 
                        marginLeft: "auto",
                        color: "var(--error-color, #dc3545)",
                        fontSize: "var(--font-size-sm)",
                        padding: "4px 8px"
                      }}
                      title="Удалить комментарий"
                    >
                      🗑️
                    </button>
                  )}
                </div>
              </div>
            </div>
            )
          }).filter(Boolean) : (
            <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
              Пока нет комментариев
            </div>
          )}

          <div style={{ marginTop: "var(--spacing-lg)" }}>
            <textarea
              className="input textarea"
              placeholder="Написать комментарий (до 500 символов)..."
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              maxLength="500"
            />
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginTop: "var(--spacing-sm)",
              }}
            >
              <div className="char-counter">{commentText.length}/500</div>
              <button className="btn btn-primary" onClick={handleCommentSubmit}>
                Отправить
              </button>
            </div>
          </div>
        </div>
      </div>

      <BottomNavigation currentPage={currentPage || "home"} onNavigate={onNavigate} />

      {/* Alert Dialog */}
      <AlertDialog
        open={showAlert}
        title={alertData.title}
        message={alertData.message}
        variant={alertData.variant}
        onClose={() => setShowAlert(false)}
      />

      {/* Confirm Dialog for deleting comment */}
      <ConfirmDialog
        open={showDeleteCommentConfirm}
        title="Удалить комментарий"
        message="Удалить этот комментарий?"
        confirmText="Удалить"
        cancelText="Отмена"
        confirmVariant="destructive"
        onConfirm={confirmDeleteComment}
        onCancel={() => {
          setShowDeleteCommentConfirm(false)
          setCommentToDelete(null)
        }}
      />
    </div>
  )
}

export default Comments
