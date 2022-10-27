import Blockly from 'blockly';

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

