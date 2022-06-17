import com.ubirch.Service
import com.ubirch.controllers.{ AcctEventsEnricherController, InfoController, ResourcesController }

import org.scalatra.LifeCycle

import javax.servlet.ServletContext

/**
  * Represents the configuration of controllers
  */
class ScalatraBootstrap extends LifeCycle {

  lazy val infoController: InfoController = Service.get[InfoController]
  lazy val acctEventsEnricherController: AcctEventsEnricherController = Service.get[AcctEventsEnricherController]
  lazy val resourceController: ResourcesController = Service.get[ResourcesController]

  override def init(context: ServletContext): Unit = {

    context.setInitParameter("org.scalatra.cors.preflightMaxAge", "5")
    context.setInitParameter("org.scalatra.cors.allowCredentials", "false")
    context.setInitParameter("org.scalatra.environment", "production")

    context.mount(
      handler = infoController,
      urlPattern = "/",
      name = "Info"
    )
    context.mount(
      handler = acctEventsEnricherController,
      urlPattern = "/api/acct_events",
      name = "AcctEventsEnricher"
    )
    context.mount(
      handler = resourceController,
      urlPattern = "/api-docs",
      name = "Resources"
    )
  }
}

