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

(defn switch-pane [x-inc y-inc]
  (with-window-panes-cursor (fn [window panes cursor]
    (loop [x (:x cursor)
           y (:y cursor)]
      (let [{:keys [id] :as pane} (pane-at window panes x y)]
        (cond (nil? pane) [window panes cursor]
              (not= id (:pane cursor)) [window panes (assoc cursor :x x
                                                                   :y y
                                                                   :pane id)]
              :else (recur (+ x x-inc) (+ y y-inc))))))))

(defn print-miss [c]
  (fn [state]
    (do
      (println "Unknown char: " c)
      state)))

(defn translate [k]
  (match [(:c k) (:ctrl? k) (:alt? k)]
    [:up true _] (switch-pane 0 -1)
    [:down true _] (switch-pane 0 1)
    [:left true _] (switch-pane -1 0)
    [:right true _] (switch-pane 1 0)
    [:up false _] (move-cursor 0 -1)
    [:down false _] (move-cursor 0 1)
    [:left false _] (move-cursor -1 0)
    [:right false _] (move-cursor 1 0)
    [\v _ _] (split-vertical)
    [\h _ _] (split-horizontal)
    :else (print-miss k)))
