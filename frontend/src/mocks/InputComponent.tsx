import { Input } from "@mantine/core"
import { useNode } from "@craftjs/core"
import React from "react"

export const InputComponent = ({ text }: { text: string }) => {
  const {
    connectors: {
      connect,
      drag
    }
  } = useNode()

  return (
    <div ref={(ref: HTMLDivElement) => connect(drag(ref))}>
      <Input value={text} readOnly/>
    </div>
  )
}
