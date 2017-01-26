(ns spacon.trigger-test
  (:require [clojure.test :refer :all]
            [spacon.components.trigger.db :as trigger]
            [spacon.test-utils :as utils]
            [spacon.specs.trigger]
            [clojure.spec :as spec]
            [clojure.spec.gen :as gen]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.core.async :refer :all :as async]))

(use-fixtures :once utils/setup-fixtures)

(defn generate-test-trigger []
  (->> (gen/generate (spec/gen :spacon.specs.trigger/trigger-spec))
       (transform-keys ->snake_case_keyword)))

(deftest all-trigger-test
  (is (true? (utils/spec-passed? `trigger/all))))

(deftest trigger-http-crud-test
  (let [token (utils/authenticate "admin@something.com" "admin")
        auth {"Authorization" (str "Token " token)}
        test-trigger(generate-test-trigger)]

    (testing "Creating a trigger through REST api produces a valid HTML response"
      (let [res (utils/request-post "/api/triggers" test-trigger auth)
            new-trigger (transform-keys ->kebab-case-keyword (:result res))]
        (is (contains? res :result)
            "Response should have result keyword")
        (is (spec/valid? :spacon.specs.trigger/trigger-spec new-trigger)
            "The response should contain a trigger that conforms to the trigger spec")))

    (testing "Retrieving all triggers through REST api produces a valid HTML response"
      (let [res (-> (utils/request-get "/api/triggers" auth))
            trigger (->> res :result first (transform-keys ->kebab-case-keyword))]
        (is (contains? res :result)
            "Response should have result keyword")
        (is (spec/valid? :spacon.specs.trigger/trigger-spec trigger)
            "The response should contain a trigger that conforms to the trigger spec")))

    (testing "Retrieving trigger by its key through REST api produces a valid HTML response"
      (let [t (-> (utils/request-get "/api/triggers" auth) :result first)
            res (-> (utils/request-get (str "/api/triggers/" (:id t)) auth))
            trigger (transform-keys ->kebab-case-keyword (:result res))]
        (is (contains? res :result)
            "Response should have result keyword")
        (is (spec/valid? :spacon.specs.trigger/trigger-spec trigger)
            "The response should contain a trigger that conforms to the trigger spec")))

    (testing "Updating a trigger through REST api produces a valid HTML response"
      (let [trigger (-> (utils/request-get "/api/triggers" auth) :result first)
            renamed-trigger (->> (assoc trigger :name "foo") (transform-keys ->snake_case_keyword))
            res (utils/request-put (str "/api/triggers/" (:id trigger)) renamed-trigger auth)
            updated-trigger (transform-keys ->kebab-case-keyword (:result res))]
        (is (contains? res :result)
            "Response should have result keyword")
        (is (spec/valid? :spacon.specs.trigger/trigger-spec updated-trigger)
            "The response should contain a trigger that conforms to the trigger spec")
        (is (= "foo" (:name updated-trigger))
            "The response should contain the updated trigger name")))

    (testing "Deleting triggers through REST api produces a valid HTML response"
      (let [trigger (-> (utils/request-get "/api/triggers" auth) :result first)
            res (utils/request-delete (str "/api/triggers/" (:id trigger)) auth)]
        (is (= "success" (:result res))
            "The response should contain a success message")))))

(deftest http-trigger-check-test
  (testing "Sending a geojson point to /api/trigger/check will check the triggers"
    (let [trigger-component (:trigger user/system-val)
          feature (gen/generate (spec/gen :spacon.specs.geojson/pointfeature-spec))
          c (:source-channel trigger-component)
          m (mult c)
          tap-channel (chan)]
      (tap m tap-channel)
      (utils/request-post "/api/trigger/check" feature)
      (let [[val ch] (alts!! [tap-channel (timeout 2000)])]
        (println "got this val from tap-channel" val)
        (is (= (:geometry feature) (:value val))
            "The point geom should be sent at the value on the source channel")))))

(deftest mult-test
  (testing "Why does this work but the above test doesn't?"
    (let [feature (gen/generate (spec/gen :spacon.specs.geojson/pointfeature-spec))
          c (chan)
          m (mult c)
          tap-channel (chan)]
      (tap m tap-channel)
      (go (>! c (:geometry feature)))
      (let [[val ch] (alts!! [tap-channel (timeout 2000)])]
        (println "got this val from tap-channel" val)
        (is (= (:geometry feature) val)
            "The point geom should be sent at the value on the source channel")))))





