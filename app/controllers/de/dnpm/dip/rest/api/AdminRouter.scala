package de.dnpm.dip.rest.api


import java.net.URI
import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.mvc.Results.Ok
import play.api.libs.json.Json



class AdminRouter @Inject()(
  adminController: AdminController
)
extends SimpleRouter
{

  val status =
    Json.obj("status" -> "Up and running")


  override val routes: Routes = {

    
    case GET(p"/peer2peer/status") =>

      // If the request reaches this point, the backend app is up and running
      adminController.Action {
        Ok(status)
      }

    case GET(p"/admin/connection-report") =>
      adminController.connectionReport

  }


}
