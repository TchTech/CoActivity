"use client"

import { UserProvider } from "../../context/UserContext"

export default function ResetPasswordLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <UserProvider>
      {children}
    </UserProvider>
  )
}
