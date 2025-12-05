"use client"

import { Suspense } from "react"
import { useSearchParams } from "next/navigation"
import VerifyEmail from "../../hypertexts/VerifyEmail"

function VerifyEmailContent() {
  const searchParams = useSearchParams()
  const token = searchParams.get("token")

  return <VerifyEmail token={token || undefined} />
}

export default function VerifyEmailWrapper() {
  return (
    <Suspense fallback={
      <div style={{ 
        display: "flex", 
        justifyContent: "center", 
        alignItems: "center", 
        minHeight: "100vh",
        fontFamily: "Roboto, sans-serif"
      }}>
        <div>Загрузка токена...</div>
      </div>
    }>
      <VerifyEmailContent />
    </Suspense>
  )
}

