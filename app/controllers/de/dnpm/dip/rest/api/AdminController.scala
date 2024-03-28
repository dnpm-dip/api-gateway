package de.dnpm.dip.rest.api



import java.net.URI
import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  ControllerComponents
}
import de.dnpm.dip.admin.api.AdminService
import de.dnpm.dip.admin.api.AdminPermissions._
import play.api.libs.json.Json.toJson
import de.dnpm.dip.auth.api.{
  Authorization,
  AuthorizationOps,
  UserPermissions,
  UserAuthenticationService
}


class AdminController @Inject()(
  override val controllerComponents: ControllerComponents
)(
  implicit ec: ExecutionContext
)
extends BaseController
with AuthorizationOps[UserPermissions]
{

  import de.dnpm.dip.rest.util.AuthorizationConversions._


  implicit val authService: UserAuthenticationService =
    UserAuthenticationService.getInstance.get


  val adminService =
    AdminService
      .getInstance
      .get


  def connectionReport: Action[AnyContent] =
    AuthorizedAction(GetConnectionReport).async {
      adminService.connectionReport
        .map(toJson(_))
        .map(Ok(_))
    }

}
