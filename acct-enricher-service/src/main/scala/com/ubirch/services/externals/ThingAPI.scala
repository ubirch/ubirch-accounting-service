package com.ubirch.services.externals

import com.ubirch.ConfPaths.ThingAPIConfPaths
import com.ubirch.services.formats.JsonConverterService

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.core5.http.{ ContentType, HttpHost }

import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait ThingAPI {
  def getTenants(accessToken: String): Task[List[Tenant]]
  def getTenantIdentities(accessToken: String, tenantId: UUID): Task[List[Identity]]
}
case class Identity(keycloakId: String, deviceId: String, description: String, attributes: Map[String, String], tenantId: Option[UUID])
case class Tenant(id: String, name: String, attributes: Map[String, String], subTenants: List[Tenant], path: String)

@Singleton
class DefaultThingAPI @Inject() (config: Config, httpClient: HttpClient, jsonConverterService: JsonConverterService) extends ThingAPI with LazyLogging {

  private final val THING_API_ENDPOINT: String = config.getString(ThingAPIConfPaths.URL)
  private final val REALM: String = config.getString(ThingAPIConfPaths.REALM_NAME)

  final val GATEWAY_URL = new URL(THING_API_ENDPOINT)

  override def getTenants(accessToken: String): Task[List[Tenant]] = {
    for {
      res <- httpClient.executeAsTask {
        SimpleRequestBuilder
          .get()
          .setHttpHost(new HttpHost(GATEWAY_URL.getProtocol, GATEWAY_URL.getHost, GATEWAY_URL.getPort))
          .setPath(s"/ubirch-web-ui/api/v1/tenants?realm=$REALM")
          .setHeader("Authorization", "bearer " + accessToken)
          .setHeader("Content-Type", ContentType.APPLICATION_JSON.toString)
          .build()
      }

      bodyAsString = new String(res.body, StandardCharsets.UTF_8)
      _ = logger.debug(bodyAsString)

      tenants <- Task.fromEither(jsonConverterService.as[List[Tenant]](bodyAsString))

    } yield {
      tenants
    }
  }

  override def getTenantIdentities(accessToken: String, tenantId: UUID): Task[List[Identity]] = {
    for {
      res <- httpClient.executeAsTask {
        SimpleRequestBuilder
          .get()
          .setHttpHost(new HttpHost(GATEWAY_URL.getProtocol, GATEWAY_URL.getHost, GATEWAY_URL.getPort))
          .setPath(s"/ubirch-web-ui/api/v1/tenants/$tenantId/devices?realm=$REALM&page=0&size=500")
          .setHeader("Authorization", "bearer " + accessToken)
          .setHeader("Content-Type", ContentType.APPLICATION_JSON.toString)
          .build()
      }

      bodyAsString = new String(res.body, StandardCharsets.UTF_8)

      tenants <- Task.fromEither(jsonConverterService.as[List[Identity]](bodyAsString)).map(_.map(_.copy(tenantId = Some(tenantId))))

    } yield {
      tenants
    }
  }
}
