(ns whip.core
  (:require [whip.display :refer :all]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [schema.macros :as sm]))

(sm/defrecord Cursor
  [x :- s/Int
   y :- s/Int
   pane :- s/Int
   window :- s/Int])

(sm/defrecord Pane
  [id :- s/Int
   buf-x :- s/Int
   buf-y :- s/Int
   width :- s/Int
   height :- s/Int
   buffer :- s/Int])

(sm/defrecord PaneLocation
  [x :- s/Int
   y :- s/Int
   pane :- s/Int])

(sm/defrecord Window
  [id :- s/Int
   name :- s/Str
   panes :- [PaneLocation]])

(sm/defrecord State
  [cursor :- Cursor
   panes :- [Pane]
   windows :- [Window]])

(defn move-cursor [x y]
  (sm/fn new-state :- State [state :- State]
    (-> state
        (update-in [:cursor :x] + x)
        (update-in [:cursor :y] + y))))

(defn print-miss [c]
  (sm/fn new-state :- State [state :- State]
    (do
      (println "Unknown char: " c)
      state)))

(defn translate [c]
  (match [c]
    [:up] (move-cursor 0 -1)
    [:down] (move-cursor 0 1)
    [:left] (move-cursor -1 0)
    [:right] (move-cursor 1 0)
    :else (print-miss c)))

(defn create-system []
  (component/system-map
    :display (create-display :swing)))

(def state (State. (Cursor. 0 0 0 0) [] []))

(defn main [system init-state]
  (loop [state init-state]
    (let [display (:display system)
          c (async/<!! (input-chan display))
          f (translate c)
          new-state (f state)
          cursor (:cursor new-state)]
      (println "state -->" new-state)
      (set-cursor display (:x cursor) (:y cursor))
      (redraw display)
      (recur new-state))))
