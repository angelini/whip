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
  [cursor :- Cursor
   buffers :- {s/Int Buffer}
   panes :- {s/Int Pane}
   windows :- {s/Int Window}])
