import { Text } from "@mantine/core"
import { useNode } from "@craftjs/core"
import React from "react"

export const TextComponent = ({ text }: { text: string }) => {
  const {
    connectors: {
      connect,
      drag
    }
  } = useNode()

  return (
    <div ref={(ref: HTMLDivElement) => connect(drag(ref))}>
      <Text>{text}</Text>
    </div>
  )
}
