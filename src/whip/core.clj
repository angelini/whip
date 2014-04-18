(ns whip.core
  (:require [whip.base.layout :refer :all]
            [whip.base.state :refer :all]
            [whip.display :refer :all]
            [whip.loader :refer :all]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [schema.macros :as sm]))

(defn create-system []
  (component/system-map
    :display (create-display :swing)
    :loader (create-loader ["default"])))

(defn init-state [width height translate]
  (let [buffer (create-buffer)
        pane (create-pane (:id buffer) width height)
        window (create-window (:id pane))
        [wid pid bid] (map :id [window pane buffer])]
    (map->State {:buffers {bid buffer}
                 :panes {pid pane}
                 :windows {wid window}
                 :mode (map->Mode {:name "default"
                                   :translate translate})
                 :cursor (map->Cursor {:x 0
                                       :y 0
                                       :pane pid
                                       :window wid})})))

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
  (doseq [[pane-id loc] (:locs window)
         :let [pane (get panes pane-id)]]
    (draw-borders display loc pane)
    (draw-pane display loc pane (get buffers (:buffer pane)))))

(defn listen [display init-state]
  (loop [state init-state]
    (let [c (async/<!! (input-chan display))
          f ((get-in state [:mode :translate]) c)
          new-state (f state)
          {:keys [cursor buffers panes windows]} new-state
          window (get windows (:window cursor))]
      (println "state -->" new-state)
      (draw display buffers panes window)
      (set-cursor display (:x cursor) (:y cursor))
      (redraw display)
      (recur new-state))))

(defn main [system]
  (let [{:keys [display loader]} system
        [width height] (size display)
        default (plugin loader "default")
        translate ('translate (ns-map default))
        state (init-state width height translate)]
    (listen display state)))
