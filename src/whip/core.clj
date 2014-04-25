(ns whip.core
  (:require [whip.base.layout :refer :all]
            [whip.base.state :refer :all]
            [whip.server :refer :all]
            [whip.display :refer :all]
            [whip.loader :refer :all]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [schema.macros :as sm]))

(defn create-system []
  (component/system-map
    :server (create-server 8080)
    :loader (create-loader ["default"])))

(defn init-state [width height translate]
  (let [buffer (create-buffer)
        pane (create-pane (:id buffer) width height)
        window (create-window (:id pane) width height)
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
  (map #(put-cell display %1 y %2) (iterate inc x) row))

(defn draw-pane [display loc pane buffer]
  (let [{:keys [x y]} loc
        visible (visible-content pane buffer)]
    (map #(draw-row display x %1 %2) (iterate inc y) visible)))

(defn translate [state key-message]
  ((get-in state [:mode :translate]) key-message))

(defn eval-handler [f state]
  (f state))

(defn draw [state display]
  (let [{:keys [cursor buffers panes windows]} state
        window (get windows (:window cursor))]
    (set-cursor display (:x cursor) (:y cursor))
    (doseq [[pane-id loc] (:locs window)
           :let [pane (get panes pane-id)]]
      (draw-borders display loc pane)
      (draw-pane display loc pane (get buffers (:buffer pane))))
    (sync-display display)
    state))

(defn resize-handler [state size-message]
  (let [{:keys [cursor panes windows]} state
        {:keys [width height]} size-message
        window (get windows (:window cursor))
        [window panes] (resize window panes width height)]
    (-> state
        (assoc :panes panes)
        (assoc-in [:windows (:window cursor)] window))))

(defn main-loop [display init-state]
  (loop [state init-state]
    (let [{:keys [type body] :as m} (async/<!! (message-chan display))]
      (println "; Message" m)
      (recur (case type
                   :key (-> (translate state body)
                            (eval-handler state)
                            (draw display))
                   :size (-> (resize-handler state body)
                             (draw display))
                   (do (println "; Unknown message" body)
                       state))))))

(defn main [system]
  (let [{:keys [server loader]} system
        default (plugin loader "default")
        translate ('translate (ns-map default))]
    (loop []
      (let [[in out] (async/<!! (connections-chan server))
            display (-> (create-display in out)
                        (component/start))
            {:keys [width height]} (size display)
            state (init-state width height translate)]
        (async/go (main-loop display state))
        (recur)))))
