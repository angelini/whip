(ns whip.base.layout
  (:require [clojure.math.numeric-tower :as math]
            [clojure.algo.generic.functor :refer (fmap)]
            [schema.core :as s]
            [schema.macros :as sm]))

(def Color (s/enum :black
                   :white))

(sm/defrecord Cell
  [c :- Character
   fg :- Color
   bg :- Color])

(sm/defrecord Buffer
  [id :- s/Int
   name :- s/Str
   content :- [[Cell]]
   meta :- {}])

(sm/defrecord Pane
  [id :- s/Int
   width :- s/Int
   height :- s/Int
   buffer :- s/Int
   buf-x :- s/Int
   buf-y :- s/Int])

(sm/defrecord Window
  [id :- s/Int
   name :- s/Str
   width :- s/Int
   height :- s/Int
   locs :- {s/Int {:x s/Int
                   :y s/Int}}])

(def buffer-id (atom 0))
(def pane-id (atom 0))
(def window-id (atom 0))

(defn create-buffer
  ([] (create-buffer "-- buffer --"))
  ([name] (map->Buffer {:id (swap! buffer-id inc)
                        :name name
                        :content [[]]})))

(defn create-pane [buffer width height]
  (map->Pane {:id (swap! pane-id inc)
              :width width
              :height height
              :buffer buffer
              :buf-x 0 :buf-y 0}))

(defn create-window
  ([pane w h] (create-window pane w h "-- window --"))
  ([pane w h name] (map->Window {:id (swap! window-id inc)
                                 :name name
                                 :width w
                                 :height h
                                 :locs {pane {:x 0 :y 0}}})))

(defn visible-cols [row start end]
  (let [col-len (count row)
        first-col (min col-len start)
        last-col (min col-len end)]
    (subvec row first-col last-col)))

(defn in-pane? [loc pane x y]
  (and (<= (:x loc) x (+ (:x loc) (:width pane)))
       (<= (:y loc) y (+ (:y loc) (:height pane)))))

(defn resize-loc [loc [n-width n-height] [o-width o-height]]
  (let [x-perc (/ (:x loc) o-width)
        y-perc (/ (:y loc) o-height)]
    (assoc loc :x (int (math/round (* n-width x-perc)))
               :y (int (math/round (* n-height y-perc))))))

(defn resize-pane [pane [n-width n-height] [o-width o-height]]
  (let [w-perc (/ (:width pane) o-width)
        h-perc (/ (:height pane) o-height)]
    (assoc pane :width (int (math/round (* n-width w-perc)))
                :height (int (math/round (* n-height h-perc))))))

(sm/defn strings->cells :- [[Cell]]
  "Transforms an array of strings into a matrix of cells"
  [strs :- [s/Str]
   fg :- Color
   bg :- Color]
  (vec (map (fn [row]
              (vec (map #(map->Cell {:c %
                                     :fg fg
                                     :bg bg})
                        row)))
            strs)))

(sm/defn cells->strings :- [s/Str]
  "Transforms a matrix of cells into an array of strings"
  [cells :- [[Cell]]]
  (vec (map (fn [row]
              (apply str (map :c row)))
            cells)))

(sm/defn visible-content :- [[Cell]]
  "Returns the content of the buffer visible within the pane"
  [pane :- Pane
   buffer :- Buffer]
  (let [{:keys [width height buf-x buf-y]} pane
        content (:content buffer)
        row-len (count content)
        first-row (min row-len buf-y)
        last-row (min row-len (+ buf-y height))]
    (map (fn [r] (visible-cols r buf-x (+ buf-x width)))
         (subvec content first-row last-row))))

(sm/defn pane-at :- (s/maybe s/Int)
  "Returns the ID of the pane at x and y"
  [window :- Window
   panes :- {s/Int Pane}
   x :- s/Int
   y :- s/Int]
  (loop [[[pane-id loc] & locs] (seq (:locs window))]
    (if (or (nil? loc)
            (in-pane? loc (get panes pane-id) x y))
      pane-id
      (recur locs))))

(sm/defn insert-at :- Buffer
  "Returns a new buffer with char c at x and y"
  [buffer :- Buffer
   c :- Character
   x :- s/Int
   y :- s/Int]
  buffer)

(sm/defn resize
  "Returns a new window and panes with widths and heights adjusted to scale"
  [window :- Window
   panes :- {s/Int Pane}
   n-width :- s/Int
   n-height :- s/Int]
  (let [{:keys [width height locs]} window
        n-size [n-width n-height]
        o-size [width height]
        pane-ids (keys locs)
        rpanes (fmap (fn [pane]
                       (if (>= (.indexOf pane-ids (:id pane)) 0)
                         (resize-pane pane n-size o-size)
                         pane))
                     panes)
        rlocs (fmap #(resize-loc % n-size o-size) locs)]
    [(assoc window :width n-width
                   :height n-height
                   :locs rlocs)
     rpanes]))
