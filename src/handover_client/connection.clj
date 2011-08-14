(ns handover-client.connection
  (:use [clojure.contrib.properties :only [get-system-property]])
  (:import [org.jivesoftware.smack Connection XMPPConnection])
  (:use digest))

(def connection (atom nil))

(defmacro with-connection [con & body] 
  `(do
     ~@body
     (.disconnect ~con)))

(defn connect [host] 
  (doto (XMPPConnection. host) .connect))

(defn account-manager [con] 
  (when con (.getAccountManager con)))

(defn create-user [connection username password] 
  (if-let [am (account-manager connection)]
    (try 
      (do (.createAccount am username password) :ok) (catch Exception _ nil))))

(defn create-id [in] 
  (->> in (digest "SHA1") (take 8) (apply str)))

(defn generate-new-id [] 
  (->> (System/nanoTime) (str (get-system-property "user.home") (rand-int 100)) create-id))

(defn create-tmp-accounts [con] 
  "Create two tmp accounts."
  (let [other (generate-new-id)
        me (str other "-1")
        [tmp-other tmp-me] (map #(str "TEMP-" %) [other me])
        passwd (generate-new-id)
        other-passwd other
        accounts [[con tmp-me passwd] [con tmp-other other-passwd]]]
    (when (not-any? nil? (map #(apply create-user %) accounts))
      {:me {:id tmp-me :password passwd}
       :other {:id tmp-other :password other-passwd}
       })))
