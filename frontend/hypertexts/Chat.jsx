"use client"

import { useState } from "react"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/rooms.css"

function Chat({ onNavigate }) {
  const [messageText, setMessageText] = useState("")

  // Демо данные чата
  const chatData = {
    roomName: "Физика-механика в Саратове",
    members: 8,
    category: "Наука",
  }

  const messages = [
    {
      id: 1,
      sender: "Петр Сидоров",
      text: "Всем привет! Напоминаю, что встреча завтра в 12:00",
      time: "14:25",
      avatar: "/male-avatar.png",
      isOwn: false,
    },
    {
      id: 2,
      sender: "Вы",
      text: "Отлично, буду обязательно!",
      time: "14:27",
      avatar: "/diverse-user-avatars.png",
      isOwn: true,
    },
    {
      id: 3,
      sender: "Анна Иванова",
      text: "А можно взять с собой друга?",
      time: "14:30",
      avatar: "/diverse-female-avatar.png",
      isOwn: false,
    },
    {
      id: 4,
      sender: "Петр Сидоров",
      text: "Конечно! Чем больше, тем лучше 😊",
      time: "14:32",
      avatar: "/male-avatar.png",
      isOwn: false,
    },
  ]

  const handleSendMessage = () => {
    if (messageText.trim()) {
      console.log("Отправка сообщения:", messageText)
      setMessageText("")
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  return (
    <div className="chat-container">
      {/* Шапка чата - кликабельна */}
      <div className="chat-header" onClick={() => onNavigate("roomInfo")}>
        <button
          className="btn-icon"
          onClick={(e) => {
            e.stopPropagation()
            onNavigate("rooms")
          }}
        >
          ←
        </button>
        <img src="/physics-icon.jpg" alt={chatData.roomName} className="avatar avatar-md" />
        <div className="chat-header-info">
          <div className="chat-header-title">{chatData.roomName}</div>
          <div className="chat-header-subtitle">{chatData.members} участников</div>
        </div>
        <button className="btn-icon" onClick={(e) => e.stopPropagation()}>
          ⋮
        </button>
      </div>

      {/* Сообщения */}
      <div className="chat-messages">
        {messages.map((message) => (
          <div key={message.id} className={`message ${message.isOwn ? "own" : ""}`}>
            <img src={message.avatar || "/placeholder.svg"} alt={message.sender} className="avatar avatar-md" />
            <div className="message-content">
              {!message.isOwn && <div className="message-sender">{message.sender}</div>}
              <div className="message-text">{message.text}</div>
              <div className="message-time">{message.time}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Поле ввода */}
      <div className="chat-input-container">
        <button className="btn-icon">📎</button>
        <textarea
          className="chat-input"
          placeholder="Сообщение (до 500 символов)..."
          value={messageText}
          onChange={(e) => setMessageText(e.target.value)}
          onKeyPress={handleKeyPress}
          maxLength="500"
          rows="1"
        />
        <button className="chat-send-btn" onClick={handleSendMessage}>
          ➤
        </button>
      </div>
    </div>
  )
}

export default Chat
