"use client"

import { useState } from "react"
import { UserProvider } from "./context/UserContext"
import Login from "./hypertexts/Login"
import Register from "./hypertexts/Register"
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

/**
 * Главный компонент приложения
 * Переключение между страницами
 */
function App() {
  const [currentPage, setCurrentPage] = useState("login")
  const [viewingUserId, setViewingUserId] = useState(null)
  const [viewingPostId, setViewingPostId] = useState(null)
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
      case "home":
        return <Feed onNavigate={handleNavigate} />
      case "profile":
        return <Profile onNavigate={handleNavigate} userId={viewingUserId} />
      case "comments":
        return <Comments onNavigate={handleNavigate} postId={viewingPostId} />
      case "rooms":
        return <RoomsList onNavigate={handleNavigate} />
      case "chat":
        return <Chat onNavigate={handleNavigate} roomId={viewingRoomId} />
      case "roomInfo":
        return <RoomInfo onNavigate={handleNavigate} roomId={viewingRoomId} />
      case "createPost":
        return <CreatePost onNavigate={handleNavigate} />
      case "createRoom":
        return <CreateRoom onNavigate={handleNavigate} />
      case "settings":
        return <Settings onNavigate={handleNavigate} />
      case "notifications":
        return <Notifications onNavigate={handleNavigate} />
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
