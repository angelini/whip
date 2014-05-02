(ns whip.plugins.insert
  (:require [whip.base.macros :refer :all]
            [clojure.core.match :refer (match)]))

(defn insert-char [c]
  (with-pane-buffer-cursor (fn [pane buffer cursor])))

(defn translate [k]
  (match [(:c k) (:ctrl? k) (:alt? k)]
    [\a _ _] (insert-char \a)))
