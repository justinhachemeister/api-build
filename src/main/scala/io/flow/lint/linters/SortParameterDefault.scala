package io.flow.lint.linters

import io.flow.lint.Linter
import com.bryzek.apidoc.spec.v0.models.{Operation, Resource, Service}

/**
  *  for resources w/ sort parameter:
  *    - default to lower(name), created_at if there is a name field
  *    - otherwise default to created_at
  */
case object SortParameterDefault extends Linter with Helpers {

  override def validate(service: Service): Seq[String] = {
    service.resources.flatMap(validateResource(service, _))
  }

  def validateResource(service: Service, resource: Resource): Seq[String] = {
    resource.operations.flatMap(validateOperation(service, resource, _))
  }

  def validateOperation(service: Service, resource: Resource, operation: Operation): Seq[String] = {
    operation.parameters.find(_.name == "sort") match {
      case None => {
        Nil
      }
      case Some(sort) => {
        sort.default match {
          case None => {
            Seq(error(resource, operation, "Parameter sort requires a default"))
          }
          case Some(default) => {
            val expected = computeDefault(service, resource.plural)
            default == expected match {
              case true => Nil
              case false => {
                Seq(error(resource, operation, s"Parameter sort default expected to be[$expected] and not[$default]"))
              }
            }
          }
        }
      }
    }
  }

  def computeDefault(service: Service, plural: String): String = {
    service.models.find(_.plural == plural) match {
      case None => sys.error(s"Could not find model with plural[$plural]")
      case Some(model) => {
        model.fields.find(_.name == "name") match {
          case None => "created_at"
          case Some(_) => "lower(name), created_at"
        }
      }
    }
  }

}
