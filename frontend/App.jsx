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

/**
 * Главный компонент приложения
 * Переключение между страницами
 */
function App() {
  const [currentPage, setCurrentPage] = useState("login")
  const [viewingUserId, setViewingUserId] = useState(null)
  const [viewingPostId, setViewingPostId] = useState(null)

  const handleNavigate = (page, param) => {
    console.log("[v0] Navigation:", page, "param:", param)
    setCurrentPage(page)

    if (page === "profile") {
      setViewingUserId(param || null)
    } else if (page === "comments") {
      setViewingPostId(param || null)
      console.log("[v0] Setting post ID:", param)
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
        return <Chat onNavigate={handleNavigate} />
      case "roomInfo":
        return <RoomInfo onNavigate={handleNavigate} />
      case "createPost":
        return <CreatePost onNavigate={handleNavigate} />
      case "createRoom":
        return <CreateRoom onNavigate={handleNavigate} />
      case "settings":
        return <Settings onNavigate={handleNavigate} />
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
