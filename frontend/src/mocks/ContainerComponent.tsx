import { Stack } from "@mantine/core"
import { useNode } from "@craftjs/core"
import React from "react"
import type { Property } from "csstype"

export const ContainerDefaultProps : Partial<{
  background: Property.BackgroundColor;
  padding: Property.Padding<number>;
  height: Property.Height;
}> = {
  background: '#ffffff',
  padding: 3,
};

export const ContainerComponent: React.FunctionComponent<React.PropsWithChildren<typeof ContainerDefaultProps>> = ({
  children,
  height,
  background,
  padding
}) => {
  const {
    id,
    connectors: {
      drag,
      connect
    }
  } = useNode()
  return (
    <Stack ref={(ref: HTMLDivElement) => connect(drag(ref))} sx={{ height }}>
      {children || "I'm empty"}
    </Stack>
  )
}

export const ContainerSettings = () => {
  const {
    background,
    padding,
    actions: { setProp },
  } = useNode((node) => ({
    background: node.data.props.background,
    padding: node.data.props.padding,
  }));

  return (
    <div>
      <form>
        <label>Background</label>
        <input
          name="background-color"
          value={background}
          onChange={(color) => {
            setProp((props) => (props.background = color), 500);
          }}
        />
      </form>
      <form>
        <label>Padding</label>
        <input
          defaultValue={padding}
          onChange={(_, value) =>
            setProp((props) => (props.padding = value), 500)
          }
        />
      </form>
    </div>
  );
};

ContainerComponent.craft = {
  props: ContainerDefaultProps,
  related: {
    settings: ContainerSettings,
  },
};
