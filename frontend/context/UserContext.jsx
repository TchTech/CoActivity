"use client"

import { createContext, useContext, useState, useEffect } from "react"
import { userAPI } from "../lib/api"

const UserContext = createContext(null)

export function UserProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [subscribedUsers, setSubscribedUsers] = useState([])

  // Загрузка текущего пользователя при монтировании
  useEffect(() => {
    // TODO: Получить ID текущего пользователя из cookies/session
    // Пока что используем заглушку
    const loadUser = async () => {
      try {
        // В реальном приложении здесь должен быть эндпоинт /users/me
        // или получение из session
        // const user = await userAPI.getCurrentUser()
        // setCurrentUser(user)
      } catch (error) {
        console.error("Ошибка загрузки пользователя:", error)
      } finally {
        setLoading(false)
      }
    }

    loadUser()
  }, [])

  const login = async (userData) => {
    setCurrentUser(userData)
    // Сохранить в localStorage для демонстрации
    if (typeof window !== "undefined") {
      localStorage.setItem("currentUser", JSON.stringify(userData))
    }
  }

  const logout = () => {
    setCurrentUser(null)
    setSubscribedUsers([])
    if (typeof window !== "undefined") {
      localStorage.removeItem("currentUser")
    }
  }

  const subscribeToUser = async (userId, userToSubscribeId) => {
    try {
      await userAPI.subscribe(userId, userToSubscribeId)
      setSubscribedUsers((prev) => [...prev, userToSubscribeId])
      return true
    } catch (error) {
      console.error("Ошибка подписки:", error)
      throw error
    }
  }

  const unsubscribeFromUser = async (userId, userToUnsubscribeId) => {
    try {
      await userAPI.unsubscribe(userId, userToUnsubscribeId)
      setSubscribedUsers((prev) => prev.filter((id) => id !== userToUnsubscribeId))
      return true
    } catch (error) {
      console.error("Ошибка отписки:", error)
      throw error
    }
  }

  // Загрузка из localStorage при монтировании
  useEffect(() => {
    if (typeof window !== "undefined") {
      const savedUser = localStorage.getItem("currentUser")
      if (savedUser) {
        try {
          setCurrentUser(JSON.parse(savedUser))
        } catch (e) {
          console.error("Ошибка парсинга сохраненного пользователя:", e)
        }
      }
      setLoading(false)
    }
  }, [])

  return (
    <UserContext.Provider
      value={{
        currentUser,
        loading,
        subscribedUsers,
        login,
        logout,
        subscribeToUser,
        unsubscribeFromUser,
        setCurrentUser,
      }}
    >
      {children}
    </UserContext.Provider>
  )
}

export function useUser() {
  const context = useContext(UserContext)
  if (!context) {
    throw new Error("useUser must be used within UserProvider")
  }
  return context
}

