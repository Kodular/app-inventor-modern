import { Button, ColorInput, NumberInput, Stack, TextInput } from "@mantine/core"
import { useNode } from "@craftjs/core"
import React from "react"
import { Property } from "csstype"

const ButtonDefaultProps: Partial<{
  color: Property.Color;
  backgroundColor: Property.BackgroundColor;
  padding: Property.Padding<number>;
  height: Property.Height;
  width: Property.Width;
  text: string;
  fontSize: number;
}> = {
  padding: 4,
  text: "Hello K2",
}

export function ButtonComponent (props: React.PropsWithChildren<typeof ButtonDefaultProps> = ButtonDefaultProps) {
  const {
    connectors: {
      connect,
      drag
    }
  } = useNode()

  return (
    <div ref={(ref: HTMLDivElement) => connect(drag(ref))}>
      <Button
        style={{
          color: props.color,
          backgroundColor: props.backgroundColor,
          fontSize: props.fontSize
        }}
      >{props.text}</Button>
    </div>
  )
}

const ButtonSettings = () => {
  const {
    props,
    actions: { setProp },
  } = useNode((node) => ({
    props: node.data.props
  }))

  return (
    <Stack>
      <ColorInput
        placeholder="Pick color"
        label="Text color"
        defaultValue={props.color}
        onChange={(color) => {
          setProp((props: typeof ButtonDefaultProps) => (props.color = color), 500)
        }}
      />
      <ColorInput
        placeholder="Pick color"
        label="Background color"
        defaultValue={props.backgroundColor}
        onChange={(color) => {
          setProp((props: typeof ButtonDefaultProps) => (props.backgroundColor = color), 500)
        }}
      />
      <NumberInput
        placeholder="Padding"
        label="Padding"
        defaultValue={props.padding}
        onChange={(value) =>
          setProp((props: typeof ButtonDefaultProps) => (props.padding = value), 500)
        }
      />
      <TextInput
        placeholder="Height"
        label="Height"
        value={props.height}
        onChange={(e) =>
          setProp((props: typeof ButtonDefaultProps) => (props.height = e.currentTarget.value), 500)
        }
      />
      <TextInput
        placeholder="Width"
        label="Width"
        value={props.width}
        onChange={(e) =>
          setProp((props: typeof ButtonDefaultProps) => (props.width = e.currentTarget.value), 500)
        }
      />
      <TextInput
        placeholder="Text"
        label="Text"
        value={props.text}
        onChange={(e) =>
          setProp((props: typeof ButtonDefaultProps) => (props.text = e.currentTarget.value), 500)
        }
      />
    </Stack>
  )
}

ButtonComponent.craft = {
  props: ButtonDefaultProps,
  related: {
    settings: ButtonSettings,
  },
}
