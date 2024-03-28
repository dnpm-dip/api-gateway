package de.dnpm.dip.rest.api


import java.net.URI
import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import play.api.mvc.Results.Ok



class AdminRouter @Inject()(
  adminController: AdminController
)
extends SimpleRouter
{

  override val routes: Routes = {

    case GET(p"/peer2peer/status") =>
      adminController.Action {
        Ok("Up and running")
      }

    case GET(p"/admin/connection-report") =>
      adminController.connectionReport

  }


}
