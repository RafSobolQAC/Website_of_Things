package controllers

import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

@Singleton
class HomeController @Inject()(
                                cc: ControllerComponents
                              ) extends AbstractController(cc)
   with play.api.i18n.I18nSupport {

  def index: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index("Done!"))
  }

  def aboutMe: Action[AnyContent] = Action {implicit request: Request[AnyContent] =>
    Ok(views.html.about())
  }
}
