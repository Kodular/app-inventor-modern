import { BlocklyWorkspace } from "react-blockly";
import { useEffect, useState } from "react";
import '@/blocks/customBlocks'
import { initialXml, toolboxCategories } from "@/blocks/toolbox";
import { javascriptGenerator } from "blockly/javascript"

export default function () {
  useEffect(() => {
    console.log('blocks')
  }, [])

  const [xml, setXml] = useState("");
  const [javascriptCode, setJavascriptCode] = useState("");

  function workspaceDidChange(workspace) {
    console.log(xml)
    const code = javascriptGenerator.workspaceToCode(workspace);
    setJavascriptCode(code);
  }

  return (
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
      onDispose={()=>void 0} onImportXmlError={()=>void 0} onInject={()=>void 0}
    />
  )
}
