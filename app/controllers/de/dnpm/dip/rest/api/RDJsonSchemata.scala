package de.dnpm.dip.rest.api


import cats.Eval
import play.api.libs.json.Json
import json.Schema
import json.schema.Version._
import com.github.andyglow.jsonschema.AsPlay._
import de.dnpm.dip.service.DataUpload
import de.dnpm.dip.rd.model.RDPatientRecord
import DataUpload.Schemas._
import de.dnpm.dip.rd.model.json.Schemas._


trait RDJsonSchemata extends JsonSchemata[DataUpload[RDPatientRecord]]
{

  /**
   * Map of JSON schemata by schema spec version
   *
   * Eval used as value type to allow lazily populating the Map with Eval.later(...)
   */
  override val formattedSchemata: Map[String,Eval[String]] = 
    Map(
      "draft-12" -> Eval.later(Schema[DataUpload[RDPatientRecord]].asPlay(Draft12("http://dnpm-dip/schema/rd-submission"))),
      "draft-09" -> Eval.later(Schema[DataUpload[RDPatientRecord]].asPlay(Draft09("http://dnpm-dip/schema/rd-submission"))),
      "draft-07" -> Eval.later(Schema[DataUpload[RDPatientRecord]].asPlay(Draft07("http://dnpm-dip/schema/rd-submission"))),
      "draft-04" -> Eval.later(Schema[DataUpload[RDPatientRecord]].asPlay(Draft04()))
    )
    .map {
      case (version,value) => version -> value.map(Json.prettyPrint)
    }

}
