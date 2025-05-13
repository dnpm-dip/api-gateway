package de.dnpm.dip.rest.api


import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.mvc.Results.Ok
import play.api.routing.sird._
import play.api.libs.json.Json.toJson


class AdminRouter @Inject()(
  val controller: AdminController
)
extends SimpleRouter
{

  override val routes: Routes = {
    
    case GET(p"/peer2peer/meta-info") => controller.Action { Ok(toJson(MetaInfo.instance)) }

    case GET(p"/peer2peer/status") => controller.status

    case GET(p"/admin/connection-report") => controller.connectionReport

  }


}
