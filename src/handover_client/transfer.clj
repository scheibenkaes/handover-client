(ns handover-client.transfer
  (:use [seesaw make-widget mig])
  (:import [javax.swing JFileChooser])
  (:import [org.jivesoftware.smackx.filetransfer FileTransferListener FileTransferRequest]))

(def running-transfers (ref []))

(def completed-transfers (ref []))

(def requested-transfers (ref []))

(defn choose-transfer [parent callback-fn]
  (let [chooser (JFileChooser.)
        ret (.showOpenDialog chooser parent)]
    (when (= JFileChooser/APPROVE_OPTION ret)
      (callback-fn (.getSelectedFile chooser)))))

(extend-type FileTransferRequest
  MakeWidget
  (make-widget* [req]
    (mig-panel
      :contraints ["" "[][][]"]
      :items [["Ihr Partner möchte Ihnen eine Datei übermitteln." "span 3,wrap,growx"]
              ["Dateiname:" "span 2"] [(.getFileName req) "wrap"]
              ["Beschreibung:" "span 2"][(.getDescription req) "wrap"]
              ["Dateigröße:" "span 2"] [(str (.getFileSize req)) "wrap"]])))

