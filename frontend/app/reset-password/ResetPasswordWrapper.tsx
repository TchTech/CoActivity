"use client"

import { Suspense } from "react"
import { useSearchParams } from "next/navigation"
import ResetPassword from "../../hypertexts/ResetPassword"

function ResetPasswordContent() {
  const searchParams = useSearchParams()
  const token = searchParams.get("token")

  return <ResetPassword token={token || undefined} />
}

export default function ResetPasswordWrapper() {
  return (
    <Suspense fallback={
      <div style={{ 
        display: "flex", 
        justifyContent: "center", 
        alignItems: "center", 
        minHeight: "100vh",
        fontFamily: "Roboto, sans-serif"
      }}>
        <div>Загрузка...</div>
      </div>
    }>
      <ResetPasswordContent />
    </Suspense>
  )
}

