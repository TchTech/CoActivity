"use client"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "./ui/alert-dialog"

/**
 * Компонент диалога подтверждения удаления
 * @param {boolean} open - открыт ли диалог
 * @param {function} onOpenChange - функция изменения состояния открытия
 * @param {function} onConfirm - функция подтверждения удаления
 * @param {string} title - заголовок диалога
 * @param {string} description - описание действия
 */
export function ConfirmDeleteDialog({ open, onOpenChange, onConfirm, title = "Удалить пост?", description = "Вы уверены, что хотите удалить этот пост? Это действие нельзя отменить." }) {
  const handleConfirm = (e) => {
    // Предотвращаем всплытие события
    if (e) {
      e.stopPropagation()
    }
    onConfirm(e)
    onOpenChange(false)
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent
        style={{
          backgroundColor: "var(--bg-secondary)",
          borderColor: "var(--border-color)",
          color: "var(--text-primary)",
        }}
      >
        <AlertDialogHeader>
          <AlertDialogTitle
            style={{
              color: "var(--text-primary)",
              fontSize: "var(--font-size-xl)",
              fontWeight: 600,
            }}
          >
            {title}
          </AlertDialogTitle>
          <AlertDialogDescription
            style={{
              color: "var(--text-secondary)",
              fontSize: "var(--font-size-base)",
              marginTop: "var(--spacing-sm)",
            }}
          >
            {description}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter style={{ marginTop: "var(--spacing-lg)", gap: "var(--spacing-md)" }}>
          <AlertDialogCancel
            style={{
              backgroundColor: "var(--bg-tertiary)",
              color: "var(--text-primary)",
              border: "1px solid var(--border-color)",
              padding: "var(--spacing-sm) var(--spacing-lg)",
              borderRadius: "var(--radius-md)",
              cursor: "pointer",
              fontSize: "var(--font-size-base)",
              fontWeight: 500,
              transition: "all 0.2s ease",
            }}
            onMouseEnter={(e) => {
              e.target.style.backgroundColor = "var(--bg-hover)"
            }}
            onMouseLeave={(e) => {
              e.target.style.backgroundColor = "var(--bg-tertiary)"
            }}
          >
            Отмена
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleConfirm}
            style={{
              backgroundColor: "var(--error-color)",
              color: "#fff",
              border: "none",
              padding: "var(--spacing-sm) var(--spacing-lg)",
              borderRadius: "var(--radius-md)",
              cursor: "pointer",
              fontSize: "var(--font-size-base)",
              fontWeight: 600,
              transition: "all 0.2s ease",
            }}
            onMouseEnter={(e) => {
              e.target.style.backgroundColor = "#d32f2f"
            }}
            onMouseLeave={(e) => {
              e.target.style.backgroundColor = "var(--error-color)"
            }}
          >
            Удалить
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

