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

/**
 * Enumeration for supported mediums.
 */
object Medium extends Enumeration {
  type Medium = Value

  val Unknown  = Value("unknown")
  val Search   = Value("search")
  val Internal = Value("internal")
  val Social   = Value("social")
  val Email    = Value("email")
  val Paid     = Value("paid")

  def fromString(s: String): Option[Medium] = values.find(_.toString == s)
}
