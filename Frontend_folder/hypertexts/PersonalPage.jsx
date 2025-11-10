import Header from "./Header"
import BottomNavigation from "./BottomNavigation"
import PostCard from "./PostCard"
import "../styles/global.css"
import "../styles/personal-page.css"

/**
 * Компонент личной страницы пользователя
 */
function PersonalPage() {
  // Данные профиля (обычно приходят с сервера)
  const userProfile = {
    name: "Петр Сидоров",
    city: "г. Саратов",
    rating: 8.5,
    subscribers: 106,
    rooms: 12,
    subscriptions: 80,
    about:
      "Занимаюсь на кружках по программированию, в детстве играл в настольный теннис день за днем, конструировал роботов на робототехнике в 5 классе - я гений. Собрал робот-пылесос, когда старый сломался, в 16 лет выиграл олимпиаду Изумруд по информационной безопасности. Общительный, физик-механик чуть-чуть, на любительском уровне. Собираюсь с друзьями и читаю труды Альберта Эйнштейна по теории относительности.",
    interests: ["программирование", "робототехника", "физика-механика", "настольный теннис"],
  }

  // Пример поста
  const samplePost = {
    id: 1,
    authorName: "Петр Сидоров",
    title: "Собрание физиков-механиков в Обсерватории Саратова",
    description:
      "Уникальная возможность присоединиться к научному сообществу! Живые дискуссии, свежие исследования, неформальное общение. Только здесь - звёздное небо + продвинутая механика! 🔭 9 Ноября - 12:00 ⏰, г. Саратов, ул. Ленина, д. 17 Приходи за знаниями, останься за открытиями! Места ограничены — регистрируйся сейчас! Тел: 8-926-277-12-99",
    imageUrl: "https://hebbkx1anhila5yf.public.blob.vercel-storage.com/image-BMTnbKAlMQ1XVn1iPlu89sDh5FpEAn.png",
    likes: 18,
    comments: 4,
  }

  return (
    <div className="app">
      <Header />

      <main className="personal-page">
        {/* Шапка профиля */}
        <section className="profile-header">
          <div className="profile-avatar-container">
            <div className="profile-avatar">👤</div>
            <span className="profile-rating-badge">{userProfile.rating}</span>
          </div>

          <h1 className="profile-name">{userProfile.name}</h1>
          <p className="profile-city">{userProfile.city}</p>

          <button className="subscribe-button">Подписаться</button>
        </section>

        {/* Статистика */}
        <section className="profile-stats">
          <div className="stat-item">
            <div className="stat-label">Подписчики</div>
            <div className="stat-value">{userProfile.subscribers}</div>
          </div>

          <div className="stat-item">
            <div className="stat-label highlight">КОМНАТЫ</div>
            <div className="stat-value">{userProfile.rooms}</div>
          </div>

          <div className="stat-item">
            <div className="stat-label">Подписки</div>
            <div className="stat-value">{userProfile.subscriptions}</div>
          </div>
        </section>

        {/* О человеке с темами */}
        <section className="about-section">
          <h2 className="about-title">О человеке</h2>
          <p className="about-text">{userProfile.about}</p>

          <div className="interests-container">
            {userProfile.interests.map((interest, index) => (
              <span key={index} className="interest-tag">
                {interest}
              </span>
            ))}
          </div>
        </section>

        {/* Секция контента (постов) */}
        <section className="content-section">
          <h2 className="section-title">Контент</h2>
          <PostCard post={samplePost} />
        </section>
      </main>

      <BottomNavigation />
    </div>
  )
}

export default PersonalPage
