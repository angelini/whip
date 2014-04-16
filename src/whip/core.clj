(ns whip.core
  (:require [whip.layout :refer :all]
            [whip.display :refer :all]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [schema.macros :as sm])
  (:import [whip.layout Buffer Pane PaneLocation Window]))

(sm/defrecord Cursor
  [x :- s/Int
   y :- s/Int
   pane :- s/Int
   window :- s/Int])

(sm/defrecord State
  [cursor :- Cursor
   buffers :- {s/Int Buffer}
   panes :- {s/Int Pane}
   windows :- {s/Int Window}])

(defn move-cursor [x y]
  (sm/fn new-state :- State [state :- State]
    (-> state
        (update-in [:cursor :x] + x)
        (update-in [:cursor :y] + y))))

(defn split-with-border [size]
  (let [half (int (/ size 2))]
    (if (even? size)
      [half (- half 1)]
      [half half])))

(sm/defn split-vertical :- State
  [state :- State]
  (let [{:keys [windows panes cursor]} state
        {:keys [id width height buffer]} (get panes (:pane cursor))
        {:keys [x y]} (get-in windows [(:window cursor) :pane-locs id])
        [cur-width new-width] (split-with-border width)
        new-pane (create-pane new-width height buffer)
        new-loc (create-loc (:id new-pane) (+ x cur-width 1) y)]
    (-> state
        (assoc-in [:panes (:id new-pane)] new-pane)
        (assoc-in [:windows (:window cursor) :pane-locs (:id new-pane)] new-loc)
        (assoc-in [:panes id :width] cur-width))))

(sm/defn split-horizontal :- State
  [state :- State]
  (let [{:keys [windows panes cursor]} state
        {:keys [id width height buffer]} (get panes (:pane cursor))
        {:keys [x y]} (get-in windows [(:window cursor) :pane-locs id])
        [cur-height new-height] (split-with-border height)
        new-pane (create-pane width new-height buffer)
        new-loc (create-loc (:id new-pane) x (+ y cur-height 1))]
    (-> state
        (assoc-in [:panes (:id new-pane)] new-pane)
        (assoc-in [:windows (:window cursor) :pane-locs (:id new-pane)] new-loc)
        (assoc-in [:panes id :height] cur-height))))

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
    [\v] split-vertical
    [\h] split-horizontal
    :else (print-miss c)))

(defn create-system []
  (component/system-map
    :display (create-display :swing)))

(defn init-state [width height]
  (let [buffer (create-buffer)
        pane (create-pane width height (:id buffer))
        location (PaneLocation. (:id pane) 0 0)
        window (-> (create-window)
                   (assoc :pane-locs {(:pane location) location}))]
    (map->State {:cursor (Cursor. 0 0 (:id pane) (:id window))
                 :buffers {(:id buffer) buffer}
                 :panes {(:id pane) pane}
                 :windows {(:id window) window}})))

(defn draw-borders [display loc pane]
  (let [top (:y loc)
        bottom (+ top (:height pane))
        left (:x loc)
        right (+ left (:width pane))]
    (doseq [x (range left right)]
      (put-char display x (- top 1) "-")
      (put-char display x bottom "-"))
    (doseq [y (range top bottom)]
      (put-char display (- left 1) y "|")
      (put-char display right y "|"))))

(defn draw-row [display x y row]
  (map #(put-char display %1 y %2) (iterate inc x) row))

(defn draw-pane [display loc pane buffer]
  (let [{:keys [x y]} loc
        visible (visible-content pane buffer)]
    (map #(draw-row display x %1 %2) (iterate inc y) visible)))

(defn draw [display buffers panes window]
  (doseq [[pane-id loc] (:pane-locs window)
         :let [pane (get panes pane-id)]]
    (draw-borders display loc pane)
    (draw-pane display loc pane (get buffers (:buffer pane)))))

(defn main [system init-state]
  (loop [state init-state]
    (let [display (:display system)
          c (async/<!! (input-chan display))
          f (translate c)
          new-state (f state)
          {:keys [cursor buffers panes windows]} new-state
          window (get windows (:window cursor))]
      (println "state -->" new-state)
      (draw display buffers panes window)
      (set-cursor display (:x cursor) (:y cursor))
      (redraw display)
      (recur new-state))))
