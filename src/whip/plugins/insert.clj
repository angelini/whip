(ns whip.plugins.insert
  (:require [whip.base.macros :refer :all]
            [whip.plugins.plugin :refer [Plugin]]
            [clojure.core.match :refer [match]]))

(defn insert-char [c]
  (with-pane-buffer-cursor (fn [pane buffer cursor])))

(deftype Insert []
  Plugin
  (translate [this k]
    (match [(:c k) (:ctrl? k) (:alt? k)]
      [\a _ _] (insert-char \a))))

(defn create [] (new Insert))
