"use client"

import { Suspense } from "react"
import ResetPasswordWrapper from "./ResetPasswordWrapper"

function LoadingFallback() {
  return (
    <div style={{ 
      display: "flex", 
      justifyContent: "center", 
      alignItems: "center", 
      minHeight: "100vh",
      fontFamily: "Roboto, sans-serif"
    }}>
      <div>Загрузка...</div>
    </div>
  )
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<LoadingFallback />}>
      <ResetPasswordWrapper />
    </Suspense>
  )
}

