package com.ubirch.services

import com.ubirch.NotFoundException
import com.ubirch.models.postgres.{ EventDAO, TenantDAO }
import com.ubirch.util.TaskHelpers

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
class DefaultSummaryService @Inject() (eventDAO: EventDAO, tenantDAO: TenantDAO) extends SummaryService with TaskHelpers with LazyLogging {

  val cats: List[String] = List("anchoring", "upp_verification", "uvs_verification").distinct

  override def get(invoiceId: String, invoiceDate: LocalDate, from: LocalDate, to: LocalDate, orderRef: String, tenantId: UUID, category: Option[String]): Task[Consumption] =
    for {
      tenant <- tenantDAO.getTenant(tenantId)
      _ <- earlyResponseIf(tenant.isEmpty)(NotFoundException("Unknown tenant: " + tenantId.toString))
      _ <- earlyResponseIf(category.isDefined && !cats.contains(category.get))(new IllegalArgumentException("Unknown category: " + cats.mkString(", ")))
      subTenants <- tenantDAO.getSubTenants(tenantId)
      _ = logger.info("summary_for_tenants:" + subTenants.map(x => x.getEffectiveName + " id=" + x.id).mkString(","))
      eventsProSubTenant <- Task.sequence(subTenants.map(st => eventDAO.get(st.id, category, from, to).map(_.map(x => (st, x)))))
    } yield {

      val customers = eventsProSubTenant.
        flatMap {
          _.groupBy { case (subTenantRow, _) => subTenantRow } // we group by subtenants
            .mapValues(_.groupBy { case (_, eventRow) => eventRow.category }) //we group by category
            .map { case (subTenantRow, categoriesAndEvents) =>

              val totalPerCat =
                categoriesAndEvents.mapValues { events =>
                  events.map { case (_, eventRow) => eventRow }.foldLeft(0)((acc, eventRow) => acc + eventRow.count) //we count all values per category
                }.map { case (category, count) =>
                  CustomerDetails(tenant.map(_.mapEffectiveCategory(category)).getOrElse(category), count)
                }

              (subTenantRow, totalPerCat.toList)
            }
            .map { case (subTenantRow, customerDetails) =>
              Customer(subTenantRow.id.toString, subTenantRow.getEffectiveName, customerDetails.sortBy(_.eventType)) //ensamble Customer
            }
        }

      Consumption(
        schemaVersion = "V1.00",
        supplierName = "ubirch GmbH",
        supplierId = "ubirch GmbH",
        invoiceId = invoiceId,
        invoiceDate = invoiceDate,
        customers = customers
      )
    }
}
