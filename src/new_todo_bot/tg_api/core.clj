(ns new-todo-bot.tg-api.core)

(defn new-keyboard-button
  []
{:text "test" :callback_data "asdf"})


(defn new-keyboard
  []
  [[(new-keyboard-button) (new-keyboard-button) (new-keyboard-button)]
   [(new-keyboard-button) (new-keyboard-button) (new-keyboard-button)]
   [(new-keyboard-button) (new-keyboard-button) (new-keyboard-button)]])
