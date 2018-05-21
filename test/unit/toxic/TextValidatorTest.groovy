// Copyright (c) 2015 Value Pay Services, LLC.  All rights reserved.
package toxic

import org.junit.*
import static org.junit.Assert.fail

public class TextValidatorTest {
  @Test
  public void testValidator() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing %=foo%"
    def actual = "Testing Validate"

    xv.validate(actual, expected, tp)
    assert tp.foo == "Validate"
  }

  @Test
  public void testValidator2() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing %=foo% Function"
    def actual = "Testing Validate Function"

    xv.validate(actual, expected, tp)
    assert tp.foo == "Validate"
  }

  @Test
  public void testValidator3() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "%=foo% Function"
    def actual = "Validate Function"

    xv.validate(actual, expected, tp)
    assert tp.foo == "Validate"
  }

  @Test
  public void testValidatorMultiple() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing %=foo% %=bar% %%"
    def actual = "Testing Validate Function Multiple"

    xv.validate(actual, expected, tp)
    assert tp.foo == "Validate"
    assert tp.bar == "Function"
  }

  @Test
  public void testValidatorXml() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()
    xv.init(tp)

    def expected = """
<response id='%=test1%' data='%%'/>
"""
    def actual = """
<response id='test1' data='abc'/>
"""
    xv.validate(actual, expected, tp)
    assert tp.test1 == "test1"
  }

  @Test
  public void testValidatorDoesNotStripWhitespace() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()
    xv.init(tp)

    def expected = """Line 1
  Line 2 has some leading whitespace
Line 3 does not
"""

    def actual = """Line 1
Line 2 has some leading whitespace
Line 3 does not
"""

    try {
      xv.validate(actual, expected, tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }
  }

  @Test
  public void testValidatorDanglingVar() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing %=foo"
    def actual = "Testing Validate"

    try {
      xv.validate(actual, expected, tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }
  }

  @Test
  public void testValidatorLineBreaks() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = """
Header1: %%
Header2: test
Header3: %%/%%
Header4: 123
"""
    def actual = """
Header1: foo
Header2: test
Header3: rab1/bar3
Header4: 123
"""
    xv.validate(actual, expected, tp)
  }

  @Test
  public void testValidatorDoesNotStripHeaders() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "<html><body>test</body></html>"
    def actual = """HTTP/1.1 200 OK
Content-Type: text/html
Content-Length: 30

<html><body>test</body></html>"""
    try {
      xv.validate(actual, expected, tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }
  }

  @Test
  public void testValidatorMismatch() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing %=foo"
    def actual = "Testing Validate !"

    try {
      xv.validate(actual, expected, tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }
  }

  @Test
  public void testValidateSaveVarAndCompare() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()
    tp.foo="Hello"

    def expected = "%=bar=foo% World"
    def actual = "Hello World"

    xv.validate(actual, expected, tp)

    assert tp.bar == tp.foo
  }

  @Test
  public void testValidateCount() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing Validate%#1%"
    def actual = "Testing Validate!"

    xv.validate(actual, expected, tp)
  }

  @Test
  public void testValidateCountMulti() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing Another2%#2%0Validate%#1%"
    def actual = "Testing Another2000Validate!"

    xv.validate(actual, expected, tp)
  }

  @Test
  public void testValidatorCountMismatch() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing %#1"
    def actual = "Testing Validate !"

    try {
      xv.validate(actual, expected, tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }
  }

  @Test
  public void testValidatorCountFail() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def expected = "Testing 20%#1%00"
    def actual = "Testing 20a10"

    try {
      xv.validate(actual, expected, tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }
  }

  @Test
  public void testValidatorNull() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()
    def test = "Testing 20%#1%00"

    try {
      xv.validate(null, test, tp)
      fail("Expected ValidationException")
    } catch (ValidationException ex) { 
      assert "Content mismatch; actual=null; expected=${test}" == ex.message
    }

    try {
      xv.validate(test, null, tp)
      fail("Expected ValidationException")
    } catch (ValidationException ex) { 
      assert "Content mismatch; actual=${test}; expected=null" == ex.message
    }

    xv.validate(null, null, tp)
  }

  @Test
  public void testValidatorEmpty() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def test = "Testing 20%#1%00"

    try {
      xv.validate(test, "", tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }

    try {
      xv.validate("", test, tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }

    xv.validate("", "", tp)
  }

  @Test
  public void testValidatorRepeated() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def actual = "TestingTesting"
    def expected = "Testing"

    try {
      xv.validate(actual, expected, tp)
      assert false : "Expected ValidatorException"
    } catch (ValidationException ve) {
    }
  }

  @Test
  public void testValidatorVariableCounter() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    def actual = "Test1: 13; Test2: 34; Test3: hello"
    def expected = "Test1: %=foo++%; Test2: %=foo++%; Test3: %=foo%"
    xv.validate(actual, expected, tp)

    assert tp.foo == "hello"
    assert tp.foo0 == "13"
    assert tp.foo1 == "34"
    assert tp.foo_count == 2
  }

  @Test
  void testVariableAssignmentWithSkipUntil() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    String expected = 'bla=%>5%;foo=%=foo%;%%'
    String actual   = 'bla=ignore;ignore=this;and=this;foo=42;the_rest'
    xv.validate(actual, expected, tp)

    assert tp.foo == '42'
  }

  @Test
  void testVariableAssignmentWithSkipSpecific() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    String expected = 'bla=%#15%;foo=%=foo%'
    String actual   = 'bla=ignore;and=this;foo=42'
    xv.validate(actual, expected, tp)

    assert tp.foo == '42'
  }

  @Test
  void testEmptyStringVariableAssignment() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    String expected = "%=value%"
    String actual   = ""
    xv.validate(actual, expected, tp)

    assert tp.value == ""
  }

  @Test
  void testEmptyStringSkipsValidation() {
    def xv = new TextValidator()
    def tp = new ToxicProperties()

    String expected = "%%"
    String actual   = ""
    xv.validate(actual, expected, tp)

    // If no validation exception is thrown, test passes
  }
}
