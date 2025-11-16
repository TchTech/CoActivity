import Header from "./Header"
import BottomNavigation from "./BottomNavigation"
import PostCard from "./PostCard"
import Comment from "./Comment"
import "../styles/global.css"
import "../styles/comments.css"

/**
 * Страница с постом и комментариями
 */
function CommentsPage() {
  // Данные поста
  const post = {
    id: 1,
    authorName: "Петр Сидоров",
    title: "Собрание физиков-механиков в Обсерватории Саратова",
    description:
      "Уникальная возможность присоединиться к научному сообществу! Живые дискуссии, свежие исследования, неформальное общение. Только здесь - звёздное небо + продвинутая механика! 🔭 9 Ноября - 12:00 ⏰, г. Саратов, ул. Ленина, д. 17 Приходи за знаниями, останься за открытиями! Места ограничены — регистрируйся сейчас! Тел: 8-926-277-12-99",
    imageUrl: "https://hebbkx1anhila5yf.public.blob.vercel-storage.com/image-BMTnbKAlMQ1XVn1iPlu89sDh5FpEAn.png",
    likes: 18,
    comments: 4,
  }

  // Данные комментариев
  const comments = [
    {
      id: 1,
      authorName: "Сергей Миколенко",
      rating: 8.0,
      time: "3 ч",
      text: "Замечательное мероприятие. Жаль, что я химик, а не физик((",
      likes: 2,
      dislikes: 2,
    },
  ]

  // Ответы на комментарии
  const replies = [
    {
      id: 1,
      authorName: "Петр Сидоров",
      rating: 8.5,
      time: "25 мин",
      text: "Ничего страшного, мы рады всем. Приходите обязательно, Сергей)",
    },
    {
      id: 2,
      authorName: "Анатолий Семин",
      rating: 7.1,
      time: "8 мин",
      text: "Да, Серега, подлетайю. Похихичим!",
    },
  ]

  const singleComment = {
    id: 2,
    authorName: "Анатолий Семин",
    rating: 7.5,
    time: "8 мин",
    text: "Отлично! Как раз думал, чем заняться накануне. Большой науке - дорога!",
    likes: 6,
    dislikes: 0,
  }

  return (
    <div className="app">
      <Header />

      <main className="comments-page">
        {/* Пост */}
        <PostCard post={post} />

        {/* Комментарии */}
        <section className="comments-section">
          <h2 className="comments-title">Комментарии</h2>

          <Comment comment={comments[0]} replies={replies} />
          <Comment comment={singleComment} />
        </section>
      </main>

      <BottomNavigation />
    </div>
  )
}

export default CommentsPage
