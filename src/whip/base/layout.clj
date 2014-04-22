(ns whip.base.layout
  (:require [schema.core :as s]
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
   content :- [[Character]]
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
  ([pane] (create-window pane "-- window --"))
  ([pane name] (map->Window {:id (swap! window-id inc)
                             :name name
                             :locs {pane {:x 0 :y 0}}})))

(defn visible-cols [row start end]
  (let [col-len (count row)
        first-col (min col-len start)
        last-col (min col-len end)]
    (subvec row first-col last-col)))

(defn in-pane? [loc panes x y]
  (let [pane ((:pane loc) panes)]
    (and (<= (:x loc) x (+ (:x loc) (:width pane)))
         (<= (:y loc) y (+ (:y loc) (:height pane))))))

(sm/defn visible-content :- [[Character]]
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

(sm/defn pane-at :- s/Int
  "Returns the ID of the pane at x and y"
  [window :- Window
   panes :- [Pane]
   x :- s/Int
   y :- s/Int]
  (loop [[loc & locs] (:locs window)]
    (if (or (in-pane? loc panes x y)
            (nil? loc))
      (:pane loc)
      (recur locs))))
