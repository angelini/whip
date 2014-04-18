(ns whip.base.layout
  (:require [schema.core :as s]
            [schema.macros :as sm]))

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
                             :locs {(:id pane) {:x 0 :y 0}}})))

(defn visible-cols [row start end]
  (let [col-len (count row)
        first-col (min col-len start)
        last-col (min col-len end)]
    (subvec row first-col last-col)))

(sm/defn visible-content :- [[Character]]
  "Returns the content of the buffer visible within the pane"
  [pane :- Pane
   buffer :- Buffer]
  (let [{:keys [width height buf-x buf-y]} pane
        content (:content buffer)
        row-len (count content)
        first-row (min row-len buf-y)
        last-row (min row-len (+ buf-y height))]
    (println "pane " pane)
    (println "buffer " buffer)
    (println "content " content)
    (map (fn [r] (visible-cols r buf-x (+ buf-x width)))
         (subvec content first-row last-row))))
