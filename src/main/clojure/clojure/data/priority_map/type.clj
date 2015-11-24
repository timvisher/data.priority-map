;; A priority map is a map from items to priorities,
;; offering queue-like peek/pop as well as the map-like ability to
;; easily reassign priorities and other conveniences.
;; by Mark Engelberg (mark.engelberg@gmail.com)
;; October 31, 2013

(ns
  ^{:author "Mark Engelberg",
     :doc "The platform specific implementation of the PersistentPriorityMap type."}
    clojure.data.priority-map.type
  (:import clojure.lang.MapEntry java.util.Map clojure.lang.PersistentTreeMap))

(defmacro apply-keyfn [x]
  `(if ~'keyfn (~'keyfn ~x) ~x))

; A Priority Map is comprised of a sorted map that maps priorities to hash sets of items
; with that priority (priority->set-of-items),
; as well as a hash map that maps items to priorities (item->priority)
; Priority maps may also have metadata
; Priority maps can also have a keyfn which is applied to the "priorities" found as values in
; the item->priority map to get the actual sortable priority keys used in priority->set-of-items.

(deftype PersistentPriorityMap [priority->set-of-items item->priority _meta keyfn]
  Object
  (toString [this] (str (.seq this)))

  clojure.lang.ILookup
  ; valAt gives (get pm key) and (get pm key not-found) behavior
  (valAt [this item] (get item->priority item))
  (valAt [this item not-found] (get item->priority item not-found))

  clojure.lang.IPersistentMap
  (count [this] (count item->priority))

  (assoc [this item priority]
    (let [current-priority (get item->priority item nil)]
      (if current-priority
        ;Case 1 - item is already in priority map, so this is a reassignment
        (if (= current-priority priority)
          ;Subcase 1 - no change in priority, do nothing
          this
          (let [priority-key (apply-keyfn priority)
                current-priority-key (apply-keyfn current-priority)
                item-set (get priority->set-of-items current-priority-key)]
            (if (= (count item-set) 1)
              ;Subcase 2 - it was the only item of this priority
              ;so remove old priority entirely
              ;and conj item onto new priority's set
              (PersistentPriorityMap.
                (assoc (dissoc priority->set-of-items current-priority-key)
                  priority-key (conj (get priority->set-of-items priority-key #{}) item))
                (assoc item->priority item priority)
                (meta this)
                keyfn)
              ;Subcase 3 - there were many items associated with the item's original priority,
              ;so remove it from the old set and conj it onto the new one.
              (PersistentPriorityMap.
                (assoc priority->set-of-items
                  current-priority-key (disj (get priority->set-of-items current-priority-key) item)
                  priority-key (conj (get priority->set-of-items priority-key #{}) item))
                (assoc item->priority item priority)
                (meta this)
                keyfn))))
        ; Case 2: Item is new to the priority map, so just add it.
        (let [priority-key (apply-keyfn priority)]
          (PersistentPriorityMap.
            (assoc priority->set-of-items
                   priority-key (conj (get priority->set-of-items priority-key #{}) item))
            (assoc item->priority item priority)
            (meta this)
            keyfn)))))

  (empty [this] (PersistentPriorityMap. (empty priority->set-of-items) {} _meta keyfn))

  ; cons defines conj behavior
  (cons [this e]
    (if (map? e)
      (into this e)
      (let [[item priority] e] (.assoc this item priority))))

  ; Like sorted maps, priority maps are equal to other maps provided
  ; their key-value pairs are the same.
  (equiv [this o] (= item->priority o))
  (hashCode [this] (.hashCode item->priority))
  (equals [this o] (or (identical? this o) (.equals item->priority o)))

  ;containsKey implements (contains? pm k) behavior
  (containsKey [this item] (contains? item->priority item))

  (entryAt [this k]
    (let [v (.valAt this k this)]
      (when-not (identical? v this)
        (MapEntry. k v))))

  (seq [this]
    (if keyfn
      (seq (for [[priority item-set] priority->set-of-items, item item-set]
             (MapEntry. item (item->priority item))))
      (seq (for [[priority item-set] priority->set-of-items, item item-set]
             (MapEntry. item priority)))))

  ;without implements (dissoc pm k) behavior
  (without
    [this item]
    (let [priority (item->priority item ::not-found)]
      (if (= priority ::not-found)
        ;; If item is not in map, return the map unchanged.
        this
        (let [priority-key (apply-keyfn priority)
              item-set (priority->set-of-items priority-key)]
          (if (= (count item-set) 1)
            ;;If it is the only item with this priority, remove that priority's set completely
            (PersistentPriorityMap. (dissoc priority->set-of-items priority-key)
                                    (dissoc item->priority item)
                                    (meta this)
                                    keyfn)
            ;;Otherwise, just remove the item from the priority's set.
            (PersistentPriorityMap.
              (assoc priority->set-of-items priority-key (disj item-set item)),
              (dissoc item->priority item)
              (meta this)
              keyfn))))))

  java.io.Serializable  ;Serialization comes for free with the other things implemented
  clojure.lang.MapEquivalence
  Map ;Makes this compatible with java's map
  (size [this] (count item->priority))
  (isEmpty [this] (zero? (count item->priority)))
  (containsValue [this v]
    (if keyfn
      (some (partial = v) (vals this)) ; no shortcut if there is a keyfn
      (contains? priority->set-of-items v)))
  (get [this k] (.valAt this k))
  (put [this k v] (throw (UnsupportedOperationException.)))
  (remove [this k] (throw (UnsupportedOperationException.)))
  (putAll [this m] (throw (UnsupportedOperationException.)))
  (clear [this] (throw (UnsupportedOperationException.)))
  (keySet [this] (set (keys this)))
  (values [this] (vals this))
  (entrySet [this] (set this))

  Iterable
  (iterator [this] (clojure.lang.SeqIterator. (seq this)))

  clojure.lang.IPersistentStack
  (peek [this]
    (when-not (.isEmpty this)
      (let [f (first priority->set-of-items)
            item (first (val f))]
        (if keyfn
          (MapEntry. item (item->priority item))
          (MapEntry. item (key f))))))

  (pop [this]
    (if (.isEmpty this) (throw (IllegalStateException. "Can't pop empty priority map"))
      (let [f (first priority->set-of-items),
            item-set (val f)
            item (first item-set),
            priority-key (key f)]
        (if (= (count item-set) 1)
          ;If the first item is the only item with its priority, remove that priority's set completely
          (PersistentPriorityMap.
            (dissoc priority->set-of-items priority-key)
            (dissoc item->priority item)
            (meta this)
            keyfn)
          ;Otherwise, just remove the item from the priority's set.
          (PersistentPriorityMap.
            (assoc priority->set-of-items priority-key (disj item-set item)),
            (dissoc item->priority item)
            (meta this)
            keyfn)))))

  clojure.lang.IFn
  ;makes priority map usable as a function
  (invoke [this k] (.valAt this k))
  (invoke [this k not-found] (.valAt this k not-found))

  clojure.lang.IObj
  ;adds metadata support
  (meta [this] _meta)
  (withMeta [this m] (PersistentPriorityMap. priority->set-of-items item->priority m keyfn))

  clojure.lang.Reversible
  (rseq [this]
    (if keyfn
      (seq (for [[priority item-set] (rseq priority->set-of-items), item item-set]
             (MapEntry. item (item->priority item))))
      (seq (for [[priority item-set] (rseq priority->set-of-items), item item-set]
             (MapEntry. item priority))))))
