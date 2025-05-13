package de.dnpm.dip.rest.api


import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.mvc.Results.Ok
import play.api.routing.sird._
import play.api.libs.json.Json.toJson


class Router @Inject()(
  catalogRouter: CatalogRouter,
  adminRouter: AdminRouter,
  mtbRouter: MTBRouter,
  rdRouter: RDRouter
)
extends SimpleRouter
{

  private val root: Routes =
    {
      case GET(p"/") =>
        catalogRouter.controller.Action { 
          Ok(toJson(MetaInfo.instance))
        }
    }

  override val routes: Routes =
    root orElse {
      Seq(
        adminRouter,
        catalogRouter,
        mtbRouter withPrefix mtbRouter.prefix,
        rdRouter withPrefix rdRouter.prefix
      )
      .reduce(_ orElse _)
      .routes
    }

}
