package com.ubirch.services.externals

import com.typesafe.config.Config
import com.ubirch.ConfPaths.ThingAPIConfPaths

import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.core5.http.{ ContentType, HttpHost }

import java.net.URL

import monix.eval.Task

import javax.inject.{ Inject, Singleton }

trait ThingAPI {
  def getTenants(accessToken: String): Task[ResponseData[Array[Byte]]]
}

@Singleton
class DefaultThingAPI @Inject() (config: Config, httpClient: HttpClient) extends ThingAPI {

  private final val THING_API_ENDPOINT: String = config.getString(ThingAPIConfPaths.URL)

  final val GATEWAY_URL = new URL(THING_API_ENDPOINT)

  override def getTenants(accessToken: String): Task[ResponseData[Array[Byte]]] = httpClient.executeAsTask {
    SimpleRequestBuilder
      .get()
      .setHttpHost(new HttpHost(GATEWAY_URL.getProtocol, GATEWAY_URL.getHost, GATEWAY_URL.getPort))
      .setPath("/ubirch-web-ui/api/v1/tenants")
      .setHeader("Content-Type", ContentType.APPLICATION_JSON.toString)
      .build()
  }

}
