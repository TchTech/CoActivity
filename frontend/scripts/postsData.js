/**
 * База данных постов для ленты CoActivity
 * Генерация случайных постов на основе шаблона
 */

import { getAllUsers } from "./usersData"

const eventLocations = [
  "Обсерватория Саратова, ул. Ленина, д. 17",
  "Научный центр МГУ, Москва",
  "Эрмитаж, Санкт-Петербург",
  "Парк культуры, Казань",
  "Скалодром 'Вершина', Екатеринбург",
  "Фотостудия 'Объектив', Новосибирск",
  "Рок-клуб 'Подвал', Краснодар",
  "Йога-студия 'Гармония', Владивосток",
]

const eventImages = [
  "https://hebbkx1anhila5yf.public.blob.vercel-storage.com/%D0%91%D0%B5%D0%B7%20%D0%BD%D0%B0%D0%B7%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F-NHDfivuU2MwLfV1jt31hDGESK67x4N.jpg",
  "/scientific-laboratory.png",
  "/vibrant-art-gallery.png",
  "/rock-climbing-wall.jpg",
  "/photography-studio.png",
  "/music-concert-stage.png",
  "/yoga-meditation-room.jpg",
  "/astronomy-observatory.jpg",
]

const eventTitles = [
  "Собрание физиков-механиков в Обсерватории Саратова",
  "Мастер-класс по органической химии для начинающих",
  "Ночь астрономических наблюдений: Сатурн и Юпитер",
  "Выставка современной живописи 'Краски города'",
  "Тренировка по скалолазанию для всех уровней",
  "Фотопрогулка: Ловим золотой час в городе",
  "Джем-сейшн для гитаристов и басистов",
  "Йога на рассвете: Практика осознанности",
  "Дискуссия о квантовой физике и её применении",
  "Химические эксперименты: Безопасная наука дома",
]

const eventDescriptions = [
  "Уникальная возможность присоединиться к научному сообществу! Живые дискуссии, свежие исследования, неформальное общение.",
  "Узнайте секреты химических реакций! Проведем несколько безопасных и зрелищных экспериментов вместе.",
  "Приглашаем всех любителей космоса! Будем наблюдать планеты через профессиональные телескопы.",
  "Представляем работы молодых художников нашего города. Свободный вход, дружеская атмосфера!",
  "От новичков до профи - каждый найдет свой маршрут! Инструктор поможет улучшить технику.",
  "Научимся фотографировать в 'золотой час'. Приносите свои камеры или смартфоны!",
  "Играем вместе, импровизируем, обмениваемся опытом. Любой уровень подготовки приветствуется!",
  "Встречаем новый день с практикой йоги. Коврики предоставляются. Подходит для начинающих.",
  "Разберем основы квантовой механики простым языком. Вопросы приветствуются!",
  "Покажем безопасные опыты, которые можно повторить дома. Для детей и взрослых.",
]

// Генерация случайной даты в пределах ближайших 30 дней
const getRandomDate = () => {
  const days = Math.floor(Math.random() * 30) + 1
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toLocaleDateString("ru-RU", { day: "numeric", month: "long" })
}

// Генерация случайного времени
const getRandomTime = () => {
  const hours = Math.floor(Math.random() * 12) + 10 // 10:00 - 21:00
  const minutes = Math.random() > 0.5 ? "00" : "30"
  return `${hours}:${minutes}`
}

// Генерация телефона
const getRandomPhone = () => {
  const phone = `8-${Math.floor(Math.random() * 900) + 100}-${Math.floor(Math.random() * 900) + 100}-${Math.floor(Math.random() * 90) + 10}-${Math.floor(Math.random() * 90) + 10}`
  return phone
}

// Генерация постов
const generatePosts = () => {
  const users = getAllUsers()
  const posts = []

  for (let i = 0; i < 20; i++) {
    const user = users[Math.floor(Math.random() * users.length)]
    const titleIndex = Math.floor(Math.random() * eventTitles.length)

    const post = {
      id: i + 1,
      userId: user.id,
      author: {
        name: user.name,
        avatar: user.avatar,
        rating: user.rating,
      },
      title: eventTitles[titleIndex],
      content: `${eventDescriptions[titleIndex]} 📍 ${getRandomDate()} - ${getRandomTime()} 📍 ${eventLocations[Math.floor(Math.random() * eventLocations.length)]}. Призыв за знаниями, оставайся с открытиями! Места ограничены - регистрируйся сейчас! Тел: ${getRandomPhone()}`,
      image: eventImages[Math.floor(Math.random() * eventImages.length)],
      time: `${Math.floor(Math.random() * 24)} ч`,
      likes: Math.floor(Math.random() * 100) + 5,
      dislikes: Math.floor(Math.random() * 20) + 1,
      comments: Math.floor(Math.random() * 30) + 1,
    }

    posts.push(post)
  }

  return posts
}

export const allPosts = generatePosts()

// Получить все посты
export const getAllPosts = () => allPosts

// Получить посты по ID пользователей (подписки)
export const getPostsByUserIds = (userIds) => {
  return allPosts.filter((post) => userIds.includes(post.userId))
}

// Получить пост по ID
export const getPostById = (id) => {
  return allPosts.find((post) => post.id === id)
}

export const getPostsByUserId = (userId) => {
  return allPosts.filter((post) => post.userId === userId)
}
