"use client"

import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { commentAPI, postAPI, imageAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { usePostInteractions } from "../hooks/usePostInteractions"
import { useCommentInteractions } from "../hooks/useCommentInteractions"
import { ConfirmDeleteDialog } from "../components/ConfirmDeleteDialog"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/post.css"
import "../styles/navigation.css"

function Comments({ onNavigate, postId }) {
  const { currentUser } = useUser()
  const [postData, setPostData] = useState(null)
  const [comments, setComments] = useState([])
  const [postComments, setPostComments] = useState(0)
  const [commentText, setCommentText] = useState("")
  const [loading, setLoading] = useState(true)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isDeleted, setIsDeleted] = useState(false)
  
  // Hook for post interactions (likes/dislikes)
  const postInteractions = usePostInteractions(postData)

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
      try {
        const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
        console.log("[Comments] Loading comments for post:", postIdValue)
        const apiComments = await commentAPI.getByPost(postIdValue)
        console.log("[Comments] Loaded comments from API:", apiComments)
        // Map and sort comments by creation date (oldest first for chronological order)
        // Маппинг комментариев (без ответов - только корневые комментарии)
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
                  avatar: c.author?.avatar || "/placeholder.svg",
                  rating: c.author?.rating,
                },
                text: c.text || c.content || "",
                createdAt: c.createdAt || c.created_at,
                time: c.createdAt ? new Date(c.createdAt).toLocaleDateString("ru-RU") : "",
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
        // Fallback: use comments from post object if available
        const postComments = post.comments || []
        if (Array.isArray(postComments) && postComments.length > 0) {
          const mappedComments = postComments.map(c => ({
            id: c.id,
            userId: c.author?.id || c.authorId,
            author: {
              id: c.author?.id || c.authorId,
              name: c.author?.name || c.author?.username || "Пользователь",
              avatar: c.author?.avatar || "/placeholder.svg",
            },
            text: c.text || c.content || "",
            createdAt: c.createdAt,
            likes: c.likedUsers?.length || 0,
            likedUsers: c.likedUsers || [],
            isLiked: false,
            commentCount: 0,
            replies: [],
          }))
          setComments(mappedComments)
          setPostComments(mappedComments.length)
        } else {
          setComments([])
          setPostComments(0)
        }
      }
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
      const newComment = await commentAPI.create(postIdValue, commentData)

      // Optimistically update UI with the new comment
      const uiComment = {
        id: newComment?.id || Date.now(),
        userId: currentUser.id,
        author: {
          id: currentUser.id,
          name: currentUser.name || currentUser.username || "Вы",
          avatar: currentUser.avatar || "/male-avatar.png",
          rating: currentUser.rating || 0,
        },
        text: commentText,
        createdAt: new Date().toISOString(),
        time: "только что",
        likes: 0,
        likedUsers: [],
        isLiked: false,
        commentCount: 0,
        replies: [],
      }

      // Add new comment to the list
      setComments([...comments, uiComment])
      setPostComments(postComments + 1)

      setCommentText("")
      
      // Refresh comments from server to get the actual comment data
      // This ensures we have the correct ID and any server-side formatting
      // Use a small delay to ensure the server has processed the comment
      setTimeout(async () => {
        try {
          const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
          if (!postIdValue) {
            console.error("[Comments] Invalid postId for refresh:", postId)
            return
          }
          const refreshedComments = await commentAPI.getByPost(postIdValue)
          if (Array.isArray(refreshedComments) && refreshedComments.length > 0) {
            // Маппинг комментариев (без ответов - только корневые комментарии)
            const sortedComments = refreshedComments.map(c => {
              const authorId = c.author?.id || c.authorId
              const authorName = c.author?.name || c.author?.username || "Пользователь"
              
              return {
                id: c.id,
                userId: authorId,
                author: {
                  id: authorId,
                  name: authorName,
                  username: c.author?.username,
                  avatar: c.author?.avatar || "/placeholder.svg",
                  rating: c.author?.rating,
                },
                text: c.text || c.content || "",
                createdAt: c.createdAt || c.created_at,
                time: c.createdAt ? new Date(c.createdAt).toLocaleDateString("ru-RU") : "",
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
            setComments(sortedComments)
            setPostComments(sortedComments.length)
          } else {
            // If refresh returns empty, keep the optimistic update
            console.warn("Comment refresh returned empty, keeping optimistic update")
          }
        } catch (error) {
          console.error("Ошибка обновления комментариев:", error)
          // Keep the optimistic update if refresh fails
        }
      }, 500)
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
            <img
              src={postData.author?.avatar || "/placeholder.svg"}
              alt={postData.author?.name || "Пользователь"}
              className="avatar avatar-md avatar-clickable"
              onClick={(e) => {
                e.stopPropagation()
                onNavigate("profile", postData.userId || postData.author?.id)
              }}
            />
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
                          alert("Не удалось удалить пост. Попробуйте еще раз.")
                        }
                      } finally {
                        setIsDeleting(false)
                      }
                    }}
                  />
                </>
              )}
              <button className="btn btn-primary">подписаться</button>
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
            const authorAvatar = comment.author?.avatar || "/placeholder.svg"
            const commentText = comment.text || comment.content || ""
            const commentTime = comment.time || (comment.createdAt ? new Date(comment.createdAt).toLocaleDateString("ru-RU") : "")
            
            return (
              <div key={comment.id}>
                <div className="comment">
                  <div className="comment-header">
                    <img
                      src={authorAvatar}
                      alt={authorName}
                      className="avatar avatar-md avatar-clickable"
                      onClick={() => {
                        if (authorId) {
                          onNavigate("profile", authorId)
                        }
                      }}
                    />
                    <div style={{ flex: 1 }}>
                      <div className="post-username">
                        {authorName}
                        {comment.author?.rating && (
                          <span className="badge badge-rating">{comment.author.rating.toFixed(1)}</span>
                        )}
                      </div>
                      <div className="post-time">{commentTime}</div>
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

      <BottomNavigation currentPage="home" onNavigate={onNavigate} />
    </div>
  )
}

export default Comments
