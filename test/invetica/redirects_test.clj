(ns invetica.redirects-test
  (:require [clojure.test :refer :all]
            [invetica.redirects :as sut]
            [invetica.test.spec :as test.spec]
            [invetica.test.util :as t]
            [ring.mock.request :as mock]))

;; -----------------------------------------------------------------------------
;; Specs

(use-fixtures :once test.spec/instrument)

(deftest t-specs
  (test.spec/is-well-specified 'invetica.redirects))

;; -----------------------------------------------------------------------------
;; Sites

(def sites
  #{{:site/aliases #{"invetica.co.uk"
                     "invetica.pro"
                     "invetika.com"}
     :site/canonical "www.invetica.co.uk"
     :site/https? true}
    {:site/aliases #{"example.co.uk"}
     :site/canonical "www.example.com"
     :site/https? false}})

(deftest t-deprecated-config
  (is (= (sut/compile-sites sites)
         (sut/make-registry
          {"invetica.co.uk" {:aliases #{"invetica.pro"
                                        "invetika.com"}
                             :canonical "www.invetica.co.uk"
                             :https? true}
           "example.co.uk" {:canonical "www.example.com"
                            :https? false}}))))

;; -----------------------------------------------------------------------------
;; Redirects

(defn- test-cases
  []
  #{{:desc "No redirect for unknown request."
     :request (mock/request :get "/foo")
     :response nil}
    {:desc "No redirect for unknown request with query string."
     :request (-> (mock/request :get "http://example.com/foo")
                  (mock/query-string {"a" "b" "c" "d"}))
     :response nil}
    {:desc "No redirect for canonical URL."
     :request (-> (mock/request :get "https://www.invetica.co.uk/foo")
                  (mock/query-string {"a" "b" "c" "d"}))
     :response nil}
    {:desc "302 redirect for site alias without required HTTPS."
     :request (-> (mock/request :get "/foo")
                  (assoc :server-name "invetika.com")
                  (mock/header "host" "invetika.com")
                  (mock/query-string {"a" "b", "c" "d"}))
     :response {:status 302
                :headers {"Location" "https://www.invetica.co.uk/foo?a=b&c=d"}
                :body ""}}
    {:desc "302 redirect for site alias with required HTTPS."
     :request (-> (mock/request :get "https://invetica.co.uk/foo")
                  (mock/query-string {"a" "b" "c" "d"}))
     :response {:status 302
                :headers {"Location" "https://www.invetica.co.uk/foo?a=b&c=d"}
                :body ""}}
    {:desc "307 redirect for POST request."
     :request (-> (mock/request :post "/foo")
                  (assoc :server-name "invetika.com")
                  (mock/header "host" "invetika.com")
                  (mock/query-string {"a" "b", "c" "d"}))
     :response {:status 307
                :headers {"Location" "https://www.invetica.co.uk/foo?a=b&c=d"}
                :body ""}}
    {:desc "Redirect to canonical www preserves HTTPS."
     :request (-> (mock/request :get "https://invetica.co.uk/foo")
                  (mock/query-string {"a" "b", "c" "d"}))
     :response {:status 302
                :headers {"Location" "https://www.invetica.co.uk/foo?a=b&c=d"}
                :body ""}}})

(deftest t-canonical-redirect
  (let [registry* (sut/compile-sites sites)]
    (doseq [{:keys [request response]} (test-cases)]
      (is (= response
             (sut/canonical-redirect registry* request))
          (str "Unexpected response for request:\n"
               (t/pprint-str request)
               "\nUsing registry:\n"
               (t/pprint-str registry*))))))
