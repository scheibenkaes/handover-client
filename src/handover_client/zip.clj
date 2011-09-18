(ns handover-client.zip
  (:import [java.io File])
  (:import [java.util.zip ZipOutputStream ZipEntry])
  (:use [clojure.contrib.io :only [file-str]])
  (:require [clojure.contrib.duck-streams :as duck])
  (:use clojure.java.io))

(defn temp-file []
  (File/createTempFile (-> (gensym) name) nil))

(defn package-files [tmp-file files]
  (with-open [zip-out (ZipOutputStream. (output-stream tmp-file))]
    (doseq [f files]
      (with-open [f-in (input-stream f)]
        (let [entry (ZipEntry. (.getName f))]
          (.putNextEntry zip-out entry)
          (duck/copy f-in zip-out)
          (.closeEntry zip-out))))
    tmp-file))

(defn append-postfix [file]
  (if (.endsWith file ".zip")
    file
    (str file ".zip")))

(defn rename-to [old-name new-name]
  (let [parent (.getParentFile old-name)
        new-file (file-str (.getCanonicalPath parent) "/" (append-postfix new-name))]
    (.renameTo old-name new-file)
    new-file))

(defn create-zip [file-name & files]
  (when-not (empty? files)
    (-> (temp-file) (package-files files) (rename-to file-name))))
