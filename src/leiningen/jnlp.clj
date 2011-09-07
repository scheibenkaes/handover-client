(ns leiningen.jnlp  "Generate the projects jnlp file."
  (:use [leiningen.jar :only [get-default-uberjar-name]]
        [leiningen.uberjar :only [uberjar]]))

(def template
  "<?xml version='1.0' encoding='UTF-8'?>
<jnlp codebase='%s' 
        href='handover.jnlp'>
    <information>
        <title>Handover</title>
    </information>
    <resources>
        <jar href='%s'/>
    </resources>
    <application-desc main-class='%s'/>
</jnlp>")


(defn fill-template [codebase jarfile main-class]
  (format template codebase jarfile main-class))

(defn jnlp [project]
  (let [{:keys [main url uberjar-name jnlp-file]} project]
    (uberjar project)
    (spit jnlp-file (fill-template url (or uberjar-name (get-default-uberjar-name project)) main))
    (println "Created " jnlp-file)))
