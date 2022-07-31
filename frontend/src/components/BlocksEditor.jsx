import { BlocklyWorkspace } from "react-blockly";
import Blockly from "blockly";
import { useEffect, useState } from "react";
import '@/blocks/customBlocks'
import { initialXml, toolboxCategories } from "@/blocks/toolbox";

export default function () {
  useEffect(() => {
    console.log('blocks')
  }, [])

  const [xml, setXml] = useState("");
  const [javascriptCode, setJavascriptCode] = useState("");

  
  function workspaceDidChange(workspace) {
    console.log(xml)
    const code = Blockly.JavaScript.workspaceToCode(workspace);
    setJavascriptCode(code);
  }

  return (
    <>
      <BlocklyWorkspace
        toolboxConfiguration={toolboxCategories}
        initialXml={initialXml}
        workspaceConfiguration={{
          grid: {
            spacing: 20,
            length: 3,
            colour: "#ccc",
            snap: true,
          },
        }}
        onWorkspaceChange={workspaceDidChange}
        onXmlChange={setXml}
        className="blockly-height"
      />
    </>
  )
}