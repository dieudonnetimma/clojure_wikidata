(defproject clj-wikidata "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.3.4" :exclusions [cheshire crouton
                                                org.clojure/tools.reader]]
                 [org.clojure/data.json "0.2.5"]])
