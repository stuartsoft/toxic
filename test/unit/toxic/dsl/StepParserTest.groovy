package toxic.dsl

import org.junit.*
import static org.junit.Assert.fail

class StepParserTest {
  @Test
  void should_parse_steps() {
    StepParser stepParser = new StepParser()
    stepParser.step('foo', 'bar') {
      input1 'value1'
    }
    assert 1 == stepParser.steps.size()
    Step step = stepParser.steps[0]
    assert 'foo' == step.function
    assert 'bar' == step.name
    assert ['input1':'value1'] == step.args
    assert [:] == step.outputs
  }

  @Test
  void should_not_allow_duplicate_step_names() {
    StepParser stepParser = new StepParser()
    
    try {
      stepParser.step('foo', 'bar') {
        input1 'value1'
      }
      
      stepParser.step('foo', 'bar') {
        input1 'value1'
      }
      fail('Expected Exception')
    }
    catch(IllegalArgumentException e) {
      assert 'Found duplicated step name; step=bar' == e.message
    }
  }
}
