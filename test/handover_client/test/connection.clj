(ns handover-client.test.connection
  (:use handover-client.connection)
  (:use clojure.test))

(deftest test-create-id 
  (is (string? (create-id "asd"))))

(deftest test-generate-new-id 
  (is (= 8 (count (generate-new-id)))))