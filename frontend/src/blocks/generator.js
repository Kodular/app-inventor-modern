import { javascriptGenerator } from "blockly/javascript"

javascriptGenerator["new_boundary_function"] = function (block) {
  var text_name = block.getFieldValue("Name")
  var statements_content = javascriptGenerator.statementToCode(block, "Content")
  // TODO: Assemble Javascript into code variable.
  var code = "function " + text_name + "() {\n" + statements_content + "}\n"
  return code
}

javascriptGenerator["return"] = function (block) {
  var value_name = javascriptGenerator.valueToCode(block, "NAME", javascriptGenerator.ORDER_ATOMIC)
  // TODO: Assemble Javascript into code variable.
  var code = "return " + value_name + "\n"
  return code
}
