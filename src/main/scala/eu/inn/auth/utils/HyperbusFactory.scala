package eu.inn.auth.utils

import java.util.concurrent.{Executor, SynchronousQueue, ThreadPoolExecutor, TimeUnit}

import com.typesafe.config.Config
import eu.inn.hyperbus.Hyperbus
import eu.inn.hyperbus.transport.api.{TransportConfigurationLoader, TransportManager}

import scala.concurrent.ExecutionContext

class HyperbusFactory(val config: Config) {
  lazy val hyperbus = new Hyperbus(newTransportManager(), HyperbusFactory.defaultHyperbusGroup(config))(ExecutionContext.fromExecutor(newPoolExecutor()))

  private def newPoolExecutor(): Executor = {
    val maximumPoolSize: Int = Runtime.getRuntime.availableProcessors() * 16
    new ThreadPoolExecutor(0, maximumPoolSize, 5 * 60L, TimeUnit.SECONDS, new SynchronousQueue[Runnable])
  }

  private def newTransportManager(): TransportManager = {
    new TransportManager(TransportConfigurationLoader.fromConfig(config))
  }
}

object HyperbusFactory {
  def defaultHyperbusGroup(config: Config) = {
    if (config.hasPath("hyperbus.group-name"))
      Some(config.getString("hyperbus.group-name"))
    else None
  }
}
