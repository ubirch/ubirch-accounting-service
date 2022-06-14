package com.ubirch.services.externals

import com.ubirch.ConfPaths.ThingAPIConfPaths
import com.ubirch.models.postgres.TenantRow
import com.ubirch.services.formats.JsonConverterService

import com.typesafe.config.Config
import monix.eval.Task
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.core5.http.{ ContentType, HttpHost }

import java.net.URL
import java.nio.charset.StandardCharsets
import javax.inject.{ Inject, Singleton }

trait ThingAPI {
  def getTenants(accessToken: String): Task[List[TenantRow]]
}

@Singleton
class DefaultThingAPI @Inject() (config: Config, httpClient: HttpClient, jsonConverterService: JsonConverterService) extends ThingAPI {

  private final val THING_API_ENDPOINT: String = config.getString(ThingAPIConfPaths.URL)

  final val GATEWAY_URL = new URL(THING_API_ENDPOINT)

  override def getTenants(accessToken: String): Task[List[TenantRow]] = for {
    res <- httpClient.executeAsTask {
      SimpleRequestBuilder
        .get()
        .setHttpHost(new HttpHost(GATEWAY_URL.getProtocol, GATEWAY_URL.getHost, GATEWAY_URL.getPort))
        .setPath("/ubirch-web-ui/api/v1/tenants")
        .setHeader("Authorization", "bearer " + accessToken)
        .setHeader("Content-Type", ContentType.APPLICATION_JSON.toString)
        .build()
    }

    tenants <- Task.fromEither(jsonConverterService.as[List[TenantRow]](new String(res.body, StandardCharsets.UTF_8)))

  } yield {
    tenants
  }

}
