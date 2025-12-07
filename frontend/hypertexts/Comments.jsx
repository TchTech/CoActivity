"use client"

import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { commentAPI, postAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import { usePostInteractions } from "../hooks/usePostInteractions"
import { useCommentInteractions } from "../hooks/useCommentInteractions"
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
  const [replyingTo, setReplyingTo] = useState(null)
  const [loading, setLoading] = useState(true)
  
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
      const post = await postAPI.getById(postIdValue)
      console.log("[Comments] Loaded post:", post)
      setPostData(post)
      
      // Load comments from backend
      try {
        const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
        console.log("[Comments] Loading comments for post:", postIdValue)
        const apiComments = await commentAPI.getByPost(postIdValue)
        console.log("[Comments] Loaded comments from API:", apiComments)
        // Map and sort comments by creation date (oldest first for chronological order)
        const sortedComments = Array.isArray(apiComments) && apiComments.length > 0
          ? apiComments.map(c => {
              // Ensure we properly map the comment data structure
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
                isLiked: false,
                commentCount: 0,
                replies: [],
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
      // Если не удалось загрузить данные, показываем пустое состояние
      setPostData(null)
      setComments([])
      setPostComments(0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    // Ensure postId is a primitive value
    const postIdValue = typeof postId === "object" ? (postId?.id || postId?.postId || null) : postId
    console.log("[Comments] useEffect triggered, postId:", postId, "postIdValue:", postIdValue)
    if (postIdValue) {
      fetchPostData()
    } else {
      console.warn("[Comments] No valid postId provided")
      setLoading(false)
    }
  }, [postId])

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


  const handleCommentLike = async (commentId, isReply = false, parentId = null) => {
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
          if (isReply && comment.id === parentId) {
            return {
              ...comment,
              replies: comment.replies.map((reply) => {
                if (reply.id === commentId) {
                  const wasLiked = reply.isLiked
                  return {
                    ...reply,
                    likes: wasLiked ? reply.likes - 1 : reply.likes + 1,
                    isLiked: !wasLiked,
                  }
                }
                return reply
              }),
            }
          } else if (comment.id === commentId) {
            const wasLiked = comment.isLiked
            return {
              ...comment,
              likes: wasLiked ? comment.likes - 1 : comment.likes + 1,
              isLiked: !wasLiked,
            }
          }
          return comment
        })
      })
    } catch (error) {
      console.error("Ошибка при лайке комментария:", error)
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

      if (replyingTo) {
        // For replies, we'll add them to the parent comment's replies array
        // Note: The backend may not support nested replies yet, so this is optimistic UI
        setComments((prevComments) => {
          return prevComments.map((comment) => {
            if (comment.id === replyingTo) {
              return {
                ...comment,
                commentCount: (comment.commentCount || 0) + 1,
                replies: [
                  ...(comment.replies || []),
                  {
                    ...uiComment,
                    id: newComment?.id || Date.now() + Math.random(),
                  },
                ],
              }
            }
            return comment
          })
        })
        setReplyingTo(null)
      } else {
        // Add new comment to the list
        setComments([...comments, uiComment])
        setPostComments(postComments + 1)
      }

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
            const sortedComments = refreshedComments.map(c => {
              // Ensure we have all required fields
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
                isLiked: false,
                commentCount: 0,
                replies: [],
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
      <div style={{ padding: "var(--spacing-lg)", paddingBottom: "80px" }}>
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
            <button className="btn btn-primary">подписаться</button>
          </div>

          <h3 className="post-title">{postData.name || postData.title}</h3>
          <p className="post-content">{postData.text || postData.content}</p>

          <img src={postData.image?.url || postData.image || "/placeholder.svg"} alt={postData.name || postData.title} className="post-image" />

          <div className="post-actions">
            {/* Кнопка "Лайк" */}
            <button 
              className={`post-action-btn ${postInteractions.isLiked ? "liked" : ""}`} 
              onClick={postInteractions.handleLike}
              disabled={postInteractions.loading}
            >
              <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                {/* SVG-разметка для Лайка */}
                <path d="M7 22V11M2 13l5-10 5 10M17 22v-6M12 18l5-6 5 6" fill={postInteractions.isLiked ? "currentColor" : "none"}/>
                <path d="M12 2L7 7h10L12 2z" fill={postInteractions.isLiked ? "currentColor" : "none"}/>
                <path d="M7 7v15h10V7" fill={postInteractions.isLiked ? "currentColor" : "none"}/>
              </svg>
              <span>{postInteractions.likes}</span>
            </button>
            {/* Кнопка "Дизлайк" */}
            <button 
              className={`post-action-btn ${postInteractions.isDisliked ? "disliked" : ""}`} 
              onClick={postInteractions.handleDislike}
              disabled={postInteractions.loading}
            >
              <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                {/* SVG-разметка для Дизлайка */}
                <path d="M17 2v11M22 11l-5-10-5 10M7 2v6M12 6l-5 6-5-6" fill={postInteractions.isDisliked ? "currentColor" : "none"}/>
                <path d="M12 22L7 17h10L12 22z" fill={postInteractions.isDisliked ? "currentColor" : "none"}/>
                <path d="M7 17V2h10v15" fill={postInteractions.isDisliked ? "currentColor" : "none"}/>
              </svg>
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
                  <button className="comment-action" onClick={() => setReplyingTo(comment.id)}>
                    <svg className="comment-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    </svg>
                    ответить
                  </button>
                  <button
                    className={`comment-action ${comment.isLiked ? "liked" : ""}`}
                    onClick={() => handleCommentLike(comment.id)}
                  >
                    <svg className="comment-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M7 22V11M2 13l5-10 5 10M17 22v-6M12 18l5-6 5 6" fill={comment.isLiked ? "currentColor" : "none"}/>
                      <path d="M12 2L7 7h10L12 2z" fill={comment.isLiked ? "currentColor" : "none"}/>
                      <path d="M7 7v15h10V7" fill={comment.isLiked ? "currentColor" : "none"}/>
                    </svg>
                    {comment.likedUsers?.length || comment.likes || 0}
                  </button>
                  {comment.commentCount > 0 && (
                    <button className="comment-action">
                      <svg className="comment-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                      </svg>
                      {comment.commentCount}
                    </button>
                  )}
                </div>
              </div>

              {comment.replies && comment.replies.length > 0 && (
                <div className="comment-replies">
                  <div className="reply-label">ответы</div>
                  {comment.replies.map((reply) => (
                    <div key={reply.id} className="comment">
                      <div className="comment-header">
                        <img
                          src={reply.author?.avatar || "/placeholder.svg"}
                          alt={reply.author?.name || "Пользователь"}
                          className="avatar avatar-sm avatar-clickable"
                          onClick={() => {
                            const userId = reply.userId || reply.author?.id
                            if (userId) {
                              onNavigate("profile", userId)
                            }
                          }}
                        />
                        <div style={{ flex: 1 }}>
                          <div className="post-username" style={{ fontSize: "var(--font-size-sm)" }}>
                            {reply.author?.name || reply.author?.username || "Пользователь"}
                            {reply.author?.rating && (
                              <span className="badge badge-rating" style={{ fontSize: "10px", padding: "2px 8px" }}>
                                {reply.author.rating.toFixed(1)}
                              </span>
                            )}
                          </div>
                          <div className="post-time">{reply.time || (reply.createdAt ? new Date(reply.createdAt).toLocaleDateString("ru-RU") : "")}</div>
                        </div>
                      </div>

                      <p className="comment-content" style={{ fontSize: "var(--font-size-sm)" }}>
                        {reply.text || reply.content || ""}
                      </p>

                      <div className="comment-actions">
                        <button className="comment-action" onClick={() => setReplyingTo(comment.id)}>
                          <svg className="comment-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                          </svg>
                          ответить
                        </button>
                        <button
                          className={`comment-action ${reply.isLiked ? "liked" : ""}`}
                          onClick={() => handleCommentLike(reply.id, true, comment.id)}
                        >
                          <svg className="comment-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M7 22V11M2 13l5-10 5 10M17 22v-6M12 18l5-6 5 6" fill={reply.isLiked ? "currentColor" : "none"}/>
                            <path d="M12 2L7 7h10L12 2z" fill={reply.isLiked ? "currentColor" : "none"}/>
                            <path d="M7 7v15h10V7" fill={reply.isLiked ? "currentColor" : "none"}/>
                          </svg>
                          {reply.likedUsers?.length || reply.likes || 0}
                        </button>
                        {reply.commentCount > 0 && (
                          <button className="comment-action">
                            <svg className="comment-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                            </svg>
                            {reply.commentCount}
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            )
          }).filter(Boolean) : (
            <div style={{ textAlign: "center", padding: "var(--spacing-xl)", color: "var(--text-muted)" }}>
              Пока нет комментариев
            </div>
          )}

          <div style={{ marginTop: "var(--spacing-lg)" }}>
            {replyingTo && (
              <div
                style={{
                  fontSize: "var(--font-size-sm)",
                  color: "var(--accent-gold)",
                  marginBottom: "var(--spacing-sm)",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <span>Ответ на комментарий</span>
                <button
                  onClick={() => setReplyingTo(null)}
                  style={{
                    background: "none",
                    border: "none",
                    color: "var(--text-muted)",
                    cursor: "pointer",
                  }}
                >
                  ✕
                </button>
              </div>
            )}
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
