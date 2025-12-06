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
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
      {
        source: '/auth/:path*',
        destination: 'http://localhost:8080/auth/:path*',
      },
      {
        source: '/users/:path*',
        destination: 'http://localhost:8080/users/:path*',
      },
      {
        source: '/posts/:path*',
        destination: 'http://localhost:8080/posts/:path*',
      },
      {
        source: '/images/:path*',
        destination: 'http://localhost:8080/images/:path*',
      },
      {
        source: '/password-reset/:path*',
        destination: 'http://localhost:8080/password-reset/:path*',
      },
      {
        source: '/email-verification/:path*',
        destination: 'http://localhost:8080/email-verification/:path*',
      },
    ]
  },
}

export default nextConfig
