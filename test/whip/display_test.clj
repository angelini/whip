(ns whip.display-test
  (:require [whip.display :refer :all]
            [clojure.test :refer :all]
            [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(deftest blank-content-test
  (let [blank (generate-blank-content 3 4)]
    (is (= 4 (count blank)))
    (is (= 3 (count (nth blank 0))))))

(deftest diff-tests
  (let [c1 (generate-blank-content 3 3)
        c2 (generate-blank-content 3 3)]
    (testing "identical content"
      (is (= 0 (count (diff-content c1 c2)))))
    (testing "nil content"
      (is (= 9 (count (diff-content nil c1)))))
    (testing "single diff"
      (let [c2 (assoc-in c2 [1 1 :c] \a)
            diff (diff-content c1 c2)]
        (is (= 1 (count diff)))
        (is (= 1 (get-in diff [0 :x])))
        (is (= 1 (get-in diff [0 :y])))
        (is (= \a (get-in diff [0 :cell :c])))))))
