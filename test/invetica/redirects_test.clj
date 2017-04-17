(ns invetica.redirects-test
  (:require [clojure.test :refer :all]
            [invetica.redirects :as sut]
            [invetica.test.spec :as test.spec]
            [ring.mock.request :as mock]))

(use-fixtures :once test.spec/instrument)

(deftest t-specs
  (test.spec/is-well-specified 'invetica.redirects))

(def registry
  {"invetica.co.uk" {:aliases #{"invetica.pro"
                                "invetika.com"}
                     :canonical "www.invetica.co.uk"
                     :https? true}
   "example.co.uk" {:canonical "www.example.com"
                    :https? false}})

(deftest t-canonical-redirect
  (let [registry* (sut/make-registry registry)]
    (are [request response]
        (= response
           (sut/canonical-redirect registry* request))
      (mock/request :get "/foo")
      nil

      ;; With a matching canonical HTTP host/server-name
      (-> (mock/request :get "http://example.com/foo")
          (mock/query-string {"a" "b" "c" "d"}))
      nil

      ;; With a matching canonical HTTPS host/server-name
      (-> (mock/request :get "https://www.invetica.co.uk/foo")
          (mock/query-string {"a" "b" "c" "d"}))
      nil

      ;; With wrong scheme, and no www
      (-> (mock/request :get "http://invetica.co.uk/foo")
          (mock/query-string {"a" "b" "c" "d"}))
      {:status 302
       :headers {"Location" "https://www.invetica.co.uk/foo?a=b&c=d"}
       :body ""}

      ;; With www missing
      (-> (mock/request :get "https://invetica.co.uk/foo")
          (mock/query-string {"a" "b" "c" "d"}))
      {:status 302
       :headers {"Location" "https://www.invetica.co.uk/foo?a=b&c=d"}
       :body ""}

      ;; With a GET to a redirectable host/server-name
      (-> (mock/request :get "/foo")
          (assoc :server-name "invetika.com")
          (mock/header "host" "invetika.com")
          (mock/query-string {"a" "b" "c" "d"}))
      {:status 302
       :headers {"Location" "https://www.invetica.co.uk/foo?a=b&c=d"}
       :body ""}

      ;; With a POST to a redirectable host/server-name
      (-> (mock/request :post "/foo")
          (assoc :server-name "invetika.com")
          (mock/header "host" "invetika.com")
          (mock/query-string {"a" "b" "c" "d"}))
      {:status 307
       :headers {"Location" "https://www.invetica.co.uk/foo?a=b&c=d"}
       :body ""})))
