(defproject invetica/redirects "0.3.0-SNAPSHOT"
  :description "Canonical redirects for your URL."
  :url "https://github.com/invetica/redirects"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[invetica/spec "0.4.0"]
                 [invetica/uri "0.5.0"]
                 [org.clojure/clojure "1.9.0-alpha17"]
                 [ring/ring-core "1.6.2"]
                 [invetica/ring-spec "0.0.3-SNAPSHOT"]]
  :min-lein-version "2.5.0"
  :profiles
  {:dev
   {:aliases {"lint" ["do" ["whitespace-linter"] ["eastwood"]]}
    :dependencies [[com.gfredericks/test.chuck "0.2.8"]
                   [org.clojure/test.check "0.10.0-alpha2"]
                   ;; See https://github.com/ring-clojure/ring-mock/pull/12
                   [invetica/ring-mock "0.4.0-SNAPSHOT"]]
    :plugins [[jonase/eastwood "0.2.3"]
              [lein-eftest "0.3.1"]
              [listora/whitespace-linter "0.1.0"]]}
   :ci {:eftest {:report eftest.report.pretty/report}}})
