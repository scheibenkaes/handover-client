(ns handover-client.zip.ui
  (:require [handover-client.zip :as zip]
            [handover-client.transfer :as transfer])
  (:use [seesaw core mig])
  (:import [javax.swing JFileChooser]))

(def chooser (atom nil))

(defn now-str []
  (-> (java.text.SimpleDateFormat. "dd.MM.yyyy-HH:mm:ss") (.format (java.util.Date.))))

(defn create-zip-panel []
  (let [file-chooser (doto
                       (JFileChooser.)
                       (.setControlButtonsAreShown false)
                       (.setMultiSelectionEnabled true)
                       (.setFileSelectionMode JFileChooser/FILES_ONLY))]
    (reset! chooser file-chooser)
    (mig-panel :constraints ["" "[]"]
               :items [["Wählen Sie die Dateien aus, die in eine ZIP-Datei verpackt werden sollen." "wrap"]
                       [file-chooser "grow,wrap"]
                       ["Geben Sie einen Namen für die ZIP-Datei an:" "grow,wrap"]
                       [(text :text (now-str)
                              :id :file-name) "growx,wrap"]])))

(defn on-create-zip [w]
  (let [files (-> @chooser .getSelectedFiles seq)
        file-name (-> (select w [:#file-name]) text)]
    {:files files :file-name file-name}))

(defn create-dialog []
  (dialog :content (create-zip-panel) :option-type :ok-cancel :success-fn on-create-zip 
          :title "Mehrere Dateien als ZIP versenden."
          :resizable? false))

(defn wait-dialog [info]
  (dialog
    :options []
    :content (mig-panel
               :constraints ["" "[480]"]
               :items [[(format "<html>Es werden <strong>%s</strong> Dateien verpackt.</html>" 
                                (-> info :files count str)) "grow"]])
    :resizable? false
    :title "Bitte warten Sie bis die ZIP-Datei erstellt wurde."))

(defn run-zip-creation-in-wait-dialog [{:keys [files file-name] :as info}]
  "Show a wait dialog during the zip creation.
  info - has to be a map. See on-create-zip"
  (let [dlg (wait-dialog info)
        wait (promise)
        zipper (fn []
                 (let [zf (apply zip/create-zip file-name files)]
                   (deliver wait zf)))]
    (.start (Thread. #(-> dlg (config! :on-close :nothing) pack! show!)))
    (.start (Thread. zipper))
    (transfer/transfer-file @wait)
    (hide! dlg)
    (dispose! dlg)))

(defn show-zip-dialog []
  "Display the zip creation dialog.
  Returns a map of:
  :files - the selected files
  :file-name - the filename the user specified"
  (-> (create-dialog) pack! show!))
