package toxic.dsl

class Assertion implements Serializable {
  static final String variableReplacement = ''

  List<String> assertions = []

  Assertion gt(Object ying, Object yang) {
    assertOperation(ying, yang, '>')
  }

  Assertion gte(Object ying, Object yang) {
    assertOperation(ying, yang, '>=')
  }

  Assertion lt(Object ying, Object yang) {
    assertOperation(ying, yang, '<')
  }

  Assertion lte(Object ying, Object yang) {
    assertOperation(ying, yang, '<=')
  }

  Assertion eq(Object ying, Object yang) {
    assertOperation(ying, yang, '==')
  }

  Assertion neq(Object ying, Object yang) {
    assertOperation(ying, yang, '!=')
  }

  Assertion contains(String ying, String yang) {
    assertFunction(ying, yang, 'contains')
  }

  Assertion contains(Map ying, String yang) {
    assertFunction(ying, yang, 'containsKey')
  }

  Assertion contains(List ying, String yang) {
    assertFunction(ying, yang, 'contains')
  }

  Assertion startswith(Object ying, Object yang) {
    assertFunction(ying, yang, 'startsWith')
  }

  Assertion endswith(Object ying, Object yang) {
    assertFunction(ying, yang, 'endsWith')
  }

  private Assertion assertFunction(def ying, def yang, String function) {
    String failureMessage = failureMessage("\"${ying}.${function}(${yang})\"")
    assertions << "assert ${format(ying)}.${function}(${format(yang)}) : ${failureMessage}"
    this
  }

  private Assertion assertOperation(Object ying, Object yang, String operator) {
    String failureMessage = failureMessage("\"${ying ?: "''"} ${operator} ${yang ?: "''"}\"")
    if (ying instanceof String && ying?.isNumber() && yang instanceof String && yang?.isNumber()) {
      if (ying.isLong()) ying = ying as long
      else if (ying.isDouble()) ying = ying as double
      if (yang.isLong()) yang = yang as long
      else if (yang.isDouble()) yang = yang as double
    }
    assertions << "assert ${format(ying)} ${operator} ${format(yang)} : ${failureMessage}"
    this
  }

  private String format(Object value) {
    return value
  }

  private String format(String value) {
    return "'''${value}'''"
  }

  private String format(Map value) {
    return "[" + value.collect {k,v-> "${k}:${format(v)}" }.join('') + "]"
  }

  private String format(List value) {
    return "[" + value.collect {v -> format(v) }.join(',') + "]"
  }

  private String failureMessage(String message) {
    message.replaceAll(Step.beginVariableRegex, variableReplacement).replaceAll(Step.endVariableRegex, variableReplacement)
  }
}
