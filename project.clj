(defproject handover-client "1.1.0"
  :description "Handing over files. DAU approved."
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [self/smack "3.2.1"]
                 [self/smackx "3.2.1"]
                 [digest "1.2.1"]
                 [seesaw "1.1.0"]]
  :dev-dependencies [[lein-vim "1.0.2-SNAPSHOT"]
                     [self/smackx-debug "3.2.1"]
                     [org.clojars.autre/lein-vimclojure "1.0.0"]]
  :url "http://scheibenkaes.org/assets/handover/"
  :jnlp-file "handover.jnlp"
  :extra-files-to-clean ["handover.jnlp"]
  :main handover-client.core)
