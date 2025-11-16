"use client"

import { useState } from "react"
import "../styles/comments.css"

/**
 * Компонент комментария
 */
function Comment({ comment, replies }) {
  const [showReplies, setShowReplies] = useState(true)
  const [commentLikes, setCommentLikes] = useState(comment.likes || 2)

  return (
    <div className="comment">
      <div className="comment-header">
        <div className="comment-avatar">👤</div>

        <div className="comment-author-info">
          <div>
            <span className="comment-author-name">{comment.authorName}</span>
            {comment.rating && <span className="comment-rating">{comment.rating}</span>}
          </div>
          <div className="comment-time">{comment.time}</div>
        </div>
      </div>

      <p className="comment-text">{comment.text}</p>

      <div className="comment-footer">
        <button className="reply-button">ответить</button>

        <div className="comment-likes">
          <button className="like-btn">👍 {commentLikes}</button>
          <button className="like-btn">👎 {comment.dislikes || 2}</button>
        </div>
      </div>

      {replies && replies.length > 0 && (
        <div className="replies-section">
          <h4 className="replies-title">ответы</h4>
          {replies.map((reply) => (
            <div key={reply.id} className="reply">
              <div className="comment-header">
                <div className="comment-avatar">👤</div>

                <div className="comment-author-info">
                  <div>
                    <span className="comment-author-name">{reply.authorName}</span>
                    {reply.rating && <span className="comment-rating">{reply.rating}</span>}
                  </div>
                  <div className="comment-time">{reply.time}</div>
                </div>
              </div>

              <p className="comment-text">{reply.text}</p>

              <div className="comment-footer">
                <button className="reply-button">ответить</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default Comment
