/**
 * База данных комментариев для каждого поста
 * Каждый пост имеет уникальный набор комментариев
 */

import { getAllUsers } from "./usersData"

const commentTexts = [
  "Замечательное мероприятие! Обязательно приду",
  "Отлично! Как раз искал что-то подобное",
  "Жаль, что я в другом городе. Удачи вам!",
  "Супер идея! Можно с другом?",
  "Интересно, а для новичков подойдет?",
  "Записался! Жду с нетерпением",
  "Какой крутой формат! Спасибо что организовали",
  "Уже второй раз участвую, всем рекомендую!",
  "А можно будет задавать вопросы?",
  "Отличная возможность для знакомства с единомышленниками",
  "Места еще есть? Очень хочу попасть!",
  "Давно мечтал попробовать, это мой шанс",
  "Звучит потрясающе! Беру с собой камеру",
  "Кто еще идет? Давайте познакомимся заранее",
  "Гениально! Просто то что мне нужно",
]

const replyTexts = [
  "Приходи обязательно! Будем рады",
  "Конечно, приводи кого угодно!",
  "Да, для любого уровня подготовки",
  "Еще есть места, успевай записаться",
  "Отлично! Увидимся там)",
  "Спасибо за интерес! Ждем тебя",
  "Конечно можно задавать вопросы!",
  "Супер, вместе веселее!",
  "Да, подходит для начинающих",
  "Давай, будет здорово!",
]

// Генерация комментариев для поста
const generateCommentsForPost = (postId, postAuthorId, postTime) => {
  const users = getAllUsers()
  const numComments = Math.floor(Math.random() * 5) + 2 // 2-6 комментариев
  const comments = []

  let postHours = 0
  if (postTime.includes("ч")) {
    postHours = Number.parseInt(postTime)
  } else if (postTime.includes("мин")) {
    postHours = 0
  }

  for (let i = 0; i < numComments; i++) {
    // Выбираем случайного пользователя (не автора поста)
    const availableUsers = users.filter((u) => u.id !== postAuthorId)
    const commentAuthor = availableUsers[Math.floor(Math.random() * availableUsers.length)]

    const commentText = commentTexts[Math.floor(Math.random() * commentTexts.length)]
    const hasReplies = Math.random() > 0.6 // 40% комментариев имеют ответы

    let commentTime
    if (postHours === 0) {
      // Если пост недавний (в минутах), комментарии тоже недавние
      commentTime = `${Math.floor(Math.random() * 50) + 5} мин`
    } else if (postHours < 3) {
      // Если пост 1-2 часа назад, комментарии от минут до часов
      const useMinutes = Math.random() > 0.5
      commentTime = useMinutes ? `${Math.floor(Math.random() * 50) + 10} мин` : `${Math.floor(Math.random() * 2) + 1} ч`
    } else {
      // Если пост старше, комментарии от нескольких часов до чуть меньше времени поста
      const maxHours = Math.max(1, postHours - 1)
      commentTime = `${Math.floor(Math.random() * maxHours) + 1} ч`
    }

    const comment = {
      id: `${postId}-${i + 1}`,
      userId: commentAuthor.id,
      author: {
        name: commentAuthor.name,
        avatar: commentAuthor.avatar,
        rating: commentAuthor.rating,
      },
      text: commentText,
      time: commentTime,
      likes: Math.floor(Math.random() * 10),
      isLiked: false,
      commentCount: 0,
      replies: [],
    }

    // Генерация ответов
    if (hasReplies) {
      const numReplies = Math.floor(Math.random() * 3) + 1 // 1-3 ответа
      for (let j = 0; j < numReplies; j++) {
        const replyAuthor = users[Math.floor(Math.random() * users.length)]
        const replyText = replyTexts[Math.floor(Math.random() * replyTexts.length)]

        const replyMinutes = Math.floor(Math.random() * 50) + 5

        comment.replies.push({
          id: `${postId}-${i + 1}-${j + 1}`,
          userId: replyAuthor.id,
          author: {
            name: replyAuthor.name,
            avatar: replyAuthor.avatar,
            rating: replyAuthor.rating,
          },
          text: replyText,
          time: `${replyMinutes} мин`,
          likes: Math.floor(Math.random() * 5),
          isLiked: false,
          commentCount: 0,
        })
      }
      comment.commentCount = comment.replies.length
    }

    comments.push(comment)
  }

  return comments
}

// Кэш комментариев для каждого поста
const commentsCache = {}

// Получить комментарии для конкретного поста
export const getCommentsForPost = (postId, postAuthorId, postTime) => {
  if (!commentsCache[postId]) {
    commentsCache[postId] = generateCommentsForPost(postId, postAuthorId, postTime)
  }
  return commentsCache[postId]
}
