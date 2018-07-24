/**
 * Copyright 2012-2018 Snowplow Analytics Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.snowplowanalytics.refererparser

// Json
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._

// Cats
import cats.effect.Sync
//import cats.syntax._
import cats.implicits._
import cats.Traverse

import scala.collection.immutable.HashMap
import scala.collection.JavaConverters
import scala.io.Source

import Medium._

/**
 * Handles loading and storing referers
 */
private[refererparser] object ParseReferersJson {
  case class RefererEntry(
    medium: Medium,
    source: String,
    domains: List[String],
    parameters: Option[List[String]]
  )

  case class JsonEntry(
    domains: List[String],
    parameters: Option[List[String]]
  )

  implicit val jsonEntryDecoder = deriveDecoder[JsonEntry]

  private val ReferersJsonPath = "referers.json"

  def loadJsonFromResources[F[_]: Sync]: F[Either[Exception, Map[String, RefererLookup]]] =
    Sync[F].map(Sync[F].delay { Source.fromResource(ReferersJsonPath).mkString })(loadJsonFromString(_))

  def loadJsonFromString(rawJson: String): Either[Exception, Map[String, RefererLookup]] =
    parse(rawJson).flatMap(loadJson)

  def loadJson(doc: Json): Either[Exception, Map[String, RefererLookup]] =
    parseReferersJson(doc.hcursor).map(parsed => {
      parsed.foldLeft(Map.empty[String, RefererLookup])((map, mediumEntries) => {
        val (medium, entries) = mediumEntries
        entries.foldLeft(map)((mapInner, sourceEntry) => {
          val (source, entry) = sourceEntry
          mapInner.++(entry.domains.map(domain =>
            domain -> RefererLookup(medium, source, entry.parameters.getOrElse(Nil))
          ))
        })
      })
    })

  private def parseReferersJson(c: ACursor): Either[Exception, Map[Medium, Map[String, JsonEntry]]] =
    for {
      mediumKeys <- someOrExcept(c.keys, "Referers json must be an object")
      mediumEntries <- mediumKeys.toList.map(k => for {
        medium <- someOrExcept(Medium.fromString(k), s"Unrecognized medium: '$k'")
        sourceNames <- someOrExcept(c.downField(k).keys, s"Medium '$k' not an object")
        sourceEntriesJson = sourceNames.map(mediumName => c.downField(k).downField(mediumName))
        sourceEntries <- sourceEntriesJson.map(_.as[JsonEntry]).toList.sequence
      } yield medium -> ((sourceNames zip sourceEntries).toMap)).sequence
    } yield mediumEntries.toMap
  
  private def someOrExcept[A](opt: Option[A], message: String): Either[Exception, A] =
    opt.map(Right(_)).getOrElse(Left(new CorruptJsonException(message)))
}
