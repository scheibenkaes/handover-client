(ns handover-client.connection
  (:use [clojure.contrib.properties :only [get-system-property]])
  (:import [org.jivesoftware.smack Connection XMPPConnection XMPPException Roster PacketListener])
  (:import [org.jivesoftware.smack.filter PacketFilter])
  (:require [clojure.contrib.logging :as log])
  (:use digest))

(defn localhost []
  (-> (java.net.InetAddress/getLocalHost) .getHostName))

(defn with-host-name [user host] 
  "Append the host name if needed"
  (if (.endsWith user (str "@" host))
    user
    (str user "@" host)))

(defn disconnect [^Connection con] 
  (.disconnect con))

(defn connect 
  ([server] 
   (doto (XMPPConnection. server) .connect)))

(defn login [con user password] 
  (doto con (.login user password)))

(defn connect-and-login [server user password] 
  (-> (connect server) (login user password)))

(defn account-manager [con] 
  (when con (.getAccountManager con)))

(defn create-user! [connection username password] 
  (if-let [am (account-manager connection)]
    (do 
      (log/debug (str "created account: " username " pw: " password))
      (.createAccount am username password) :ok)))

(defn create-id [in] 
  (->> in (digest "SHA1") (take 8) (apply str)))

(defn generate-new-id [] 
  (->> (System/nanoTime) (str (get-system-property "user.home") (rand-int 100)) create-id))

(defn- create-tmp-accounts! [con]
  "Create two tmp accounts."
  (let [other (generate-new-id)
        me (str other "-1")
        [tmp-other tmp-me] (map #(str "temp-" %) [other me])
        passwd tmp-me
        other-passwd tmp-other
        accounts [[con tmp-me passwd] [con tmp-other other-passwd]]]
    (when (not-any? nil? (map #(apply create-user! %) accounts))
      {:me {:id tmp-me :password passwd}
       :other {:id tmp-other :password other-passwd}
       })))

(defn- make-friend! [con other-id] 
  "Add a roster entry and a subscription for the connection in con.
  It must be logged in."
  (-> (.getRoster con) (.createEntry other-id nil nil)))

(defn roster [^Connection con]
  (.getRoster con))

(defn roster-entries [^Roster ros]
  (-> (.getEntries ros) seq))

(defn wait-for-friending [me other]
  (let [my-roster (roster me)
        other-roster (roster other)]
    (.reload my-roster)
    (.reload other-roster)
    (when (every? empty? (map roster-entries [my-roster other-roster]))
      (recur me other))))

(defn- make-friends! 
  "Make the users created by create-tmp-accounts become friends."
  ([server user-map]
   (let [me (:me user-map)
         other (:other user-map)
         my-con (connect-and-login server (:id me) (:password me))
         other-con (connect-and-login server (:id other) (:password other))]
     (make-friend! my-con (-> other :id (with-host-name server)))
     (make-friend! other-con (-> me :id (with-host-name server)))
     (wait-for-friending my-con other-con)
     (dorun (map disconnect [my-con other-con])))))

(defn create-tmp-connection! [server]
  "Main entry point for the 'temp' user workflow.
  Creates two temp users adds both to each others roster and return a map of the values:
  :me - the user which is to be logged into at the local client.
  :other - the user who receives the id to log in as the transmission partner.
  :server - the server address."
  (let [init-con (connect server)
        accounts (create-tmp-accounts! init-con)]
    (do
      (make-friends! server accounts)
      (disconnect init-con)
      (assoc accounts :server server))))
