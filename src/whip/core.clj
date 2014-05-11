(ns whip.core
  (:require [whip.base.layout :refer :all]
            [whip.base.state :refer :all]
            [whip.plugins.plugin :refer :all]
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
    :loader (create-loader ["default" "insert"])))

(defn init-state [width height plugin]
  (let [buffer (create-buffer)
        pane (create-pane (:id buffer) width height)
        window (create-window (:id pane) width height)
        [wid pid bid] (map :id [window pane buffer])]
    (map->State {:buffers {bid buffer}
                 :panes {pid pane}
                 :windows {wid window}
                 :mode (map->Mode {:name "default"
                                   :plugin plugin})
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

(defn get-handler [state key-message]
  (translate (get-in state [:mode :plugin]) key-message))

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
    (if-let [{:keys [type body] :as m} (async/<!! (message-chan display))]
      (do (println "; Message" m)
          (println "; State" state)
          (recur (case type
                       :key (-> (get-handler state body)
                                (eval-handler state)
                                (draw display))
                       :size (-> (resize-handler state body)
                                 (draw display))
                       (do (println "; Unknown message" body)
                           state))))
      (component/stop display))))

(defn main [system]
  (let [{:keys [server loader]} system
        default (plugin loader "default")]
    (loop []
      (let [[in out] (async/<!! (connections-chan server))
            display (-> (create-display in out)
                        (component/start))
            {:keys [width height]} (size display)
            state (init-state width height default)]
        (async/go (main-loop display state))
        (recur)))))
