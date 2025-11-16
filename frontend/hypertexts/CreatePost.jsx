"use client"

import { useState } from "react"
import BottomNavigation from "./BottomNavigation"
import "../styles/variables.css"
import "../styles/global.css"
import "../styles/components.css"
import "../styles/create.css"
import "../styles/navigation.css"

function CreatePost({ onNavigate }) {
  const [formData, setFormData] = useState({
    text: "",
    image: null,
    linkedRoom: "",
  })
  const [imagePreview, setImagePreview] = useState(null)

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const handleImageUpload = (e) => {
    const file = e.target.files[0]
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        alert("Размер файла не должен превышать 10 МБ")
        return
      }
      setFormData({ ...formData, image: file })

      const reader = new FileReader()
      reader.onloadend = () => {
        setImagePreview(reader.result)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()

    if (!formData.text.trim()) {
      alert("Пожалуйста, введите текст поста")
      return
    }

    if (formData.text.length < 1 || formData.text.length > 1000) {
      alert("Текст должен содержать от 1 до 1000 символов")
      return
    }

    console.log("Создание поста:", formData)
    onNavigate("profile")
  }

  const charCount = formData.text.length
  const charCountClass = charCount > 900 ? "error" : charCount > 800 ? "warning" : ""

  return (
    <div>
      <div className="top-nav">
        <button className="btn-icon" onClick={() => onNavigate("profile")}>
          ←
        </button>
        <div className="top-nav-title">Создать пост</div>
        <div style={{ width: "40px" }}></div>
      </div>

      <div className="create-container">
        <form className="create-form" onSubmit={handleSubmit}>
          <div className="form-section">
            <div className="form-section-title">Содержание поста</div>
            <div className="input-group">
              <label className="input-label">Текст *</label>
              <textarea
                name="text"
                className="input textarea"
                placeholder="Расскажите о чем-то интересном... (1-1000 символов)"
                value={formData.text}
                onChange={handleChange}
                maxLength="1000"
                rows="8"
              />
              <div className={`char-counter ${charCountClass}`}>{charCount}/1000</div>
            </div>
          </div>

          <div className="form-section">
            <div className="form-section-title">Изображение (необязательно)</div>
            <input
              type="file"
              id="image-upload"
              accept="image/*"
              onChange={handleImageUpload}
              style={{ display: "none" }}
            />
            <label htmlFor="image-upload" className="file-upload-area">
              {imagePreview ? (
                <div className="file-upload-preview">
                  <img src={imagePreview || "/placeholder.svg"} alt="Предпросмотр" className="image-preview-full" />
                  <div className="file-upload-overlay">
                    <svg className="file-upload-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" />
                      <polyline points="21 15 16 10 5 21" />
                    </svg>
                    <span className="file-upload-text">Нажмите для замены изображения</span>
                  </div>
                </div>
              ) : (
                <div className="file-upload-empty">
                  <svg className="file-upload-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                    <circle cx="8.5" cy="8.5" r="1.5" />
                    <polyline points="21 15 16 10 5 21" />
                  </svg>
                  <span className="file-upload-text">Нажмите, чтобы загрузить изображение</span>
                  <span className="file-upload-hint">PNG, JPG, JPEG до 10 МБ</span>
                </div>
              )}
            </label>
          </div>

          <div className="form-section">
            <div className="form-section-title">Интеграция с комнатой (необязательно)</div>
            <div className="input-group">
              <label className="input-label">Связать с комнатой</label>
              <select name="linkedRoom" className="input" value={formData.linkedRoom} onChange={handleChange}>
                <option value="">Не связывать</option>
                <option value="1">Физика-механика в Саратове</option>
                <option value="2">Программисты Москвы</option>
                <option value="3">Настольный теннис - выходные</option>
              </select>
              <div
                style={{ fontSize: "var(--font-size-sm)", color: "var(--text-muted)", marginTop: "var(--spacing-xs)" }}
              >
                Пост будет показан участникам выбранной комнаты
              </div>
            </div>
          </div>

          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={() => onNavigate("profile")}>
              Отмена
            </button>
            <button type="submit" className="btn btn-primary">
              Опубликовать
            </button>
          </div>
        </form>
      </div>

      <BottomNavigation currentPage="home" onNavigate={onNavigate} />
    </div>
  )
}

export default CreatePost
