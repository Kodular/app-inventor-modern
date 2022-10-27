export const initialWorkspaceJson = {
  blocks: {
    languageVersion: 0,
    blocks: [
      {
        type: "text",
        x: 100,
        y: 100,
        fields: {
          TEXT: "hello world"
        }
      }
    ]
  }
}

export const toolbox = {
  kind: "categoryToolbox",
  contents: [
    {
      kind: "category",
      name: "Controls",
      colour: "#5C81A6",
      contents: [
        {
          kind: "block",
          type: "controls_if"
        },
        {
          kind: "block",
          type: "controls_ifelse"
        },
        {
          kind: "block",
          type: "controls_repeat_ext",
          inputs: {
            TIMES: {
              shadow: {
                type: "math_number",
                fields: {
                  NUM: 10
                }
              }
            }
          }
        }
      ]
    },
    {
      kind: "category",
      name: "Logic",
      colour: "#5C81A6",
      contents: [
        {
          kind: "block",
          type: "logic_compare"
        },
        {
          kind: "block",
          type: "logic_operation"
        },
        {
          kind: "block",
          type: "logic_negate"
        },
        {
          kind: "block",
          type: "logic_boolean"
        },
        {
          kind: "block",
          type: "logic_null",
          disabled: true
        },
        {
          kind: "block",
          type: "logic_ternary"
        }
      ]
    },
    {
      kind: "category",
      name: "Math",
      colour: "#5CA65C",
      contents: [
        {
          kind: "block",
          type: "math_round"
        },
        {
          kind: "block",
          type: "math_number"
        }
      ]
    },
    {
      kind: "category",
      name: "Text",
      colour: "#5C68A6",
      contents: [
        {
          kind: "block",
          type: "text_charAt",
          inputs: {
            VALUE: {
              block: {
                type: "variables_get",
                fields: {
                  VAR: "text"
                }
              }
            }
          }
        }
      ]
    },
    {
      kind: "category",
      name: "Custom",
      colour: "#5CA699",
      contents: [
        {
          kind: "block",
          type: "new_boundary_function"
        },
        {
          kind: "block",
          type: "return"
        }
      ]
    },
    {
      kind: "category",
      name: "Components",
      expanded: true,
      contents: [
        {
          kind: "category",
          name: "Button",
          contents: [
            {
              kind: "block",
              type: "new_boundary_function"
            }
          ]
        },
        {
          kind: "category",
          name: "Input",
          contents: [
            {
              kind: "block",
              type: "return"
            }
          ]
        }
      ]
    }
  ]
}
