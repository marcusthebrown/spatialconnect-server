(ns spacon.components.trigger
  (:require [com.stuartsierra.component :as component]
            [yesql.core :refer [defqueries]]
            [clojure.spec :as s]
            [spacon.http.intercept :as intercept]
            [spacon.http.response :as response]
            [spacon.models.triggers :as triggermodel]
            [spacon.components.notification :as notification]
            [cljts.relation :as relation]
            [clojure.core.async :as async]
            [spacon.entity.notification :refer [make-mobile-notification] ]
            [cljts.io :as jtsio]
            [clojure.data.json :as json]))

(def invalid-triggers (ref {}))
(def valid-triggers (ref {}))

(defn- rules->geometries [f]
  (assoc f :rules (map (fn [rule]
                         (let [nr (jtsio/read-feature-collection
                                   (json/write-str (:rhs rule)))]
                           (assoc rule :rhs nr)))
                       (:rules f))))

(defn add-trigger [trigger]
  (let [t (rules->geometries trigger)]
    (dosync
     (commute invalid-triggers assoc (keyword (:id t)) t))))

(defn remove-trigger [trigger]
  (dosync
   (commute invalid-triggers dissoc (keyword (:id trigger)))
   (commute valid-triggers dissoc (keyword (:id trigger)))))

(defn set-valid-trigger [trigger]
  (dosync
   (commute invalid-triggers dissoc (keyword (:id trigger)))
   (commute valid-triggers assoc (keyword (:id trigger)) trigger)))

(defn set-invalid-trigger [trigger]
  (dosync
   (commute invalid-triggers assoc (keyword (:id trigger)) trigger)
   (commute valid-triggers dissoc (keyword (:id trigger)))))

(defn- load-triggers []
  (let [tl (doall (triggermodel/all))]
    (doall (map (fn [t]
                  (add-trigger t)) tl))))

(defn- handle-success [trigger notify]
  (if (:repeated trigger)
    (set-valid-trigger trigger)
    (remove-trigger trigger))
  (notification/send->notification notify
                                   (make-mobile-notification
                                    {:to nil
                                     :priority "alert"
                                     :title "Alert"
                                     :body "Point is in Polygon"})))
(defn- handle-failure [trigger]
  (if (nil? ((keyword (:id trigger)) @valid-triggers))
    (set-invalid-trigger trigger)))

(defn process-value [v notify]
  (map (fn [k]
         (let [trigger (k @invalid-triggers)]
           (doall (map
                   (fn [rule]
                     (let [rhs (:rhs rule)
                     ;lhs (:lhs rule)
                           cmp (:comparator rule)]
                       (case cmp
                         "$geowithin" (if-let [features (.features rhs)]
                                        (if (.hasNext features)
                                          (if (relation/within? v (-> features .next .getDefaultGeometry))
                                            (handle-success trigger notify)
                                            (handle-failure trigger))
                                          (handle-failure trigger)))
                         nil)))
                   (:rules trigger)))))
       (keys @invalid-triggers)))

(defn http-get [_]
  (response/ok (triggermodel/all)))

(defn http-get-trigger [context]
  (response/ok (triggermodel/find-by-id (get-in context [:path-params :id]))))

(defn http-put-trigger [context]
  (let [t (:json-params context)
        r (response/ok (triggermodel/update (get-in context [:path-params :id]) t))]
    (add-trigger t)
    r))

(defn http-put-trigger [context]
  (let [t (:json-params context)]
    (if (s/valid? :spacon.spec/trigger-spec t)
      (let [r (response/ok (triggermodel/update
                            (get-in context [:path-params :id]) t))]
        (add-trigger t)
        r)
      (response/error (str "Failed to update trigger:\n"
                           (s/explain-str :spacon.spec/trigger-spec t))))))

(defn http-post-trigger [context]
  (let [t (:json-params context)]
    (if (s/valid? :spacon.spec/trigger-spec t)
      (let [r (response/ok (triggermodel/create t))]
        (add-trigger t)
        r)
      (response/error (str "Failed to create trigger:\n"
                           (s/explain-str :spacon.spec/trigger-spec t))))))

(defn http-delete-trigger [context]
  (let [id (get-in context [:path-params :id])]
    (triggermodel/delete id)
    (remove-trigger {:id id})
    (response/ok "success")))

(defn- process-channel [notify input-channel]
  (async/go (while true
              (let [v (async/<! input-channel)
                    gj (json/write-str v)
                    f (jtsio/read-feature gj)
                    geom (.getDefaultGeometry f)]
                (doall (process-value geom notify))))))

(defn check-value [triggercomp v]
  (async/go (async/>!! (:source-channel triggercomp) v)))

(defn http-test-trigger [triggercomp context]
  (check-value triggercomp (:json-params context))
  (response/ok "success"))

(defn- routes [triggercomp]
  #{["/api/triggers" :get
     (conj intercept/common-interceptors `http-get)]
    ["/api/triggers/:id" :get
     (conj intercept/common-interceptors `http-get-trigger)]
    ["/api/triggers/:id" :put
     (conj intercept/common-interceptors `http-put-trigger)]
    ["/api/triggers" :post
     (conj intercept/common-interceptors `http-post-trigger)]
    ["/api/triggers/:id" :delete
     (conj intercept/common-interceptors `http-delete-trigger)]
    ["/api/trigger/check" :post
     (conj intercept/common-interceptors (partial http-test-trigger triggercomp)) :route-name :http-test-trigger]})

(defrecord TriggerComponent [notify location]
  component/Lifecycle
  (start [this]
    (let [c (async/chan)
          comp (assoc this :source-channel c)]
      (process-channel notify c)
      (load-triggers)
      (assoc comp :routes (routes comp))))
  (stop [this]
    (async/close! (:source-channel this))
    this))

(defn make-trigger-component []
  (map->TriggerComponent {}))
