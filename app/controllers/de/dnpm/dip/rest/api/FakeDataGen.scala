package de.dnpm.dip.rest.api


import scala.util.Random
import java.time.LocalDate
import de.ekut.tbi.generators.Gen
import de.dnpm.dip.model.{
  Id,
  PatientRecord
}
import de.dnpm.dip.service.mvh.{
  Consent,
  ModelProjectConsent,
  Submission,
  TransferTAN,
}
import de.dnpm.dip.service.DataUpload


trait FakeDataGen[T <: PatientRecord]
{

  protected implicit val rnd: Random =
    new Random

  protected implicit def genDataUpload(
    implicit genT: Gen[T]
  ): Gen[DataUpload[T]] =
    for {
      ttan   <- Gen.uuidStrings.map(Id[TransferTAN](_))
      record <- Gen.of[T]

      consentDate =
        record.getCarePlans
          .map(_.issuedOn)
          .minOption
          .map(_ minusWeeks 2)
          .getOrElse(LocalDate.now)

      metadata =
        Submission.Metadata(
          Submission.Type.Test,
          ttan,
          ModelProjectConsent(
            "Patient Info TE Consent MVGenomSeq vers01",
            Some(consentDate minusDays 1),
            ModelProjectConsent.Purpose.values
              .toList
              .map(
                Consent.Provision(
                  consentDate,
                  _,
                  Consent.Provision.Type.Permit 
                )
              )
          ),
          Some(List.empty)
        )
    } yield DataUpload(
      record,
      Some(metadata)
    )

}
