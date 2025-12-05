"use client"

import { useState } from "react"
import BottomNavigation from "./BottomNavigation"
import { roomAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
import AuthAlert from "../components/AuthAlert"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/create.css"
import "../styles/navigation.css"

function CreateRoom({ onNavigate, currentPage }) {
  const { currentUser } = useUser()
  const [formData, setFormData] = useState({
    name: "",
    category: "",
    description: "",
    format: "offline",
    type: "open",
    date: "",
    time: "",
    city: "",
    address: "",
    maxMembers: 20,
  })
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState({ visible: false, message: "", type: "error" })

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
    // Сбрасываем кастомное сообщение валидации при изменении поля
    if (e.target.setCustomValidity) {
      e.target.setCustomValidity("")
    }
  }
  
  // Обработчик для кастомных сообщений валидации
  const handleInvalid = (e) => {
    e.preventDefault()
    const field = e.target
    if (field.validity.valueMissing) {
      setAlert({ visible: true, message: "Заполните поля", type: "error" })
      field.setCustomValidity("Заполните поля")
    } else {
      field.setCustomValidity("")
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!currentUser?.id) {
      setAlert({ visible: true, message: "Необходимо войти в систему", type: "error" })
      return
    }

    // Валидация обязательных полей - проверяем каждое поле отдельно
    if (!formData.name || formData.name.trim() === "") {
      setAlert({ visible: true, message: "Заполните поля", type: "error" })
      return
    }
    
    if (!formData.category || formData.category.trim() === "") {
      setAlert({ visible: true, message: "Заполните поля", type: "error" })
      return
    }
    
    if (!formData.description || formData.description.trim() === "") {
      setAlert({ visible: true, message: "Заполните поля", type: "error" })
      return
    }
    
    if (!formData.date) {
      setAlert({ visible: true, message: "Заполните поля", type: "error" })
      return
    }
    
    if (!formData.time) {
      setAlert({ visible: true, message: "Заполните поля", type: "error" })
      return
    }
    
    if (!formData.maxMembers || formData.maxMembers < 2) {
      setAlert({ visible: true, message: "Заполните поля", type: "error" })
      return
    }

    if (formData.name.length > 50) {
      setAlert({ visible: true, message: "Название не должно превышать 50 символов", type: "error" })
      return
    }

    if (formData.description.length > 500) {
      setAlert({ visible: true, message: "Описание не должно превышать 500 символов", type: "error" })
      return
    }
    
    if (formData.format === "online" && formData.maxMembers > 20) {
      setAlert({ visible: true, message: "Для онлайн формата максимум 20 участников", type: "error" })
      return
    }

    if (formData.format === "offline") {
      if (!formData.city || formData.city.trim() === "") {
        setAlert({ visible: true, message: "Заполните поля", type: "error" })
        return
      }
      if (!formData.address || formData.address.trim() === "") {
        setAlert({ visible: true, message: "Заполните поля", type: "error" })
        return
      }
    }

    setLoading(true)
    setAlert({ visible: false, message: "", type: "error" })

    try {
      const meetingDateTime = new Date(
        `${formData.date}T${formData.time || "00:00"}:00`
      ).toISOString()

      const payload = {
        description: formData.description,
        category: formData.category,
        maxCollaborators: parseInt(formData.maxMembers, 10) || null,
        meetingTime: meetingDateTime,
        meetingType: formData.format === "online" ? "online" : "offline",
        location:
          formData.format === "offline"
            ? `${formData.city}${formData.address ? ", " + formData.address : ""}`
            : null,
        joinType: formData.type === "open" ? "open" : "application",
      }

      const createdRoom = await roomAPI.create(currentUser.id, payload)
      console.log("Комната создана:", createdRoom)
      onNavigate("rooms")
    } catch (err) {
      console.error("Ошибка создания комнаты:", err)
      setAlert({ visible: true, message: err.message || "Ошибка при создании комнаты", type: "error" })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      {/* Верхняя навигация */}
      <div className="top-nav">
        <button className="btn-icon" onClick={() => onNavigate("rooms")}>
          ←
        </button>
        <div className="top-nav-title">Создать комнату</div>
        <div style={{ width: "40px" }}></div>
      </div>

      <div className="create-container">
        <AuthAlert 
          type={alert.type} 
          message={alert.message} 
          visible={alert.visible} 
          onClose={() => setAlert({ visible: false, message: "", type: "error" })} 
        />
        <form className="create-form" onSubmit={handleSubmit}>
          {/* Основная информация */}
          <div className="form-section">
            <div className="form-section-title">Основная информация</div>

            <div className="input-group">
              <label className="input-label">Название комнаты *</label>
              <input
                type="text"
                name="name"
                className="input"
                placeholder="Например: Физика-механика в Саратове"
                value={formData.name}
                onChange={handleChange}
                onInvalid={handleInvalid}
                maxLength="50"
                required
              />
              <div className="char-counter">{formData.name.length}/50</div>
            </div>

            <div className="input-group">
              <label className="input-label">Категория (увлечение) *</label>
              <select name="category" className="input" value={formData.category} onChange={handleChange} onInvalid={handleInvalid} required>
                <option value="">Выберите категорию</option>
                <option value="science">Наука</option>
                <option value="it">Программирование</option>
                <option value="sport">Спорт</option>
                <option value="art">Искусство</option>
                <option value="music">Музыка</option>
                <option value="books">Книги</option>
                <option value="travel">Путешествия</option>
                <option value="cooking">Кулинария</option>
                <option value="photo">Фотография</option>
                <option value="games">Игры</option>
              </select>
            </div>

            <div className="input-group">
              <label className="input-label">Описание *</label>
              <textarea
                name="description"
                className="input textarea"
                placeholder="Расскажите о вашей комнате... (до 500 символов)"
                value={formData.description}
                onChange={handleChange}
                onInvalid={handleInvalid}
                maxLength="500"
                rows="5"
                required
              />
              <div className="char-counter">{formData.description.length}/500</div>
            </div>
          </div>

          {/* Дата и время */}
          <div className="form-section">
            <div className="form-section-title">Дата и время</div>

            <div className="input-group">
              <label className="input-label">Дата проведения *</label>
              <input type="date" name="date" className="input" value={formData.date} onChange={handleChange} onInvalid={handleInvalid} required />
            </div>

            <div className="input-group">
              <label className="input-label">Время проведения *</label>
              <input type="time" name="time" className="input" value={formData.time} onChange={handleChange} onInvalid={handleInvalid} required />
            </div>
          </div>

          {/* Формат */}
          <div className="form-section">
            <div className="form-section-title">Формат проведения</div>

            <div className="radio-group">
              <div className="radio-option">
                <input
                  type="radio"
                  id="offline"
                  name="format"
                  value="offline"
                  checked={formData.format === "offline"}
                  onChange={handleChange}
                />
                <label htmlFor="offline" className="radio-label">
                  Очно
                </label>
              </div>
              <div className="radio-option">
                <input
                  type="radio"
                  id="online"
                  name="format"
                  value="online"
                  checked={formData.format === "online"}
                  onChange={handleChange}
                />
                <label htmlFor="online" className="radio-label">
                  Онлайн
                </label>
              </div>
            </div>

            {formData.format === "offline" ? (
              <>
                <div className="input-group">
                  <label className="input-label">Город *</label>
                  <input
                    type="text"
                    name="city"
                    className="input"
                    placeholder="Ваш город"
                    value={formData.city}
                    onChange={handleChange}
                    onInvalid={handleInvalid}
                    required
                  />
                </div>
                <div className="input-group">
                  <label className="input-label">Адрес *</label>
                  <input
                    type="text"
                    name="address"
                    className="input"
                    placeholder="Укажите точный адрес"
                    value={formData.address}
                    onChange={handleChange}
                    onInvalid={handleInvalid}
                    required
                  />
                </div>
                <div className="input-group">
                  <label className="input-label">Максимальное количество участников *</label>
                  <input
                    type="number"
                    name="maxMembers"
                    className="input"
                    value={formData.maxMembers}
                    onChange={handleChange}
                    onInvalid={handleInvalid}
                    min="2"
                    required
                  />
                </div>
              </>
            ) : (
              <div className="input-group">
                <label className="input-label">Максимальное количество участников *</label>
                <input
                  type="number"
                  name="maxMembers"
                  className="input"
                  value={formData.maxMembers}
                  onChange={handleChange}
                  onInvalid={handleInvalid}
                  min="2"
                  max="20"
                  required
                />
                <div
                  style={{
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-muted)",
                    marginTop: "var(--spacing-xs)",
                  }}
                >
                  Для онлайн формата максимум 20 человек
                </div>
              </div>
            )}
          </div>

          {/* Тип набора */}
          <div className="form-section">
            <div className="form-section-title">Тип набора участников</div>

            <div className="radio-group">
              <div className="radio-option">
                <input
                  type="radio"
                  id="open"
                  name="type"
                  value="open"
                  checked={formData.type === "open"}
                  onChange={handleChange}
                />
                <label htmlFor="open" className="radio-label">
                  Открытая
                </label>
              </div>
              <div className="radio-option">
                <input
                  type="radio"
                  id="request"
                  name="type"
                  value="request"
                  checked={formData.type === "request"}
                  onChange={handleChange}
                />
                <label htmlFor="request" className="radio-label">
                  По заявкам
                </label>
              </div>
            </div>

            <div
              style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)", marginTop: "var(--spacing-sm)" }}
            >
              {formData.type === "open"
                ? "Любой может присоединиться к комнате в один клик"
                : "Вы будете одобрять или отклонять заявки на вступление"}
            </div>
          </div>

          {/* Кнопки действий */}
          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={() => onNavigate("rooms")}>
              Отмена
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? "Создание..." : "Создать комнату"}
            </button>
          </div>
        </form>
      </div>

      <BottomNavigation currentPage={currentPage || "createRoom"} onNavigate={onNavigate} />
    </div>
  )
}

export default CreateRoom
