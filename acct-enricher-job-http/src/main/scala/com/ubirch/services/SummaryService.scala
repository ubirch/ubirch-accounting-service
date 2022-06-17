package com.ubirch.services

import com.ubirch.models.postgres.{ EventDAO, TenantDAO }

import com.typesafe.scalalogging.LazyLogging
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
    invoiceDate: LocalDate,
    customers: List[Customer]
)

case class Customer(customerId: String, customerName: String, customerDetails: List[CustomerDetails])

case class CustomerDetails(eventType: String, eventQuantity: Int)

trait SummaryService {
  //    [Mandatory] invoice_id, invoice_date, order_ref: Could be provided by Sales and returned.
  //    [Mandatory] Date as in ddMMMyyy
  //    [Mandatory] Tenant Id
  //    [Optional] Category and/or subcategory, in the document above.
  //    [Optional] Customer Ids
  def get(invoiceId: String, invoiceDate: LocalDate, from: LocalDate, to: LocalDate, orderRef: String, tenantId: UUID, category: Option[String]): Task[Consumption]
}

@Singleton
class DefaultSummaryService @Inject() (eventDAO: EventDAO, tenantDAO: TenantDAO) extends SummaryService with LazyLogging {
  override def get(invoiceId: String, invoiceDate: LocalDate, from: LocalDate, to: LocalDate, orderRef: String, tenantId: UUID, category: Option[String]): Task[Consumption] =
    for {
      subTenants <- tenantDAO.getSubTenants(tenantId)
      _ = logger.info("summary_for_tenants:" + subTenants.map(x => x.name.getOrElse(x.groupName) + " id=" + x.id).mkString(","))
      eventsProSubTenant <- Task.sequence(subTenants.map(st => eventDAO.get(st.id, from, to).map(_.map(x => (st, x)))))
    } yield {

      val customers = eventsProSubTenant.
        flatMap {
          _.groupBy { case (t, _) => t } // we group by subtenats
            .mapValues(_.groupBy { case (_, e) => e.category }) //we group by category
            .map { case (c, mevs) =>
              val totalPerCat =
                mevs.mapValues { evs =>
                  evs.map { case (_, e) => e }.foldLeft(0)((t, e) => t + e.count) //we count all values per category
                }.map { case (c, count) => CustomerDetails(c, count) }
              (c, totalPerCat.toList)
            }
            .map { case (c, customerDetails) =>
              Customer(c.id.toString, c.name.getOrElse(c.groupName), customerDetails) //ensamble Customer
            }
        }

      Consumption("V1.00", "Ubirch", "ubirch", invoiceId, invoiceDate, customers)
    }
}
