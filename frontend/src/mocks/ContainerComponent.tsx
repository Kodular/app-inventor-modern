import { ColorInput, NumberInput, Select, Stack, TextInput } from "@mantine/core"
import { useNode } from "@craftjs/core"
import React from "react"
import type { Property } from "csstype"

const ContainerDefaultProps: Partial<{
  backgroundColor: Property.BackgroundColor;
  padding: Property.Padding<number>;
  height: Property.Height;
  width: Property.Width;
  direction: Property.FlexDirection;
}> = {
  backgroundColor: "#ffffff",
  padding: 4,
  width: "100%",
  direction: "column"
}

export function ContainerComponent (props: React.PropsWithChildren<typeof ContainerDefaultProps> = ContainerDefaultProps) {
  const {
    id,
    connectors: {
      drag,
      connect
    }
  } = useNode()
  return (
    <div
      ref={(ref: HTMLDivElement) => connect(drag(ref))}
      style={{
        display: "flex",
        flexDirection: props.direction,
        height: props.height,
        width: props.width,
        backgroundColor: props.backgroundColor,
        padding: props.padding
      }}
    >
      {props.children || "I'm empty"}
    </div>
  )
}

const ContainerSettings = () => {
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
        label="Background color"
        defaultValue={props.backgroundColor}
        onChange={(color) => {
          setProp((props: typeof ContainerDefaultProps) => (props.backgroundColor = color), 500)
        }}
      />
      <NumberInput
        placeholder="Padding"
        label="Padding"
        defaultValue={props.padding}
        onChange={(value) =>
          setProp((props: typeof ContainerDefaultProps) => (props.padding = value), 500)
        }
      />
      <TextInput
        placeholder="Height"
        label="Height"
        value={props.height}
        onChange={(e) =>
          setProp((props: typeof ContainerDefaultProps) => (props.height = e.currentTarget.value), 500)
        }
      />
      <TextInput
        placeholder="Width"
        label="Width"
        value={props.width}
        onChange={(e) =>
          setProp((props: typeof ContainerDefaultProps) => (props.width = e.currentTarget.value), 500)
        }
      />
      <Select
        label="Direction"
        placeholder="Pick one"
        data={[
          {
            value: "row",
            label: "Row"
          },
          {
            value: "column",
            label: "Column"
          },
          {
            value: "row-reverse",
            label: "Row Reverse"
          },
          {
            value: "column-reverse",
            label: "Column Reverse"
          },
        ]}
        value={props.direction}
        onChange={(value: Property.FlexDirection) =>
          setProp((props: typeof ContainerDefaultProps) => (props.direction = value), 500)
        }
      />
    </Stack>
  )
}

ContainerComponent.craft = {
  props: ContainerDefaultProps,
  related: {
    settings: ContainerSettings,
  },
}
