(ns whip.plugins.default
  (:require [whip.base.layout :refer :all]
            [whip.base.state :refer :all]
            [clojure.core.match :refer [match]]
            [schema.core :as s]
            [schema.macros :as sm])
  (:import [whip.base.state State]))

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
        {:keys [x y]} (get-in windows [(:window cursor) :locs id])
        [cur-width new-width] (split-with-border width)
        new-pane (create-pane buffer new-width height)
        new-loc {:x (+ x cur-width 1) :y y}]
    (-> state
        (assoc-in [:panes (:id new-pane)] new-pane)
        (assoc-in [:windows (:window cursor) :locs (:id new-pane)] new-loc)
        (assoc-in [:panes id :width] cur-width))))

(sm/defn split-horizontal :- State
  [state :- State]
  (let [{:keys [windows panes cursor]} state
        {:keys [id width height buffer]} (get panes (:pane cursor))
        {:keys [x y]} (get-in windows [(:window cursor) :locs id])
        [cur-height new-height] (split-with-border height)
        new-pane (create-pane buffer width new-height)
        new-loc {:x x :y (+ y cur-height 1)}]
    (-> state
        (assoc-in [:panes (:id new-pane)] new-pane)
        (assoc-in [:windows (:window cursor) :locs (:id new-pane)] new-loc)
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
