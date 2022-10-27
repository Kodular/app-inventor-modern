import React, { useEffect, useRef } from "react"
import "@/blocks/customBlocks"
import "@/blocks/generator"
import { toolbox } from "@/blocks/toolbox"
import { javascriptGenerator } from "blockly/javascript"
import BlocklyComponent from "@/Blockly"
import { useHotkeys } from "@mantine/hooks"
import Blockly from "blockly/core"

function BlocksEditor ({
  workspaceState,
  onWorkspaceStateChange
}) {
  let workspaceRef = useRef(null)

  useEffect(() => {
    if (workspaceRef.current) {

      Blockly.serialization.workspaces.load(workspaceState, workspaceRef.current)

      const ev = workspaceRef.current.addChangeListener(() => {
        onWorkspaceStateChange(Blockly.serialization.workspaces.save(workspaceRef.current))
      })

      return () => {
        workspaceRef.current.removeChangeListener(ev)
      }
    }
  }, [workspaceRef])

  const generateCode = () => {
    const code = javascriptGenerator.workspaceToCode(workspaceRef.current)
    console.log(code)
  }

  useHotkeys([
    ["ctrl+Enter", generateCode],
    ["ctrl+R", () => Blockly.svgResize(workspaceRef.current)]
  ])

  return (
    <BlocklyComponent
      trashcan={true}
      move={{
        scrollbars: true,
        drag: true,
        wheel: true
      }}
      grid={{
        spacing: 20,
        length: 3,
        colour: "#ccc",
        snap: true,
      }}
      zoom={{
        controls: true,
        wheel: true,
        startScale: 1.0,
        maxScale: 3,
        minScale: 0.3,
        scaleSpeed: 1.2,
        pinch: true
      }}
      toolbox={toolbox}
      workspaceRef={workspaceRef}
    />
  )
}

export default BlocksEditor
