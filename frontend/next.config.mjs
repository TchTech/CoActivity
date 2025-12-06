/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  transpilePackages: [],
  // Проксирование API запросов для обхода CORS
  async rewrites() {
    // Используем переменную окружения или fallback на localhost
    const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || process.env.BACKEND_URL || 'http://localhost:8080';
    
    return [
      {
        source: '/api/:path*',
        destination: `${backendUrl}/api/:path*`,
      },
      {
        source: '/auth/:path*',
        destination: `${backendUrl}/auth/:path*`,
      },
      {
        source: '/users/:path*',
        destination: `${backendUrl}/users/:path*`,
      },
      {
        source: '/posts/:path*',
        destination: `${backendUrl}/posts/:path*`,
      },
      {
        source: '/images/:path*',
        destination: `${backendUrl}/images/:path*`,
      },
      {
        source: '/password-reset/:path*',
        destination: `${backendUrl}/password-reset/:path*`,
      },
      {
        source: '/email-verification/:path*',
        destination: `${backendUrl}/email-verification/:path*`,
      },
    ]
  },
}

export default nextConfig
