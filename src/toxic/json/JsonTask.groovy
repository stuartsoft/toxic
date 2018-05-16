package toxic.json

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import toxic.CompareTask
import toxic.JsonValidator
import toxic.ToxicProperties

class JsonTask extends CompareTask {
  @Override
  def prepare(def request) {
    // Any unquoted variables, such as map or list references, will be replaced with the String representation of the JSON structure
    request = request.replaceAll(/(:\s*[^"])(%)([^%]+)(%)([^"])/) { all, begin, openDelimiter, match, closeDelimiter, end ->
      def value = props[match]
      "${begin}${JsonOutput.toJson(value)}${end}"
    }
    request = super.prepare(request)
    new JsonSlurper().parseText(request)
  }

  @Override
  String lookupExpectedResponse(File file) {
    String responseJson = super.lookupExpectedResponse(file)
    // Quote any unquoted response assignment variables so contents can be correctly parsed as JSON
    responseJson = responseJson.replaceAll(/(:\s*[^"])(%=[^%]+%)([^"])/) { all, begin, match, end ->
      "${begin}\"${match}\"${end}"
    }
  }

  @Override
  void validate(String actualResponse, String expectedResponse, ToxicProperties memory) {
    def validator = new JsonValidator()
    validator.init(memory)
    validator.validate(actualResponse, expectedResponse, memory)
  }

  @Override
  protected transmit(def request, def expectedResponse, def memory) {
    // TODO HTTP post with json
  }
}
