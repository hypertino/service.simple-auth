package eu.inn.authentication

import com.typesafe.config.Config
import eu.inn.binders.value.Null
import eu.inn.hyperbus.Hyperbus
import eu.inn.hyperbus.model._
import eu.inn.hyperbus.transport.api.matchers.{RequestMatcher, Specific}
import eu.inn.hyperbus.transport.api.uri.Uri

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class AuthenticationService(hyperbus: Hyperbus, config: Config)(implicit executionContext: ExecutionContext) {

  val requestMatcher = RequestMatcher(Some(Uri("/auth")), Map(Header.METHOD → Specific(Method.GET)))
  hyperbus.onCommand(requestMatcher) { request: DynamicRequest ⇒
    Future {
      authenticate(request.body.content.credentials.asString) match {
        case Some(authUser) ⇒
          Ok(AuthenticationResponseBody(authUser))

        case None ⇒
          NotFound(ErrorBody("not-found"))
      }
    }
  }

  def authenticate(authString: String): Option[AuthUser] = {
    var authUser: Option[AuthUser] = None
    val schemeAndCredentials = authString.replace("Authorization: ", "")
    if (isBasic(schemeAndCredentials)) {
      val credentials = schemeAndCredentials.toLowerCase.replace("basic ", "").split(":")
      val login = credentials(0)
      val password = credentials(1)
      getBasicAuthConfigs.foreach { authConfig ⇒
        if (authConfig.hasPath(login)) {
          val userConfig = authConfig.getConfig(login)
          if (userConfig.getString("password") == password) {
            val id = userConfig.getString("id")
            val roles = userConfig.getStringList("roles").toSet
            authUser = Some(AuthUser(id, roles, Null))
          }
        }
      }
    }
    authUser
  }

  def isBasic(credentials: String): Boolean = {
    credentials.toLowerCase.startsWith("basic")
  }

  def getBasicAuthConfigs: Set[Config] = config.getConfigList("auth-service.basic").toSet
}