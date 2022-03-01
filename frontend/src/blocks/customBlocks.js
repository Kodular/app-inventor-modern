import Blockly from 'blockly';
import 'blockly/javascript';

Blockly.Blocks['new_boundary_function'] = {
  init: function () {
    this.appendDummyInput()
      .appendField(new Blockly.FieldTextInput("Boundary Function Name"), "Name");
    this.appendStatementInput("Content")
      .setCheck(null);
    this.setInputsInline(true);
    this.setColour(315);
    this.setTooltip("");
    this.setHelpUrl("");
  }
};

Blockly.JavaScript['new_boundary_function'] = function (block) {
  var text_name = block.getFieldValue('Name');
  var statements_content = Blockly.JavaScript.statementToCode(block, 'Content');
  // TODO: Assemble Javascript into code variable.
  var code = 'function ' + text_name + '() {\n' + statements_content + '}\n';
  return code;
};

Blockly.Blocks['return'] = {
  init: function () {
    this.appendValueInput("NAME")
      .setCheck(null)
      .appendField("return");
    this.setInputsInline(false);
    this.setPreviousStatement(true, null);
    this.setColour(330);
    this.setTooltip("");
    this.setHelpUrl("");
  }
};

Blockly.JavaScript['return'] = function (block) {
  var value_name = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
  // TODO: Assemble Javascript into code variable.
  var code = 'return ' + value_name + '\n';
  return code;
};