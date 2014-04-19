(ns whip.base.macros
  (:require [whip.base.layout :refer :all]
            [whip.base.state :refer :all]
            [schema.macros :as sm])
  (:import [whip.base.state State]))

(defmacro with-cursor [f]
  `(sm/fn new-state :- State [state# :- State]
     (let [cursor# (:cursor state#)]
       (assoc state# :cursor (~f cursor#)))))

(defmacro with-mode [f]
  `(sm/fn new-state :- State [state# :- State]
     (let [cursor# (:mode state#)]
       (assoc state# :mode (~f cursor#)))))

(defmacro with-window-pane-panes [f]
  `(sm/fn new-state :- State [state# :- State]
     (let [cursor# (:cursor state#)
           window# (get-in state# [:windows (:window cursor#)])
           pane# (get-in state# [:panes (:pane cursor#)])
           panes# (:panes state#)
           [n-window# n-pane# n-panes#] (~f window# pane# panes#)]
       (-> state#
           (assoc :panes n-panes#)
           (assoc-in [:windows (:window cursor#)] n-window#)
           (assoc-in [:panes (:pane cursor#)] n-pane#)))))
