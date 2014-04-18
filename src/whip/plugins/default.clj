(ns whip.plugins.default
  (:require [whip.base.layout :refer :all]
            [whip.base.state :refer :all]
            [whip.base.macros :refer :all]
            [clojure.core.match :refer [match]]))

(defn split-with-border [size]
  (let [half (int (/ size 2))]
    (if (even? size)
      [half (- half 1)]
      [half half])))

(defn move-cursor [x y]
  (with-cursor (fn [cursor]
    (-> cursor
        (update-in [:x] + x)
        (update-in [:y] + y)))))

(defn split-vertical []
  (with-window-pane-panes (fn [window pane panes]
    (let [{:keys [id width height buffer]} pane
          {:keys [x y]} (get-in window [:locs id])
          [cur-width new-width] (split-with-border width)
          new-pane (create-pane buffer new-width height)
          new-loc {:x (+ x cur-width 1) :y y}]
      [(assoc-in window [:locs (:id new-pane)] new-loc)
       (assoc pane :width cur-width)
       (assoc panes (:id new-pane) new-pane)]))))

(defn split-horizontal []
  (with-window-pane-panes (fn [window pane panes]
    (let [{:keys [id width height buffer]} pane
          {:keys [x y]} (get-in window [:locs id])
          [cur-height new-height] (split-with-border height)
          new-pane (create-pane buffer width new-height)
          new-loc {:x x :y (+ y cur-height 1)}]
      [(assoc-in window [:locs (:id new-pane)] new-loc)
       (assoc pane :height cur-height)
       (assoc panes (:id new-pane) new-pane)]))))

(defn print-miss [c]
  (fn [state]
    (do
      (println "Unknown char: " c)
      state)))

(defn translate [c]
  (match [c]
    [:up] (move-cursor 0 -1)
    [:down] (move-cursor 0 1)
    [:left] (move-cursor -1 0)
    [:right] (move-cursor 1 0)
    [\v] (split-vertical)
    [\h] (split-horizontal)
    :else (print-miss c)))
