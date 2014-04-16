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

(sm/defrecord Buffer
  [id :- s/Int
   name :- s/Str
   content :- [[]]
   meta :- {}])

(sm/defrecord Pane
  [id :- s/Int
   width :- s/Int
   height :- s/Int
   buffer :- s/Int
   buf-x :- s/Int
   buf-y :- s/Int])

(sm/defrecord PaneLocation
  [pane :- s/Int
   x :- s/Int
   y :- s/Int])

(sm/defrecord Window
  [id :- s/Int
   name :- s/Str
   pane-locs :- [PaneLocation]])

(sm/defrecord State
  [wid :- s/Int
   cursor :- Cursor
   buffers :- {s/Int Buffer}
   panes :- {s/Int Pane}
   windows :- {s/Int Window}])

(defn move-cursor [x y]
  (sm/fn new-state :- State [state :- State]
    (-> state
        (update-in [:cursor :x] + x)
        (update-in [:cursor :y] + y))))

(defn insert-char [c]
  (sm/fn new-state :- State [state :- State] state))

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
    [#"a-zA-Z"] (insert-char c)
    :else (print-miss c)))

(defn create-system []
  (component/system-map
    :display (create-display :swing)))

(def buffer-id (atom 0))
(def pane-id (atom 0))
(def window-id (atom 0))

(defn create-buffer
  ([] (create-buffer "-- buffer --"))
  ([name] (map->Buffer {:id (swap! buffer-id inc)
                        :name name})))

(defn create-pane [width height buffer]
  (map->Pane {:id (swap! pane-id inc)
              :width width
              :height height
              :buffer buffer
              :buf-x 0 :buf-y 0}))

(defn create-window
  ([] (create-window "-- window --"))
  ([name] (map->Window {:id (swap! window-id inc)
                        :name name})))

(defn init-state [width height]
  (let [buffer (create-buffer)
        pane (create-pane width height (:id buffer))
        location (PaneLocation. (:id pane) 0 0)
        window (-> (create-window)
                   (assoc :pane-locs [location]))]
    (map->State {:wid (:id window)
                 :cursor (Cursor. 0 0 0 0)
                 :buffers {(:id buffer) buffer}
                 :panes {(:id pane) pane}
                 :windows {(:id window) window}})))

(defn draw-borders [display loc pane]
  (let [top (:y loc)
        bottom (+ top (:height pane))
        left (:x loc)
        right (+ left (:width pane))]
    (for [x (range left right)]
      (do (put-char display x (- top 1) "-")
          (put-char display x (+ bottom 1) "-")))
    (for [y (range top bottom)]
      (do (println "x: [" (- left 1) ", " (+ right 1) "], y: " y)
          (put-char display (- left 1) y "|")
          (put-char display (+ right 1) y "|")))))

(defn draw-pane [display loc pane buffer]
  (let [{:keys [x y]} loc
        {:keys [height width]} pane
        content (:content buffer)
        row-len (count content)
        first-row (min row-len y)
        last-row (min row-len (+ y height))]
    (for [i (range first-row last-row)
         :let [row (nth i content)
               col-len (count row)
               first-col (min col-len x)
               last-col (min col-len (+ x width))]]
      (for [j (range first-col last-col)]
        (put-char display i j (nth j row))))))

(defn draw [display buffers panes window]
  (for [loc (:pane-locs window)
        :let [pane (get panes (:pane loc))]]
    (do (draw-borders display loc pane)
        (draw-pane display loc pane (get buffers (:buffer pane))))))

(defn main [system init-state]
  (loop [state init-state]
    (let [display (:display system)
          c (async/<!! (input-chan display))
          f (translate c)
          new-state (f state)
          {:keys [wid cursor buffers panes windows]} new-state
          window (get windows wid)]
      (println "state -->" new-state)
      (draw display buffers panes window)
      (set-cursor display (:x cursor) (:y cursor))
      (redraw display)
      (recur new-state))))
