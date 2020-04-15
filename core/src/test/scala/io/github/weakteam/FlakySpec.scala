package io.github.weakteam

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FlakySpec extends AnyWordSpec with Matchers {
  "FlakySpec" should {
    "return ok" in {
      1 mustBe 1
    }
  }
}
