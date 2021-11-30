(ns request-que-tests
  (:require
   [clojure.test :refer [deftest is testing]]
   [request-que :as sut]))

(def test-state {1 {:total-count 10
                    :page 1
                    :transactions [{:date "2013-12-22" :ledger "Phone & Internet Expense" :amount "-110.71" :company "SHAW CABLESYSTEMS CALGARY AB"}
                                   {:date "2013-12-22" :ledger "Travel Expense Nonlocal" :amount "-8.1" :company "BLACK TOP CABS VANCOUVER BC"}
                                   {:date "2013-12-22" :ledger "Business Meals & Entertainment Expense" :amount "-9.88" :company "GUILT & CO. VANCOUVER BC"}]}
                 2 {:total-count 10
                    :page 2
                    :transactions [{:date "2013-12-19" :ledger "Travel Expense Nonlocal" :amount "-200" :company "YELLOW CAB COMPANY LTD VANCOUVER"}
                                   {:date "2013-12-19" :ledger "Business Meals & Entertainment Expense" :amount "-8.94" :company "NESTERS MARKET #x0064 VANCOUVER BC"}]}
                 3 {:total-count 10
                    :page 3
                    :transactions [{:date "2013-12-17" :ledger "" :amount "907.85" :company "PAYMENT RECEIVED - THANK YOU"}
                                   {:date "2013-12-17" :ledger "Auto Expense" :amount "6.23" :company "SMART CITY FOODS xxxxxx3663 BC"}
                                   {:date "2013-12-17" :ledger "Insurance Expense" :amount "-4.87" :company "LONDON DRUGS 78 POSTAL VANCOUVER BC"}]}
                 4 {:total-count 10
                    :page 4
                    :transactions [{:date "2013-12-13" :ledger "Insurance Expense" :amount "-117.81" :company "LONDON DRUGS 78 POSTAL VANCOUVER BC"}
                                   {:date "2013-12-13" :ledger "Equipment Expense" :amount "-520.85" :company "ECHOSIGN xxxxxxxx6744 CA xx8.80 USD @ xx0878"}]}
                 nil nil
                 99 {}})

(deftest make-tally-test
  (testing "Transactions return as a map keyed on date with a value of the sum of all transaction amounts (rounded to the nearest 100th)"
    (let [expected {"2013-12-22" -128.69
                    "2013-12-19" -208.94
                    "2013-12-17" 909.21
                    "2013-12-13" -638.66}
          actual   (sut/make-tally test-state)]
      (is (= expected actual)))))

(deftest parse-amount-test
  (testing "Should round numbers to the nearest 100th"
    (let [expected '(100.51 44.01 22.89)
          actual (map sut/parse-amount '(100.510234 44.0133 22.8856))]
      (is (= expected actual)))))