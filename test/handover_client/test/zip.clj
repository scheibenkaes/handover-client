(ns handover-client.test.zip
  (:use handover-client.zip
        handover-client.resources)
  (:import [java.io File])
  (:use clojure.java.io)
  (:use clojure.test))

(defn icons [& icns]
  (for [i icns] (-> (icon-by-name i) as-file)))

(deftest test-temp-file
  (is (instance? File (temp-file))))

(deftest test-create-zip-doesnt-take-nil
  (is (nil? (create-zip nil))))

(deftest test-create-zip
  (let [f (apply create-zip "test" (icons "available" "package"))]
    (is f)
    (is (.exists f))))
