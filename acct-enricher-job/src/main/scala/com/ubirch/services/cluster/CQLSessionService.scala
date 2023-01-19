package com.ubirch
package services.cluster

import com.ubirch.ConfPaths.CassandraClusterConfPaths
import com.ubirch.util.URLsHelper

import com.datastax.oss.driver.api.core._
import com.datastax.oss.driver.api.core.config.{ DefaultDriverOption, DriverConfigLoader, ProgrammaticDriverConfigLoaderBuilder }
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import java.net.InetSocketAddress
import java.nio.file.{ Files, Paths }
import java.security.KeyStore
import javax.inject._
import javax.net.ssl.{ SSLContext, TrustManagerFactory }
import scala.collection.JavaConverters._

/**
  * Component that defines a Cassandra CQLSession.
  */
trait CQLSessionService {

  val cqlSession: CqlSession
  val contactPoints: List[InetSocketAddress]
  val maybeConsistencyLevel: Option[ConsistencyLevel]
  val maybeSerialConsistencyLevel: Option[ConsistencyLevel]

  def buildContactPointsFromString(contactPoints: String): List[InetSocketAddress] = {
    URLsHelper.inetSocketAddressesString(contactPoints)
  }

  def checkConsistencyLevel(consistencyLevel: String): Option[ConsistencyLevel] = try {
    if (consistencyLevel.isEmpty) None
    else Option(DefaultConsistencyLevel.valueOf(consistencyLevel))
  } catch {
    case e: Exception =>
      throw InvalidConsistencyLevel("Invalid Consistency Level: " + e.getMessage)
  }

  def buildSSLOptions(trustStorePath: String, trustStorePassword: String): SSLContext = {
    val trustStore = KeyStore.getInstance("JKS")
    closableTry(Files.newInputStream(Paths.get(trustStorePath)))(_.close()) { stream =>
      trustStore.load(stream, trustStorePassword.toCharArray)
    }.left.foreach { e =>
      throw InvalidTrustStore("Failed to load trust store: " + e.getMessage)
    }

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    trustManagerFactory.init(trustStore)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustManagerFactory.getTrustManagers, null)

    sslContext

  }

}

/**
  * Default implementation of the CQLSessionService Service Component.
  *
  * @param config Represent an injected config object.
  */

@Singleton
class DefaultCQLSessionService @Inject() (config: Config) extends CQLSessionService with CassandraClusterConfPaths with LazyLogging {

  val keyspace: String = config.getString(KEYSPACE)
  /**
    * @note
    * If we use a test container for the test, this part should be changed so that we can easily inject the contact point of the test container.
    * since we currently use a embedded cassandra which use localhost:9042, this code works in the test as well.
    */
  val contactPoints: List[InetSocketAddress] = buildContactPointsFromString(config.getString(CONTACT_POINTS))
  val localDataCenter: String = config.getString(LOCAL_DATACENTER)
  val maybeConsistencyLevel: Option[ConsistencyLevel] = checkConsistencyLevel(config.getString(CONSISTENCY_LEVEL))
  val maybeSerialConsistencyLevel: Option[ConsistencyLevel] = checkConsistencyLevel(config.getString(SERIAL_CONSISTENCY_LEVEL))
  val withSSL: Boolean = config.getBoolean(WITH_SSL)
  lazy val trustStorePath: String = config.getString(TRUST_STORE)
  lazy val trustStorePassword: String = config.getString(TRUST_STORE_PASSWORD)
  lazy val username: String = config.getString(USERNAME)
  lazy val password: String = config.getString(PASSWORD)

  require(keyspace.nonEmpty, throw NoKeyspaceException("Keyspace must be provided."))
  require(contactPoints.nonEmpty, throw NoContactPointsException("Contact points must be provided."))
  require(localDataCenter.nonEmpty, throw NoConsistencyLevelException("Consistency level must be provided."))

  override val cqlSession: CqlSession = {
    val driverConfigLoaderBuilder: ProgrammaticDriverConfigLoaderBuilder = DriverConfigLoader.programmaticBuilder()

    val builder = CqlSession.builder
      .withKeyspace(keyspace)
      .addContactPoints(contactPoints.asJavaCollection)
      .withLocalDatacenter(localDataCenter)
      .withApplicationName("accounting_service")

    maybeConsistencyLevel.foreach { cl =>
      driverConfigLoaderBuilder.withString(DefaultDriverOption.REQUEST_CONSISTENCY, cl.name())
    }
    maybeSerialConsistencyLevel.foreach { cl =>
      driverConfigLoaderBuilder.withString(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY, cl.name())
    }
    driverConfigLoaderBuilder.withString(DefaultDriverOption.PROTOCOL_COMPRESSION, "LZ4")
    builder.withConfigLoader(driverConfigLoaderBuilder.build())

    if (username.nonEmpty && password.nonEmpty) {
      builder.withAuthCredentials(username, password)
    }

    if (withSSL) {
      builder.withSslContext(buildSSLOptions(trustStorePath, trustStorePassword))
    }

    val session = builder.build()
    logger.info("Session to keyspace has been created: " + keyspace)

    session

  }

}
