(ns handover-client.connection
  (:use [clojure.contrib.properties :only [get-system-property]])
  (:import [org.jivesoftware.smack Connection XMPPConnection XMPPException RosterListener PacketListener])
  (:import [org.jivesoftware.smack.filter PacketFilter])
  (:use digest))

(def *host* "maccie")

(defn with-host-name [user] 
  "Append the host name if needed"
  (if (.endsWith user (str "@" *host*))
    user
    (str user "@" *host*)))

(defn disconnect [^Connection con] 
  (when con
    (.disconnect con)))

(defmacro with-connection [con & body] 
  `(do
     ~@body
     (disconnect ~con)))

(defn connect 
  ([server] 
   (doto (XMPPConnection. server) .connect))
  ([]
   (connect *host*)))

(defn login [con user password] 
  (doto con (.login user password)))

(defn connect-and-login [server user password] 
  (-> (connect server) (login user password)))

(defn account-manager [con] 
  (when con (.getAccountManager con)))

(defn create-user! [connection username password] 
  (if-let [am (account-manager connection)]
    (do (.createAccount am username password) :ok)))

(defn create-id [in] 
  (->> in (digest "SHA1") (take 8) (apply str)))

(defn generate-new-id [] 
  (->> (System/nanoTime) (str (get-system-property "user.home") (rand-int 100)) create-id))

(defn create-tmp-accounts! [con]
  "Create two tmp accounts."
  (let [other (generate-new-id)
        me (str other "-1")
        [tmp-other tmp-me] (map #(str "TEMP-" %) [other me])
        passwd (generate-new-id)
        other-passwd other
        accounts [[con tmp-me passwd] [con tmp-other other-passwd]]]
    (when (not-any? nil? (map #(apply create-user! %) accounts))
      {:me {:id tmp-me :password passwd}
       :other {:id tmp-other :password other-passwd}
       })))

(defn- make-friend! [con other-id] 
  "Add a roster entry and a subscription for the connection in con.
  It must be logged in."
  (-> (.getRoster con) (.createEntry other-id nil nil)))

(defn make-friends! 
  "Make the users created by create-tmp-accounts become friends.
  When no server is given *host* is used."
  ([user-map]
   (make-friends! *host* user-map))
  ([server user-map]
   (let [me (:me user-map)
         other (:other user-map)
         my-con (connect-and-login @host (:id me) (:password me))
         other-con (connect-and-login @host (:id other) (:password other))]
     (make-friend! my-con (-> other :id with-host-name))
     (make-friend! other-con (-> me :id with-host-name)))))

