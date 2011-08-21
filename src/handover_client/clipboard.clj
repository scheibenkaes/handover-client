(ns handover-client.clipboard
  (import [java.awt Toolkit]
          [java.awt.datatransfer DataFlavor]))

(defn get-str []
  (try
    (-> 
      (Toolkit/getDefaultToolkit) 
      .getSystemClipboard 
      (.getContents nil) 
      (.getTransferData DataFlavor/stringFlavor) 
      str)
    (catch Exception _ nil)))
