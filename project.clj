(defproject invetica/redirects "0.2.0-SNAPSHOT"
  :description "Canonical redirects for your URL."
  :url "https://github.com/invetica/redirects"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[invetica/spec "0.2.0"]
                 [invetica/uri "0.3.0"]
                 [org.clojure/clojure "1.9.0-alpha15"]
                 [ring/ring-core "1.6.0-RC2"]
                 [ring/ring-spec "0.0.2"]]
  :min-lein-version "2.5.0"
  :aliases {"lint" ["do" ["whitespace-linter"] ["eastwood"]]}
  :profiles
  {:dev
   {:dependencies [[com.gfredericks/test.chuck "0.2.7"]
                   [org.clojure/test.check "0.9.0"]
                   ;; See https://github.com/ring-clojure/ring-mock/pull/12
                   [invetica/ring-mock "0.4.0-SNAPSHOT"]]
    :plugins [[jonase/eastwood "0.2.3"]
              [listora/whitespace-linter "0.1.0"]]}})
