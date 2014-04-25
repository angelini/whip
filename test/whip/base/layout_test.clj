(ns whip.base.layout-test
  (:require [whip.base.layout :refer :all]
            [clojure.test :refer :all]
            [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(def content-str
  ["hello world"
   "second line"
   "third"])

(def buffer (map->Buffer {:id 1
                          :name "-- buffer --"
                          :content (strings->cells content-str :white :black)
                          :meta {}}))

(def pane-left (map->Pane {:id 1
                           :width 5
                           :height 10
                           :buffer 1
                           :buf-x 0
                           :buf-y 0}))

(def pane-right (map->Pane {:id 2
                            :width 5
                            :height 10
                            :buffer 1
                            :buf-x 1
                            :buf-y 1}))

(def panes {1 pane-left 2 pane-right})

(def window (map->Window {:id 1
                          :name "-- window --"
                          :width 10
                          :height 10
                          :locs {1 {:x 0 :y 0}
                                 2 {:x 5 :y 0}}}))

(deftest unique-ids-test
  (let [b1 (create-buffer)
        b2 (create-buffer)]
    (is (not= (:id b1) (:id b2)))))

(deftest reversible-cell-transforms
  (let [cells (strings->cells content-str :white :black)]
    (is (= content-str (cells->strings cells)))))

(deftest visible-content-test
  (let [visible-l (-> (visible-content pane-left buffer)
                      (cells->strings))
        visible-r (-> (visible-content pane-right buffer)
                      (cells->strings))]
    (is (= ["hello" "secon" "third"] visible-l))
    (is (= ["econd" "hird"] visible-r))))

(deftest pane-at-test
  (is (= 1 (pane-at window panes 0 0)))
  (is (= 2 (pane-at window panes 6 3)))
  (is (= nil (pane-at window panes 11 0))))

(deftest resize-test
  (let [[r-window r-panes] (resize window panes 20 20)]
    (is (= 20 (:width r-window)))
    (is (= 20 (:height r-window)))
    (is (= 0 (get-in r-window [:locs 1 :x])))
    (is (= 10 (get-in r-window [:locs 2 :x])))
    (is (= 10 (get-in r-panes [1 :width])))))
