(ns whip.base.state-test
  (:require [whip.base.layout :refer :all]
            [whip.base.state :refer :all]
            [clojure.test :refer :all]
            [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(def buffer (map->Buffer {:id 1
                          :name "-- buffer --"
                          :content [[]]
                          :meta {}}))

(def pane (map->Pane {:id 1
                      :width 10
                      :height 10
                      :buffer 1
                      :buf-x 2
                      :buf-y 2}))

(def cursor (map->Cursor {:x 5
                          :y 5
                          :pane 1
                          :window 1}))

(deftest cursor-pane-loc-test
  (let [loc (cursor-pane-loc cursor pane {:x 2 :y 2})]
    (is (= 3 (:x loc)))
    (is (= 3 (:y loc)))))

(deftest cursor-buffer-loc-test
  (let [loc (cursor-buffer-loc cursor buffer pane {:x 3 :y 3})]
    (is (= 4 (:x loc)))
    (is (= 4 (:y loc)))))
