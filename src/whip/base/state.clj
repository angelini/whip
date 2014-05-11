(ns whip.base.state
  (:require [whip.base.layout :refer :all]
            [schema.core :as s]
            [schema.macros :as sm])
  (:import [whip.base.layout Buffer Pane Window]))

(sm/defrecord Mode
  [name :- s/Str
   translate :- s/Any])

(sm/defrecord Cursor
  [x :- s/Int
   y :- s/Int
   pane :- s/Int
   window :- s/Int])

(sm/defrecord State
  [mode :- Mode
   cursor :- Cursor
   buffers :- {s/Int Buffer}
   panes :- {s/Int Pane}
   windows :- {s/Int Window}])

(sm/defn ^:always-validate cursor-pane-loc :- {:x s/Int :y s/Int}
  "Returns the x and y position of the cursor with respect to the pane"
  [cursor :- Cursor
   pane :- Pane
   pane-loc :- {:x s/Int :y s/Int}]
  {:x (- (:x cursor) (:x pane-loc))
   :y (- (:y cursor) (:y pane-loc))})

(sm/defn ^:always-validate cursor-buffer-loc :- {:x s/Int :y s/Int}
  "Returns the x and y position of the cursor with respect to the buffer"
  [cursor :- Cursor
   buffer :- Buffer
   pane :- Pane
   pane-loc :- {:x s/Int :y s/Int}]
  (let [{:keys [x y]} (cursor-pane-loc cursor pane pane-loc)]
    {:x (+ (:buf-x pane) x)
     :y (+ (:buf-y pane) y)}))
