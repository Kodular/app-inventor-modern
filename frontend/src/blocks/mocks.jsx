import { Button, Group, Input, Text } from "@mantine/core"
import { useNode, Element } from "@craftjs/core";

export const TextComponent = ({ text }) => {
  const { connectors: { connect, drag } } = useNode();

  return (
    <div ref={dom => connect(drag(dom))}>
      <Text>{text}</Text>
    </div>
  )
}

export const ButtonComponent = ({ text }) => {
  const { connectors: { connect, drag } } = useNode();

  return (
    <div ref={dom => connect(drag(dom))}>
      <Button>{text}</Button>
    </div>
  )
}

export const InputComponent = ({ text }) => {
  const { connectors: { connect, drag } } = useNode();

  return (
    <div ref={dom => connect(drag(dom))}>
      <Input value={text} readOnly />
    </div>
  )
}

export const Container = ({ children }) => {
  const { id, connectors: { drag, connect } } = useNode();
  return (
    <div ref={dom => connect(drag(dom))}>
      <Group>
        {children || "I'm empty"}
      </Group>
    </div>
  )
}