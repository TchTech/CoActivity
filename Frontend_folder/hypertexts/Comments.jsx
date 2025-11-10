"use client"

import { useState, useEffect } from "react"
import BottomNavigation from "./BottomNavigation"
import { getPostById } from "../scripts/postsData"
import { getCommentsForPost } from "../scripts/commentsData"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/post.css"
import "../styles/navigation.css"

function Comments({ onNavigate, postId }) {
  const [postData, setPostData] = useState(null)
  const [comments, setComments] = useState([])
  const [postLikes, setPostLikes] = useState(0)
  const [postComments, setPostComments] = useState(0)
  const [isPostLiked, setIsPostLiked] = useState(false)
  const [commentText, setCommentText] = useState("")
  const [replyingTo, setReplyingTo] = useState(null)

  const fetchPostData = () => {
    const data = getPostById(postId)
    setPostData(data)
    if (data) {
      const initialComments = getCommentsForPost(postId, data.userId, data.time)
      setComments(initialComments)
      setPostLikes(data.likes)
      setPostComments(data.comments)
    }
  }

  useEffect(() => {
    console.log("[v0] Loading post with ID:", postId)
    fetchPostData()
  }, [postId])

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

  const handlePostLike = () => {
    if (isPostLiked) {
      setPostLikes(postLikes - 1)
    } else {
      setPostLikes(postLikes + 1)
    }
    setIsPostLiked(!isPostLiked)
  }

  const handleCommentLike = (commentId, isReply = false, parentId = null) => {
    setComments((prevComments) => {
      return prevComments.map((comment) => {
        if (isReply && comment.id === parentId) {
          return {
            ...comment,
            replies: comment.replies.map((reply) => {
              if (reply.id === commentId) {
                return {
                  ...reply,
                  likes: reply.isLiked ? reply.likes - 1 : reply.likes + 1,
                  isLiked: !reply.isLiked,
                }
              }
              return reply
            }),
          }
        } else if (comment.id === commentId) {
          return {
            ...comment,
            likes: comment.isLiked ? comment.likes - 1 : comment.likes + 1,
            isLiked: !comment.isLiked,
          }
        }
        return comment
      })
    })
  }

  const handleCommentSubmit = () => {
    if (commentText.trim()) {
      const newComment = {
        id: Date.now(),
        userId: 1,
        author: { name: "Вы", avatar: "/male-avatar.png", rating: 8.5 },
        text: commentText,
        time: "только что",
        likes: 0,
        isLiked: false,
        commentCount: 0,
        replies: [],
      }

      if (replyingTo) {
        setComments((prevComments) => {
          return prevComments.map((comment) => {
            if (comment.id === replyingTo) {
              return {
                ...comment,
                commentCount: comment.commentCount + 1,
                replies: [
                  ...comment.replies,
                  {
                    ...newComment,
                    id: Date.now() + Math.random(),
                  },
                ],
              }
            }
            return comment
          })
        })
        setReplyingTo(null)
      } else {
        setComments([...comments, newComment])
        setPostComments(postComments + 1)
      }

      setCommentText("")
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

      <div style={{ padding: "var(--spacing-lg)", paddingBottom: "80px" }}>
        <div className="post-card">
          <div className="post-header">
            <img
              src={postData.author.avatar || "/placeholder.svg"}
              alt={postData.author.name}
              className="avatar avatar-md avatar-clickable"
              onClick={(e) => {
                e.stopPropagation()
                onNavigate("profile", postData.userId)
              }}
            />
            <div className="post-user-info">
              <div className="post-username">
                {postData.author.name}
                <span className="badge badge-rating">{postData.author.rating}</span>
              </div>
              <div className="post-time">{postData.time}</div>
            </div>
            <button className="btn btn-primary">подписаться</button>
          </div>

          <h3 className="post-title">{postData.title}</h3>
          <p className="post-content">{postData.content}</p>

          <img src={postData.image || "/placeholder.svg"} alt={postData.title} className="post-image" />

          <div className="post-actions">
            <button className={`post-action-btn ${isPostLiked ? "liked" : ""}`} onClick={handlePostLike}>
              <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path
                  d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                  fill={isPostLiked ? "currentColor" : "none"}
                />
              </svg>
              <span>{postLikes}</span>
            </button>
            <button className="post-action-btn active">
              <svg className="post-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
              <span>{postComments}</span>
            </button>
            <button
              className="post-action-btn"
              style={{
                marginLeft: "auto",
                backgroundColor: "var(--accent-gold)",
                color: "var(--bg-primary)",
                padding: "8px 20px",
                borderRadius: "var(--radius-full)",
              }}
            >
              ОТКЛИКНУТЬСЯ
            </button>
          </div>
        </div>

        <div className="comments-section">
          <h2 className="comments-header">КОММЕНТАРИИ</h2>

          {comments.map((comment) => (
            <div key={comment.id}>
              <div className="comment">
                <div className="comment-header">
                  <img
                    src={comment.author.avatar || "/placeholder.svg"}
                    alt={comment.author.name}
                    className="avatar avatar-md avatar-clickable"
                    onClick={() => onNavigate("profile", comment.userId)}
                  />
                  <div style={{ flex: 1 }}>
                    <div className="post-username">
                      {comment.author.name}
                      <span className="badge badge-rating">{comment.author.rating}</span>
                    </div>
                    <div className="post-time">{comment.time}</div>
                  </div>
                </div>

                <p className="comment-content">{comment.text}</p>

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
                    <svg className="comment-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path
                        d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                        fill={comment.isLiked ? "currentColor" : "none"}
                      />
                    </svg>
                    {comment.likes}
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
                          src={reply.author.avatar || "/placeholder.svg"}
                          alt={reply.author.name}
                          className="avatar avatar-sm avatar-clickable"
                          onClick={() => onNavigate("profile", reply.userId)}
                        />
                        <div style={{ flex: 1 }}>
                          <div className="post-username" style={{ fontSize: "var(--font-size-sm)" }}>
                            {reply.author.name}
                            <span className="badge badge-rating" style={{ fontSize: "10px", padding: "2px 8px" }}>
                              {reply.author.rating}
                            </span>
                          </div>
                          <div className="post-time">{reply.time}</div>
                        </div>
                      </div>

                      <p className="comment-content" style={{ fontSize: "var(--font-size-sm)" }}>
                        {reply.text}
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
                          <svg className="comment-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <path
                              d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                              fill={reply.isLiked ? "currentColor" : "none"}
                            />
                          </svg>
                          {reply.likes}
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
          ))}

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
