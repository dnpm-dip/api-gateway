package de.dnpm.dip.rest.api



import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._



class Router @Inject()(
  catalogRouter: CatalogRouter,
  rareDiseaseRouter: RDQueryRouter
)
extends SimpleRouter
{

  override val routes: Routes =
    catalogRouter
      .orElse(
        rareDiseaseRouter withPrefix rareDiseaseRouter.prefix
      )
      .routes


}
