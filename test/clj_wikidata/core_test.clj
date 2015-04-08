(ns clj-wikidata.core-test
  (:require [clojure.test :refer :all]
            [clj-wikidata.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))



; test



(client/get "https://en.wikipedia.org/w/api.php?action=query&titles=Albert%20Einstein&prop=info&format=jsonfm")

(client/get "https://www.wikidata.org/wiki/Special:EntityData/Q276731.json")

(client/get "https://www.wikidata.org/w/index.php?search=Douglas+Adams&title=Special%3ASearch&go=Seite&uselang=de")

(client/get "https://www.wikidata.org/w/api.php?action=opensearch&search=Te")

(client/get "https://www.wikidata.org/w/index.php?search=testaa&title=Special%3ASearch&go=Go&uselang=de")


(client/get "http://www.wikidata.org/entity/Q1985727")

(client/get "http://tools.wmflabs.org/reasonator/?q=Q42")

(client/get "https://www.wikidata.org/w/index.php?search=booba&title=Special%3ASearch&go=Seite")

(client/get "http://www.wikidata.org/w/api.php?format=json&action=wbgetentities&ids=Q42&props=labels&sites=enwiki&titles&sitefilter=enwiki&languages=en")

(client/get "http://www.wikidata.org/w/api.php?action=opensearch&search=Douglas&limit=10&namespace=0&format=json")

(client/get "https://en.wikipedia.org/w/api.php?action=opensearch&search=Douglas+Adam&limit=10&namespace=0&format=json")

(client/get "https://www.wikidata.org/w/index.php?search=aa&title=Special%3ASearch&go=Go&uselang=de")



(def response-json (client/get "https://www.wikidata.org/w/index.php?title=Special%3ASearch&profile=default&search=alexander+meier&fulltext=Search&uselang=de"))

(get response-parser :body)



(parse "https://www.wikidata.org/w/index.php?title=Special%3ASearch&profile=default&search=alexander+meier&fulltext=Search&uselang=de")

(def response-parser (parse "https://www.wikidata.org/w/index.php?title=Special%3ASearch&profile=default&search=alexander+meier&fulltext=Search&uselang=de"))

(get response-parser :head)

(count (:div response-parser))

(select-keys response-parser [:head])

(def body (nth response-parser 3))
body
(get-in body [:body 0])

(def test [:body {:class "abc1"} [:div {:class "abc2"}]])
(get-in test [:div :class])

(def mw-body (get body 4))

(def mw-body-content (get mw-body 6))
(def mw-content-text (get mw-body-content 4))
(def searchresults (get mw-content-text 5))
(def mw-search-results (get searchresults 3))
(def mw-search-result-heading (get mw-search-results 2))
(def mw-search-result-heading (get mw-search-result-heading 2))
(def a (get mw-search-result-heading 2))
(def id (get a 1))
(def stringId (get-in id [:href]))
(def id (last (clojure.string/split stringId #"/")))
(class id)

(def searchId (str "http://www.wikidata.org" stringId))
searchId
(client/get searchId)

