import { Button } from "@mantine/core"
import { useNode } from "@craftjs/core"
import React from "react"

export const ButtonComponent = ({ text }: { text: string }) => {
  const {
    connectors: {
      connect,
      drag
    }
  } = useNode()

  return (
    <div ref={(ref: HTMLDivElement) => connect(drag(ref))}>
      <Button>{text}</Button>
    </div>
  )
}
