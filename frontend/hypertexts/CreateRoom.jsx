"use client"

import { useState } from "react"
import BottomNavigation from "./BottomNavigation"
import { roomAPI } from "../lib/api"
import { useUser } from "../context/UserContext"
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
    endDate: "",
    endTime: "",
    city: "Саратов",
    address: "",
    maxMembers: 20,
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!currentUser?.id) {
      setError("Необходимо войти в систему")
      return
    }

    // Валидация обязательных полей
    if (!formData.name || !formData.category || !formData.description || !formData.date || !formData.time) {
      setError("Пожалуйста, заполните все обязательные поля")
      return
    }

    if (formData.name.length > 50) {
      setError("Название не должно превышать 50 символов")
      return
    }

    if (formData.description.length > 500) {
      setError("Описание не должно превышать 500 символов")
      return
    }

    if (formData.format === "offline" && !formData.address) {
      setError("Укажите адрес для очного формата")
      return
    }

    // Валидация дат
    const now = new Date()
    const meetingDateTime = new Date(`${formData.date}T${formData.time || "00:00"}:00`)
    
    if (meetingDateTime <= now) {
      setError("Дата проведения должна быть в будущем")
      return
    }

    if (formData.endDate && formData.endTime) {
      const endDateTime = new Date(`${formData.endDate}T${formData.endTime || "00:00"}:00`)
      
      if (endDateTime <= now) {
        setError("Дата окончания должна быть в будущем")
        return
      }
      
      if (endDateTime <= meetingDateTime) {
        setError("Дата окончания должна быть позже даты проведения")
        return
      }
    }

    setLoading(true)
    setError("")

    try {
      const meetingDateTime = new Date(
        `${formData.date}T${formData.time || "00:00"}:00`
      ).toISOString()

      const endDateTime = formData.endDate && formData.endTime
        ? new Date(`${formData.endDate}T${formData.endTime || "00:00"}:00`).toISOString()
        : null

      const payload = {
        description: formData.description,
        category: formData.category,
        maxCollaborators: parseInt(formData.maxMembers, 10) || null,
        meetingTime: meetingDateTime,
        endTime: endDateTime,
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
      setError(err.message || "Ошибка при создании комнаты")
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
        {error && <div className="error-message" style={{ marginBottom: "var(--spacing-md)" }}>{error}</div>}
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
                maxLength="50"
              />
              <div className="char-counter">{formData.name.length}/50</div>
            </div>

            <div className="input-group">
              <label className="input-label">Категория (увлечение) *</label>
              <select name="category" className="input" value={formData.category} onChange={handleChange}>
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
                maxLength="500"
                rows="5"
              />
              <div className="char-counter">{formData.description.length}/500</div>
            </div>
          </div>

          {/* Дата и время */}
          <div className="form-section">
            <div className="form-section-title">Дата и время</div>

            <div className="input-group">
              <label className="input-label">Дата проведения *</label>
              <input type="date" name="date" className="input" value={formData.date} onChange={handleChange} />
            </div>

            <div className="input-group">
              <label className="input-label">Время проведения *</label>
              <input type="time" name="time" className="input" value={formData.time} onChange={handleChange} />
            </div>

            <div className="input-group">
              <label className="input-label">Дата окончания (необязательно)</label>
              <input 
                type="date" 
                name="endDate" 
                className="input" 
                value={formData.endDate} 
                onChange={handleChange}
                min={formData.date || new Date().toISOString().split('T')[0]}
              />
              <div style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)", marginTop: "var(--spacing-xs)" }}>
                Комната будет удалена через сутки после этой даты
              </div>
            </div>

            {formData.endDate && (
              <div className="input-group">
                <label className="input-label">Время окончания</label>
                <input type="time" name="endTime" className="input" value={formData.endTime} onChange={handleChange} />
              </div>
            )}
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
                  <label className="input-label">Город</label>
                  <input
                    type="text"
                    name="city"
                    className="input"
                    placeholder="Ваш город"
                    value={formData.city}
                    onChange={handleChange}
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
                  />
                </div>
              </>
            ) : (
              <div className="input-group">
                <label className="input-label">Максимальное количество участников</label>
                <input
                  type="number"
                  name="maxMembers"
                  className="input"
                  value={formData.maxMembers}
                  onChange={handleChange}
                  min="2"
                  max="20"
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
