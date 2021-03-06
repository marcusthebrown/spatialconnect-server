;; Copyright 2016-2017 Boundless, http://boundlessgeo.com
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns user
  (:require [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as server]
            [spacon.server :refer [make-spacon-server-mqtt make-spacon-server-kafka]]
            [clojure.tools.logging :as log]))

(defn start-mqtt-system []
  (log/info "starting mqtt system")
  (make-spacon-server-mqtt {:http-config {:env                     :dev
                                          ::server/join?           false
                                          ::server/allowed-origins {:creds true
                                                                    :allowed-origins (constantly true)}}
                            :mqtt-config {:broker-url (or (System/getenv "MQTT_BROKER_URL")
                                                          "tcp://localhost:1883")}}))

(defn start-kafka-system []
  (log/info "starting kafka system")
  (make-spacon-server-kafka {:http-config {::server/allowed-origins {:allowed-origins [(System/getenv "ALLOWED_ORIGINS")]}}
                             :kafka-config {:broker-url (System/getenv "kafka_BROKER_URL")}
                             :kafka-producer-config {:servers  (or (System/getenv "BOOTSTRAP_SERVERS")
                                                                   "localhost:9092")
                                                     :timeout-ms 2000}
                             :kafka-consumer-config {:servers  (or (System/getenv "BOOTSTRAP_SERVERS")
                                                                   "localhost:9092")
                                                     :group-id (or (System/getenv "GROUP_ID")
                                                                   "sc-consumers")}}))
(defn init-dev []
  (log/info "Initializing dev system for repl")
  (spacon.db.conn/migrate)
  (System/setProperty "javax.net.ssl.trustStore"
                      (or (System/getenv "TRUST_STORE")
                          "tls/test-cacerts.jks"))
  (System/setProperty "javax.net.ssl.trustStoreType"
                      (or (System/getenv "TRUST_STORE_TYPE")
                          "JKS"))
  (System/setProperty "javax.net.ssl.trustStorePassword"
                      (or (System/getenv "TRUST_STORE_PASSWORD")
                          "changeit"))
  (System/setProperty "javax.net.ssl.keyStore"
                      (or (System/getenv "KEY_STORE")
                          "tls/test-keystore.p12"))
  (System/setProperty "javax.net.ssl.keyStoreType"
                      (or (System/getenv "KEY_STORE_TYPE")
                          "pkcs12"))
  (System/setProperty "javax.net.ssl.keyStorePassword"
                      (or (System/getenv "KEY_STORE_PASSWORD")
                          "somepass")) (if (= "kafka" (System/getenv "QUEUE_TYPE"))
                                         (start-kafka-system)
                                         (start-mqtt-system)))

(def system-val nil)

(defn init []
  (alter-var-root #'system-val (constantly (init-dev))))

(defn start []
  (alter-var-root #'system-val component/start-system))

(defn stop []
  (alter-var-root #'system-val
                  (fn [s] (when s (component/stop-system s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (go))
