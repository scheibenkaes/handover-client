(ns handover-client.test.connection
  (:use handover-client.connection)
  (:use clojure.test))

(deftest test-create-id 
  (is (string? (create-id "asd"))))

(deftest test-generate-new-id 
  (is (= 13 (count (generate-new-id))))
  (is (not= (generate-new-id) (generate-new-id)))
  (is (.startsWith (generate-new-id) "TEMP-")))
