"use client"

import { UserProvider } from "../../context/UserContext"

export default function VerifyEmailLayout({
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

