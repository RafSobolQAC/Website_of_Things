package controllers

import javax.inject.{Inject, _}
import models.{Tag, Thing}
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json._
import models.JsonFormats._
import play.api.libs.json.JsValue
import reactivemongo.play.json.collection.{JSONCollection, _}

import scala.concurrent.{ExecutionContext, Future}


class ThingController @Inject()(
                                 components: ControllerComponents,
                                 val reactiveMongoApi: ReactiveMongoApi
                               ) extends AbstractController(components)
  with MongoController with ReactiveMongoComponents with play.api.i18n.I18nSupport {

  implicit def ec: ExecutionContext = components.executionContext

  def collection: Future[JSONCollection] = database.map(_.collection[JSONCollection]("things"))

  def create(thing: Thing) = Action.async { implicit request: Request[AnyContent] =>
    val futureResult = collection.flatMap(_.insert.one(thing))
    futureResult.map(_ => Ok(views.html.things(Thing.createThingForm)))
  }

  def createFromJson: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Thing].map { thing =>
      collection.flatMap(_.insert.one(thing)).map { _ => Ok("Thing created!")
      }
    }.getOrElse(Future.successful(BadRequest("Invalid Json format!")))
  }

  def showThingForm = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.things(Thing.createThingForm))
  }

  def submitForm = Action.async { implicit request: Request[AnyContent] =>
    Thing.createThingForm.bindFromRequest.fold({ formWithErrors =>
      Future.successful(BadRequest(views.html.things(formWithErrors)))
    }, { thing =>
      collection.flatMap(_.insert.one(thing)).map(_=>Ok(views.html.things(Thing.createThingForm)))
    })

  }


}
