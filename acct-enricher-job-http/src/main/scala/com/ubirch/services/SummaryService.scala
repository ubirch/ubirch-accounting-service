package com.ubirch.services

import monix.eval.Task

import java.time.LocalDate
import java.util.UUID
import javax.inject.{ Inject, Singleton }

//{
//  "schema_version":"V1.00",
//  "supplier_name":"supp_name",
//  "supplier_id":"supp_id",
//  "order_ref":"order_reference",
//  "invoice_id":"inv_id",
//  "invoice_date":"inv_date",
//  "customers":[
//    {
//      "customer_id":"cust1",
//      "customer_name":"cust1name",
//      "customer_details":[
//        {
//          "event_type":"EVT_A",
//          "event_quantity":"1000"
//        },
//        {
//          "event_type":"EVT_B",
//          "event_quantity":"100"
//        }
//      ]
//    },
//    {
//      "customer_id":"cust2",
//      "customer_name":"cust2name",
//      "customer_details":[
//        {
//          "event_type":"EVT_A",
//          "event_quantity":"1000"
//        }
//      ]
//    }
//  ]
//}
case class Consumption(
    schemaVersion: String,
    supplierName: String,
    supplierId: String,
    invoiceId: String,
    invoiceData: String,
    customers: List[Customer]
)
case class Customer(customerId: String, customerName: String, customerDetails: List[CustomerDetails])
case class CustomerDetails(eventType: String, eventQuantity: Long)

trait SummaryService {
  //    [Mandatory] invoice_id, invoice_date, order_ref: Could be provided by Sales and returned.
  //    [Mandatory] Date as in ddMMMyyy
  //    [Mandatory] Tenant Id
  //    [Optional] Category and/or subcategory, in the document above.
  //    [Optional] Customer Ids
  def get(invoiceId: String, invoiceDate: LocalDate, orderRef: String, tenantId: UUID, category: Option[String]): Task[CustomerDetails]
}

@Singleton
class DefaultSummaryService @Inject() () extends SummaryService {
  override def get(invoiceId: String, invoiceDate: LocalDate, orderRef: String, tenantId: UUID, category: Option[String]): Task[CustomerDetails] = ???
}
