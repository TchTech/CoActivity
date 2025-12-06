import type React from "react"
import type { Metadata } from "next"
import "./globals.css" // Import globals.css at the top of the file

export const metadata: Metadata = {
  title: "CoActivity - Найди единомышленников",
  description: "Социальная сеть для создания комнат по интересам и совместных активностей",
  generator: 'v0.app',
  icons: {
    icon: '/Co_p9.ico',
    shortcut: '/Co_p9.ico',
    apple: '/Co_p9.ico',
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ru" suppressHydrationWarning>
      <head>
        <link
          href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap"
          rel="stylesheet"
        />
      </head>
      <body suppressHydrationWarning>{children}</body>
    </html>
  )
}
