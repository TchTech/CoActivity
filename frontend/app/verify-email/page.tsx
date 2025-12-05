"use client"

import { Suspense } from "react"
import VerifyEmailWrapper from "./VerifyEmailWrapper"

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

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<LoadingFallback />}>
      <VerifyEmailWrapper />
    </Suspense>
  )
}

