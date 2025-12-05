"use client"

import { useState } from "react"
import { UserProvider } from "./context/UserContext"
import Login from "./hypertexts/Login"
import Register from "./hypertexts/Register"
import ForgotPassword from "./hypertexts/ForgotPassword"
import ResetPassword from "./hypertexts/ResetPassword"
import VerifyEmail from "./hypertexts/VerifyEmail"
import Profile from "./hypertexts/Profile"
import Comments from "./hypertexts/Comments"
import RoomsList from "./hypertexts/RoomsList"
import Chat from "./hypertexts/Chat"
import RoomInfo from "./hypertexts/RoomInfo"
import CreatePost from "./hypertexts/CreatePost"
import CreateRoom from "./hypertexts/CreateRoom"
import Settings from "./hypertexts/Settings"
import Feed from "./hypertexts/Feed"
import Notifications from "./hypertexts/Notifications"
import TwoFactorSettings from "./hypertexts/TwoFactorSettings"

/**
 * Главный компонент приложения
 * Переключение между страницами
 */
function App() {
  // Проверяем URL для токена сброса пароля при загрузке
  const getInitialPage = () => {
    if (typeof window !== "undefined") {
      const pathname = window.location.pathname
      const urlParams = new URLSearchParams(window.location.search)
      const token = urlParams.get("token")
      
      // Если это Next.js роут /reset-password или /verify-email, Next.js обработает его
      if ((pathname === "/reset-password" || pathname === "/verify-email") && token) {
        // Next.js страница обработает это отдельно
        return "login"
      }
      
      // Определяем тип токена по контексту или показываем verify-email по умолчанию для токенов
      if (token && (pathname === "/" || pathname === "")) {
        // По умолчанию предполагаем, что это токен подтверждения email
        // Пользователь может перейти на reset-password через прямую ссылку из письма
        return "verify-email"
      }
    }
    return "login"
  }

  const [currentPage, setCurrentPage] = useState(getInitialPage())
  const [viewingUserId, setViewingUserId] = useState(null)
  const [viewingPostId, setViewingPostId] = useState(null)
  const [viewingRoomId, setViewingRoomId] = useState(null)
  const [viewingRoomId, setViewingRoomId] = useState(null)

  const handleNavigate = (page, param) => {
    // Ensure page is always a string
    const pageStr = typeof page === "string" ? page : String(page)
    console.log("[App] Navigation:", pageStr, "param:", param, "param type:", typeof param)
    
    // Ensure param is a primitive value, not an object
    let paramValue = null
    if (param !== null && param !== undefined) {
      if (typeof param === "object") {
        // If param is an object, try to extract an ID
        paramValue = param.id || param.userId || param.postId || param.roomId || null
        console.warn("[App] Navigation param was an object, extracted:", paramValue)
      } else {
        paramValue = param
      }
    }
    
    console.log("[App] Setting currentPage to:", pageStr, "with param:", paramValue)
    setCurrentPage(pageStr)
    // Ensure page is always a string
    console.log("[App] Navigation:", pageStr, "param:", param, "param type:", typeof param)
    
    // Ensure param is a primitive value, not an object
    if (param !== null && param !== undefined) {
      if (typeof param === "object") {
        // If param is an object, try to extract an ID
        paramValue = param.id || param.userId || param.postId || param.roomId || null
        console.warn("[App] Navigation param was an object, extracted:", paramValue)
      } else {
        paramValue = param
      }
    }
    
    console.log("[App] Setting currentPage to:", pageStr, "with param:", paramValue)
    setCurrentPage(pageStr)

    if (pageStr === "profile") {
      setViewingUserId(paramValue)
      console.log("[App] Set viewingUserId to:", paramValue)
    } else if (pageStr === "comments") {
      setViewingPostId(paramValue)
      console.log("[App] Set viewingPostId to:", paramValue)
    } else if (pageStr === "chat" || pageStr === "roomInfo") {
      setViewingRoomId(paramValue)
      console.log("[App] Set viewingRoomId to:", paramValue)
    } else {
      // Clear all params for other pages
      setViewingUserId(null)
      setViewingPostId(null)
      setViewingRoomId(null)
    if (pageStr === "profile") {
      setViewingUserId(paramValue)
      console.log("[App] Set viewingUserId to:", paramValue)
    } else if (pageStr === "comments") {
      setViewingPostId(paramValue)
      console.log("[App] Set viewingPostId to:", paramValue)
    } else if (pageStr === "chat" || pageStr === "roomInfo") {
      setViewingRoomId(paramValue)
      console.log("[App] Set viewingRoomId to:", paramValue)
    } else {
      // Clear all params for other pages
      setViewingUserId(null)
      setViewingPostId(null)
      setViewingRoomId(null)
    }
  }

  const renderPage = () => {
    switch (currentPage) {
      case "login":
        return <Login onNavigate={handleNavigate} />
      case "register":
        return <Register onNavigate={handleNavigate} />
      case "forgot-password":
        return <ForgotPassword onNavigate={handleNavigate} />
      case "reset-password":
        return <ResetPassword onNavigate={handleNavigate} />
      case "verify-email":
        return <VerifyEmail onNavigate={handleNavigate} />
      case "home":
        return <Feed onNavigate={handleNavigate} currentPage={currentPage} />
        return <Feed onNavigate={handleNavigate} currentPage={currentPage} />
      case "profile":
        return <Profile onNavigate={handleNavigate} userId={viewingUserId} currentPage={currentPage} />
        return <Profile onNavigate={handleNavigate} userId={viewingUserId} currentPage={currentPage} />
      case "comments":
        return <Comments onNavigate={handleNavigate} postId={viewingPostId} currentPage={currentPage} />
        return <Comments onNavigate={handleNavigate} postId={viewingPostId} currentPage={currentPage} />
      case "rooms":
        return <RoomsList onNavigate={handleNavigate} currentPage={currentPage} />
        return <RoomsList onNavigate={handleNavigate} currentPage={currentPage} />
      case "chat":
        return <Chat onNavigate={handleNavigate} roomId={viewingRoomId} currentPage={currentPage} />
        return <Chat onNavigate={handleNavigate} roomId={viewingRoomId} currentPage={currentPage} />
      case "roomInfo":
        return <RoomInfo onNavigate={handleNavigate} roomId={viewingRoomId} currentPage={currentPage} />
        return <RoomInfo onNavigate={handleNavigate} roomId={viewingRoomId} currentPage={currentPage} />
      case "createPost":
        return <CreatePost onNavigate={handleNavigate} currentPage={currentPage} />
        return <CreatePost onNavigate={handleNavigate} currentPage={currentPage} />
      case "createRoom":
        return <CreateRoom onNavigate={handleNavigate} currentPage={currentPage} />
        return <CreateRoom onNavigate={handleNavigate} currentPage={currentPage} />
      case "settings":
        return <Settings onNavigate={handleNavigate} currentPage={currentPage} />
      case "two-factor-settings":
        return <TwoFactorSettings onNavigate={handleNavigate} />
      case "notifications":
        return <Notifications onNavigate={handleNavigate} currentPage={currentPage} />
      default:
        return <Login onNavigate={handleNavigate} />
    }
  }

  return (
    <UserProvider>
      <div>{renderPage()}</div>
    </UserProvider>
  )
}

export default App
